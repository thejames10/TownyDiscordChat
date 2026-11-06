package com.TownyDiscordChat.TownyDiscordChat.Schedulers;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.Queue.DiscordJobStore;
import com.TownyDiscordChat.TownyDiscordChat.Queue.MemberIntentStore;
import com.TownyDiscordChat.TownyDiscordChat.Queue.MemberIntentStore.Grant;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Nation;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Category;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.VoiceChannel;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Three-way reconciliation: Towny decides desired membership, Discord reports
 * reality, and SQL stores durable intent / resource mappings. The snapshot
 * runs on the Bukkit thread; all SQL happens on a separate worker.
 */
public final class Reconciler {
    private final Main plugin;
    private final DiscordJobStore resources;
    private final MemberIntentStore membership;
    private final BukkitTask periodic;
    private final AtomicBoolean reconciling = new AtomicBoolean(false);
    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    private final AtomicBoolean rerun = new AtomicBoolean(false);

    private static final class CategoryInfo {
        final String kind, name, id;
        final int slots;
        CategoryInfo(String kind, String name, String id, int slots) {
            this.kind = kind; this.name = name; this.id = id; this.slots = slots;
        }
    }
    private static final class Snapshot {
        String guildId, townCategory, nationCategory;
        final List<String> towns = new ArrayList<String>();
        final List<String> nations = new ArrayList<String>();
        final Map<String, String> roleIds = new HashMap<String,String>();
        final Map<String,String> channelIds = new HashMap<String,String>();
        final List<CategoryInfo> categories = new ArrayList<CategoryInfo>();
        final List<Grant> observed = new ArrayList<Grant>();
        final Set<String> desiredPairs = new HashSet<String>();
        final Set<String> cachedPairs = new HashSet<String>();
        int neededTownChannels, neededNationChannels;
    }

    public Reconciler(Main plugin) {
        this.plugin = plugin;
        this.resources = new DiscordJobStore(plugin);
        this.membership = new MemberIntentStore(plugin);
        long seconds = Math.max(30, plugin.getConfig().getLong("queue.reconcileSeconds", 180));
        periodic = Bukkit.getScheduler().runTaskTimer(plugin, () -> snapshot(),
                100L, seconds * 20L);
    }

