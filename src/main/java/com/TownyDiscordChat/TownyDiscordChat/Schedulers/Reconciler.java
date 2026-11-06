package com.TownyDiscordChat.TownyDiscordChat.Schedulers;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.SetupValidator;
import com.TownyDiscordChat.TownyDiscordChat.Queue.DiscordJobStore;
import com.TownyDiscordChat.TownyDiscordChat.Queue.StableEntityStore;
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
    private final StableEntityStore stable;
    private final BukkitTask periodic;
    private final AtomicBoolean reconciling = new AtomicBoolean(false);
    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    private final AtomicBoolean rerun = new AtomicBoolean(false);
    private final AtomicBoolean firstMemberAudit = new AtomicBoolean(true);

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
        final Map<String,String> townUuids = new HashMap<String,String>();
        final Map<String,String> nationUuids = new HashMap<String,String>();
        final Map<String,String> safeRoleNamesById = new HashMap<String,String>();
        final Map<String,String> safeTextNamesById = new HashMap<String,String>();
        final Map<String,String> safeVoiceNamesById = new HashMap<String,String>();
        final Map<String, String> roleIds = new HashMap<String,String>();
        final Set<String> ambiguousRoleNames = new HashSet<String>();
        final Map<String,String> channelIds = new HashMap<String,String>();
        final List<CategoryInfo> categories = new ArrayList<CategoryInfo>();
        final List<Grant> observed = new ArrayList<Grant>();
        final Set<String> desiredPairs = new HashSet<String>();
        final Set<String> cachedPairs = new HashSet<String>();
        final Set<String> linkedMembers = new HashSet<String>();
        final Map<String,String> expectedTownByMember = new HashMap<String,String>();
        final Map<String,String> expectedNationByMember = new HashMap<String,String>();
        final Set<String> cachedMembers = new HashSet<String>();
        final Set<String> blockedRoleIds = new HashSet<String>();
        int neededTownChannels, neededNationChannels;
    }

    public Reconciler(Main plugin) {
        this.plugin = plugin;
        this.resources = new DiscordJobStore(plugin);
        this.membership = new MemberIntentStore(plugin);
        this.stable = new StableEntityStore(plugin);
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
                plugin.setGuildValidated(false);
                complete();
                return;
            }
            SetupValidator.Result check=SetupValidator.validateGuild(plugin.getConfig(),guild);
            if (!check.valid()) {
                plugin.setGuildValidated(false);
                for (String error:check.errors())
                    plugin.reportAdminWarning("Discord preflight: "+error);
                complete();
                return;
            }
            if (!plugin.isGuildValidated()) {
                plugin.setGuildValidated(true);
                plugin.getLogger().info("Discord preflight passed for guild "+guild.getId()
                        + ". SQL synchronization is now enabled.");
                for (String warning:check.warnings()) plugin.getLogger().info("[DISCOVERY] "+warning);
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
        for (Town town : TownyUniverse.getInstance().getTowns()) {
            state.towns.add(town.getName());
            state.townUuids.put(town.getUUID().toString(),town.getName());
        }
        for (Nation nation : TownyUniverse.getInstance().getNations()) {
            state.nations.add(nation.getName());
            state.nationUuids.put(nation.getUUID().toString(),nation.getName());
        }
        for (Role role : guild.getRoles()) {
            if (!role.getPermissions().isEmpty()) continue;
            state.safeRoleNamesById.put(role.getId(),role.getName());
            if (state.roleIds.containsKey(role.getName())) {
                state.ambiguousRoleNames.add(role.getName());
                state.roleIds.remove(role.getName());
            } else if (!state.ambiguousRoleNames.contains(role.getName())) {
                state.roleIds.put(role.getName(), role.getId());
            }
        }

        int capacity = Math.max(1, Math.min(50, plugin.getConfig().getInt("category.SubChannelsMax",50)));
        for (Category cat : guild.getCategories()) {
            if (cat.getPermissionOverride(guild.getPublicRole()) == null
                    || !cat.getPermissionOverride(guild.getPublicRole()).getDenied()
                        .contains(Permission.VIEW_CHANNEL)) continue;
            int free = Math.max(0, capacity - cat.getChannels().size());
            if (cat.getName().equalsIgnoreCase(state.townCategory))
                state.categories.add(new CategoryInfo("TOWN_CATEGORY", state.townCategory, cat.getId(), free));
            if (cat.getName().equalsIgnoreCase(state.nationCategory))
                state.categories.add(new CategoryInfo("NATION_CATEGORY", state.nationCategory, cat.getId(), free));
        }

        for (TextChannel channel : guild.getTextChannels()) {
            if (privateChannel(guild, channel.getParent(),
                    channel.getPermissionOverride(guild.getPublicRole()) != null
                    && channel.getPermissionOverride(guild.getPublicRole())
                        .getDenied().contains(Permission.VIEW_CHANNEL)))
                state.safeTextNamesById.put(channel.getId(),channel.getName());
        }
        for (VoiceChannel channel : guild.getVoiceChannels()) {
            if (privateChannel(guild, channel.getParent(),
                    channel.getPermissionOverride(guild.getPublicRole()) != null
                    && channel.getPermissionOverride(guild.getPublicRole())
                        .getDenied().contains(Permission.VIEW_CHANNEL)))
                state.safeVoiceNamesById.put(channel.getId(),channel.getName());
        }
        snapshotChannels(guild, state);
        snapshotMembership(guild, state);
        return state;
    }

    private boolean privateChannel(Guild guild, Category parent, boolean restricted) {
        return restricted && parent != null
                && parent.getPermissionOverride(guild.getPublicRole()) != null
                && parent.getPermissionOverride(guild.getPublicRole()).getDenied()
                        .contains(Permission.VIEW_CHANNEL);
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
                        && channel.getPermissionOverride(role).getAllowed().contains(Permission.VIEW_CHANNEL)
                        && channel.getPermissionOverride(guild.getPublicRole()) != null
                        && channel.getPermissionOverride(guild.getPublicRole()).getDenied()
                            .contains(Permission.VIEW_CHANNEL)) {
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
        state.linkedMembers.addAll(linked);

        for (Map.Entry<String,UUID> link : accounts.entrySet()) {
            String memberId = link.getKey();
            UUID uuid = link.getValue();
            if (uuid == null) continue;
            Resident resident = TownyUniverse.getInstance().getResident(uuid);
            if (resident == null || !resident.hasTown()) continue;
            try {
                Town town = resident.getTown();
                if (town != null) {
                    state.expectedTownByMember.put(memberId,town.getUUID().toString());
                    if (town.hasNation()) state.expectedNationByMember.put(memberId,
                            town.getNation().getUUID().toString());
                }
            } catch (NotRegisteredException e) {
                plugin.getLogger().warning("Towny resident changed during Discord audit: " + uuid);
            }
        }

        boolean scanUnlinked = plugin.getConfig().getBoolean("security.revokeUnlinkedAccounts", false);
        for (Member member : guild.getMembers()) {
            state.cachedMembers.add(member.getId());
            boolean isLinked = linked.contains(member.getId());
            if (!isLinked && !scanUnlinked) continue;
            for (Role role : member.getRoles()) {
                if (managed(role, state)) {
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

    private static boolean managed(Role role, Snapshot state) {
        String name = role.getName();
        // Do not appropriate a similarly-prefixed role merely because its
        // name looks like ours. Existing SQL-tracked grants remain revocable.
        return (name.startsWith("town-") && state.towns.contains(name.substring(5)))
                || (name.startsWith("nation-") && state.nations.contains(name.substring(7)));
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

        // Reconcile by immutable Towny UUID before deciding anything by name.
        // This prevents a rename from being interpreted as a delete+create.
        reconcileStable(s,"TOWN",s.townUuids,townRoles,townText,townVoice);
        reconcileStable(s,"NATION",s.nationUuids,nationRoles,nationText,nationVoice);

        int townSlots = 0, nationSlots = 0;
        for (CategoryInfo cat : s.categories) {
            resources.adoptCategory(cat.kind, cat.name, cat.id);
            if ("TOWN_CATEGORY".equals(cat.kind)) townSlots += cat.slots;
            if ("NATION_CATEGORY".equals(cat.kind)) nationSlots += cat.slots;
        }
        if (townSlots < s.neededTownChannels) resources.enqueue("TOWN_CATEGORY",s.townCategory);
        if (nationSlots < s.neededNationChannels) resources.enqueue("NATION_CATEGORY",s.nationCategory);

        // A fresh install may have no SQL history and an incomplete JDA member
        // cache. Audit linked members via Discord REST in a slow, durable lane.
        boolean fullAudit = firstMemberAudit.get();
        for (String member : s.linkedMembers) {
            if (fullAudit || !s.cachedMembers.contains(member))
                resources.enqueue("AUDIT_MEMBER", member);
        }
        firstMemberAudit.set(false);
        if (plugin.getConfig().getBoolean("queue.fullAudit.enabled",true)) {
            resources.enqueueFullAuditIfDue(s.guildId,
                    plugin.getConfig().getLong("queue.fullAudit.intervalSeconds",21600L));
        }

        // Resolve expected memberships by IMMUTABLE Towny UUID, not role
        // names. During an in-place rename Discord still has the OLD name,
        // but current residents must retain their existing role ID.
        s.desiredPairs.clear();
        for (Map.Entry<String,String> member : s.expectedTownByMember.entrySet()) {
            StableEntityStore.Entity entity=stable.find(s.guildId,"TOWN",member.getValue());
            if (entity != null && entity.roleId != null) {
                String remoteName=s.safeRoleNamesById.get(entity.roleId);
                if (remoteName != null && remoteName.startsWith("town-"))
                    s.desiredPairs.add(pair(member.getKey(),entity.roleId));
            }
        }
        for (Map.Entry<String,String> member : s.expectedNationByMember.entrySet()) {
            StableEntityStore.Entity entity=stable.find(s.guildId,"NATION",member.getValue());
            if (entity != null && entity.roleId != null) {
                String remoteName=s.safeRoleNamesById.get(entity.roleId);
                if (remoteName != null && remoteName.startsWith("nation-"))
                    s.desiredPairs.add(pair(member.getKey(),entity.roleId));
            }
        }

        // A newly created town must never inherit a previous town's role.
        for (String blocked : s.blockedRoleIds)
            s.desiredPairs.removeIf(key -> key.endsWith("|" + blocked));

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

    private void reconcileStable(Snapshot s, String type, Map<String,String> live,
                                 Map<String,String> roles, Map<String,String> texts,
                                 Map<String,String> voices) throws SQLException {
        String prefix = type.toLowerCase(java.util.Locale.ROOT) + "-";
        for (Map.Entry<String,String> entry : live.entrySet()) {
            String uuid = entry.getKey(), name = entry.getValue();
            String roleKind = type + "_ROLE";
            String textKind = type + "_TEXT";
            String voiceKind = type + "_VOICE";
            String remoteRole = s.roleIds.get(prefix + name);
            String remoteText = s.channelIds.get(textKind + ":" + name);
            String remoteVoice = s.channelIds.get(voiceKind + ":" + name);

            // Backfill stable identities from verified guild resources or
            // existing SQL IDs still present with safe channel permissions.
            String roleCandidate = remoteRole != null ? remoteRole :
                    verifiedId(s.safeRoleNamesById,roles.get(name));
            String textCandidate = remoteText != null ? remoteText :
                    verifiedId(s.safeTextNamesById,texts.get(name));
            String voiceCandidate = remoteVoice != null ? remoteVoice :
                    verifiedId(s.safeVoiceNamesById,voices.get(name));
            StableEntityStore.Entity entity;
            try {
                entity = stable.sync(s.guildId,type,uuid,name,
                        roleCandidate,textCandidate,voiceCandidate);
            } catch (SQLException conflict) {
                if (remoteRole != null) s.blockedRoleIds.add(remoteRole);
                plugin.reportAdminWarning("Stable identity conflict for " + type + "/" + name
                        + ": " + conflict.getMessage());
                continue; // Other towns and access revocations still reconcile.
            }

            // In-place rename by immutable ID. Missing Discord resources are
            // recreated; existing renamed resources are NEVER duplicated.
            if (entity.roleId != null && s.safeRoleNamesById.containsKey(entity.roleId)) {
                if (!(prefix + name).equals(s.safeRoleNamesById.get(entity.roleId)))
                    resources.enqueue("RENAME_" + roleKind,uuid);
                else if (!eq(roles.get(name),entity.roleId))
                    resources.enqueue(roleKind,name); // repair legacy mapping
            } else {
                resources.enqueue(roleKind,name);
            }
            boolean createText = plugin.getConfig().getBoolean(
                    type.toLowerCase(java.util.Locale.ROOT) + ".CreateTextChannelForRole",true);
            boolean createVoice = plugin.getConfig().getBoolean(
                    type.toLowerCase(java.util.Locale.ROOT) + ".CreateVoiceChannelForRole",true);
            if (createText) {
                if (entity.textId != null && s.safeTextNamesById.containsKey(entity.textId)) {
                    if (!name.equals(s.safeTextNamesById.get(entity.textId)))
                        resources.enqueue("RENAME_" + textKind,uuid);
                    else if (!eq(texts.get(name),entity.textId))
                        resources.enqueue(textKind,name);
                } else {
                    resources.enqueue(textKind,name);
                    if (type.equals("TOWN")) s.neededTownChannels++;
                    else s.neededNationChannels++;
                }
            }
            if (createVoice) {
                if (entity.voiceId != null && s.safeVoiceNamesById.containsKey(entity.voiceId)) {
                    if (!name.equals(s.safeVoiceNamesById.get(entity.voiceId)))
                        resources.enqueue("RENAME_" + voiceKind,uuid);
                    else if (!eq(voices.get(name),entity.voiceId))
                        resources.enqueue(voiceKind,name);
                } else {
                    resources.enqueue(voiceKind,name);
                    if (type.equals("TOWN")) s.neededTownChannels++;
                    else s.neededNationChannels++;
                }
            }
        }

        // Deletions require a UUID known BEFORE the entity disappeared.
        // Unmapped legacy rows could represent a historical rename; never
        // delete them automatically without stable identity evidence.
        for (StableEntityStore.Entity previous : stable.all(s.guildId,type)) {
            if (live.containsKey(previous.uuid)) continue;
            if (previous.roleId != null && eq(previous.roleId, roles.get(previous.name)))
                resources.enqueue("DELETE_" + type + "_ROLE",previous.name);
            if (previous.textId != null && eq(previous.textId, texts.get(previous.name)))
                resources.enqueue("DELETE_" + type + "_TEXT",previous.name);
            if (previous.voiceId != null && eq(previous.voiceId, voices.get(previous.name)))
                resources.enqueue("DELETE_" + type + "_VOICE",previous.name);
            if (previous.roleId == null && previous.textId == null && previous.voiceId == null)
                stable.retireIfEmpty(s.guildId,type,previous.uuid);
        }
    }

    private static String verifiedId(Map<String,String> safeNames, String recordedId) {
        return recordedId != null && safeNames.containsKey(recordedId) ? recordedId : null;
    }

    private static boolean eq(String a, String b) {
        return a != null && a.equals(b);
    }

    public void cancel() { periodic.cancel(); }
}