    /** Event-driven coalesced scan; kicks don't wait for the full periodic interval. */
    public void requestSoon() {
        if (!plugin.isEnabled()) return;
        if (!scheduled.compareAndSet(false, true)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            scheduled.set(false);
            snapshot();
        }, 2L);
    }

    private void snapshot() {
        if (!reconciling.compareAndSet(false, true)) {
            rerun.set(true);
            return;
        }
        try {
            Guild guild = DiscordSRV.getPlugin().getMainGuild();
            if (guild == null || DiscordSRV.getPlugin().getAccountLinkManager() == null) {
                complete();
                return;
            }
            final Snapshot state = capture(guild);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    reconcile(state);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Reconciliation SQL failure: " + e.getMessage());
                } finally {
                    complete();
                }
            });
        } catch (Throwable error) {
            plugin.getLogger().warning("Cannot snapshot Towny/Discord yet: " + error.getMessage());
            complete();
        }
    }

    private void complete() {
        reconciling.set(false);
        if (rerun.getAndSet(false) && plugin.isEnabled()) requestSoon();
    }

    private Snapshot capture(Guild guild) {
        Snapshot state = new Snapshot();
        state.guildId = guild.getId();
        state.townCategory = plugin.getConfig().getString("category.Town.Name", "MINECRAFT-TOWN-CHAT");
        state.nationCategory = plugin.getConfig().getString("category.Nation.Name", "MINECRAFT-NATION-CHAT");
        for (Town town : TownyUniverse.getInstance().getTowns()) state.towns.add(town.getName());
        for (Nation nation : TownyUniverse.getInstance().getNations()) state.nations.add(nation.getName());
        for (Role role : guild.getRoles()) state.roleIds.put(role.getName(), role.getId());

        int capacity = Math.max(1, Math.min(50, plugin.getConfig().getInt("category.SubChannelsMax",50)));
        for (Category cat : guild.getCategories()) {
            int free = Math.max(0, capacity - cat.getChannels().size());
            if (cat.getName().equalsIgnoreCase(state.townCategory))
                state.categories.add(new CategoryInfo("TOWN_CATEGORY", state.townCategory, cat.getId(), free));
            if (cat.getName().equalsIgnoreCase(state.nationCategory))
                state.categories.add(new CategoryInfo("NATION_CATEGORY", state.nationCategory, cat.getId(), free));
        }

        snapshotChannels(guild, state);
        snapshotMembership(guild, state);
        return state;
    }

    private void snapshotChannels(Guild guild, Snapshot state) {
        for (String name : state.towns) {
            checkChannel(guild, state, "TOWN_TEXT", name, state.townCategory, "town-" + name);
            checkChannel(guild, state, "TOWN_VOICE", name, state.townCategory, "town-" + name);
        }
        for (String name : state.nations) {
            checkChannel(guild, state, "NATION_TEXT", name, state.nationCategory, "nation-" + name);
            checkChannel(guild, state, "NATION_VOICE", name, state.nationCategory, "nation-" + name);
        }
    }

    private void checkChannel(Guild guild, Snapshot state, String kind, String name,
                              String category, String roleName) {
        Role role = first(guild.getRolesByName(roleName, true));
        if (role == null) return;
        String id = null;
        if (kind.endsWith("TEXT")) {
            for (TextChannel channel : guild.getTextChannelsByName(name, true)) {
                if (channel.getParent() != null
                        && channel.getParent().getName().equalsIgnoreCase(category)
                        && channel.getPermissionOverride(role) != null
                        && channel.getPermissionOverride(role).getAllowed().contains(Permission.VIEW_CHANNEL)) {
                    id = channel.getId(); break;
                }
            }
        } else {
            for (VoiceChannel channel : guild.getVoiceChannelsByName(name, true)) {
                if (channel.getParent() != null
                        && channel.getParent().getName().equalsIgnoreCase(category)
                        && channel.getPermissionOverride(role) != null
                        && channel.getPermissionOverride(role).getAllowed().contains(Permission.VIEW_CHANNEL)) {
                    id = channel.getId(); break;
                }
            }
        }
        if (id != null) state.channelIds.put(kind+":"+name, id);
    }

    private void snapshotMembership(Guild guild, Snapshot state) {
        Map<String, UUID> accounts = DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts();
        if (accounts == null) accounts = Collections.emptyMap();
        Set<String> linked = new HashSet<String>(accounts.keySet());

        for (Map.Entry<String,UUID> link : accounts.entrySet()) {
            String memberId = link.getKey();
            UUID uuid = link.getValue();
            if (uuid == null) continue;
            Resident resident = TownyUniverse.getInstance().getResident(uuid);
            if (resident == null || !resident.hasTown()) continue;
            try {
                Town town = resident.getTown();
                if (town != null) {
                    addExpected(state, memberId, "town-" + town.getName());
                    if (town.hasNation()) addExpected(state, memberId,
                            "nation-" + town.getNation().getName());
                }
            } catch (NotRegisteredException e) {
                plugin.getLogger().warning("Towny resident changed during Discord audit: " + uuid);
            }
        }

        boolean scanUnlinked = plugin.getConfig().getBoolean("security.revokeUnlinkedAccounts", false);
        for (Member member : guild.getMembers()) {
            boolean isLinked = linked.contains(member.getId());
            if (!isLinked && !scanUnlinked) continue;
            for (Role role : member.getRoles()) {
                if (managed(role)) {
                    Grant grant = new Grant(member.getId(), role.getId());
                    state.observed.add(grant);
                    state.cachedPairs.add(pair(member.getId(), role.getId()));
                }
            }
        }
    }

    private void addExpected(Snapshot state, String member, String roleName) {
        String roleId = state.roleIds.get(roleName);
        if (roleId != null) state.desiredPairs.add(pair(member,roleId));
    }

    private static String pair(String member, String role) { return member + "|" + role; }

    private static boolean managed(Role role) {
        return role.getName().startsWith("town-") || role.getName().startsWith("nation-");
    }

    private static <T> T first(List<T> entries) {
        return entries.isEmpty() ? null : entries.get(0);
    }

    private void reconcile(Snapshot s) throws SQLException {
        Map<String,String> townRoles = resources.mappedIds("TOWN_ROLE");
        Map<String,String> nationRoles = resources.mappedIds("NATION_ROLE");
        Map<String,String> townText = resources.mappedIds("TOWN_TEXT");
        Map<String,String> nationText = resources.mappedIds("NATION_TEXT");
        Map<String,String> townVoice = resources.mappedIds("TOWN_VOICE");
        Map<String,String> nationVoice = resources.mappedIds("NATION_VOICE");

        for (String name : s.towns) {
            if (!eq(s.roleIds.get("town-"+name), townRoles.get(name)))
                resources.enqueue("TOWN_ROLE", name);
            if (plugin.getConfig().getBoolean("town.CreateTextChannelForRole", true)) {
                if (!eq(s.channelIds.get("TOWN_TEXT:"+name),townText.get(name)))
                    resources.enqueue("TOWN_TEXT", name);
                if (!s.channelIds.containsKey("TOWN_TEXT:"+name)) s.neededTownChannels++;
            }
            if (plugin.getConfig().getBoolean("town.CreateVoiceChannelForRole", true)) {
                if (!eq(s.channelIds.get("TOWN_VOICE:"+name),townVoice.get(name)))
                    resources.enqueue("TOWN_VOICE", name);
                if (!s.channelIds.containsKey("TOWN_VOICE:"+name)) s.neededTownChannels++;
            }
        }
        for (String name : s.nations) {
            if (!eq(s.roleIds.get("nation-"+name), nationRoles.get(name)))
                resources.enqueue("NATION_ROLE", name);
            if (plugin.getConfig().getBoolean("nation.CreateTextChannelForRole", true)) {
                if (!eq(s.channelIds.get("NATION_TEXT:"+name),nationText.get(name)))
                    resources.enqueue("NATION_TEXT", name);
                if (!s.channelIds.containsKey("NATION_TEXT:"+name)) s.neededNationChannels++;
            }
            if (plugin.getConfig().getBoolean("nation.CreateVoiceChannelForRole", true)) {
                if (!eq(s.channelIds.get("NATION_VOICE:"+name),nationVoice.get(name)))
                    resources.enqueue("NATION_VOICE", name);
                if (!s.channelIds.containsKey("NATION_VOICE:"+name)) s.neededNationChannels++;
            }
        }

        int townSlots = 0, nationSlots = 0;
        for (CategoryInfo cat : s.categories) {
            resources.adoptCategory(cat.kind, cat.name, cat.id);
            if ("TOWN_CATEGORY".equals(cat.kind)) townSlots += cat.slots;
            if ("NATION_CATEGORY".equals(cat.kind)) nationSlots += cat.slots;
        }
        if (townSlots < s.neededTownChannels) resources.enqueue("TOWN_CATEGORY",s.townCategory);
        if (nationSlots < s.neededNationChannels) resources.enqueue("NATION_CATEGORY",s.nationCategory);

        // Record pre-existing granted roles, including when SQL was wiped.
        membership.observe(s.guildId, s.observed);
        Set<String> previouslyGranted = new HashSet<String>();
        List<Grant> known = membership.tracked(s.guildId);
        for (Grant grant : known) {
            String key = pair(grant.memberId,grant.roleId);
            previouslyGranted.add(key);
            if (!s.desiredPairs.contains(key)) {
                // A removed Towny member or previously linked member must
                // relinquish access even if cached Discord state is incomplete.
                membership.desire(s.guildId,grant.memberId,grant.roleId,false);
            }
        }
        for (String key : s.desiredPairs) {
            String[] ids = key.split("\\|",2);
            if (!s.cachedPairs.contains(key)) {
                membership.desire(s.guildId,ids[0],ids[1],true);
            } else {
                // A rejoin must cancel an old queued revocation even if
                // Discord still shows the role attached.
                membership.supersedeExisting(s.guildId,ids[0],ids[1],true);
            }
        }
    }

    private static boolean eq(String a, String b) {
        return a != null && a.equals(b);
    }

    public void cancel() { periodic.cancel(); }
}
