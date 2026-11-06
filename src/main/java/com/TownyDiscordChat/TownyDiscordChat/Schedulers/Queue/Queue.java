package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Queue;

import com.TownyDiscordChat.TownyDiscordChat.Core.TDCManager;
import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.Queue.DiscordJobStore;
import com.TownyDiscordChat.TownyDiscordChat.Queue.DiscordJobStore.Job;
import com.TownyDiscordChat.TownyDiscordChat.Queue.MemberIntentStore;
import com.TownyDiscordChat.TownyDiscordChat.Queue.MemberIntentStore.Intent;
import com.TownyDiscordChat.TownyDiscordChat.Queue.MemberIntentStore.Grant;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ErrorResponseException;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Category;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.VoiceChannel;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Two-lane durable Discord dispatcher. The DB poll runs asynchronously; Towny and
 * Bukkit lookups happen on the server thread; JDA owns REST rate-limit handling.
 * No blocking .complete(), artificial rate-limit arithmetic, or API calls in a loop.
 */
public final class Queue {
    private final Main plugin;
    private final DiscordJobStore store;
    private final MemberIntentStore memberStore;
    private final BukkitTask scheduler;
    private final AtomicBoolean resourceBusy = new AtomicBoolean(false);
    private final AtomicBoolean memberBusy = new AtomicBoolean(false);
    private volatile Job current;
    private volatile Intent currentIntent;

    public Queue(long startDelaySeconds, long intervalDelaySeconds, Main plugin) {
        this.plugin = plugin;
        this.store = new DiscordJobStore(plugin);
        this.memberStore = new MemberIntentStore(plugin);
        long delay = Math.max(1, startDelaySeconds);
        long period = Math.max(1, intervalDelaySeconds);
        this.scheduler = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                new Runnable() {
                    @Override public void run() { tick(); }
                }, delay * 20L, period * 20L);
    }

    private void tick() {
        // Independent membership and resource lanes. Security revocations
        // never wait behind slow channel/category creation callbacks.
        tickMembers();
        tickResources();
    }

    private void tickMembers() {
        Intent active = currentIntent;
        if (active != null && System.currentTimeMillis() >= active.leaseUntil) {
            plugin.getLogger().severe("Membership REST operation lease expired: " + active);
            currentIntent = null;
            memberBusy.set(false);
        }
        if (!memberBusy.compareAndSet(false, true)) return;
        try {
            Intent intent = memberStore.claim(true);
            if (intent == null) intent = memberStore.claim(false);
            if (intent == null) { memberBusy.set(false); return; }
            scheduleMember(intent);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not claim membership intent: " + e.getMessage());
            currentIntent = null;
            memberBusy.set(false);
        }
    }

    private void tickResources() {
        Job active = current;
        if (active != null && System.currentTimeMillis() >= active.leaseUntil) {
            plugin.getLogger().warning("Resource REST operation lease expired: " + active);
            current = null;
            resourceBusy.set(false);
        }
        if (!resourceBusy.compareAndSet(false, true)) return;
        try {
            final Job job = store.claim();
            if (job == null) { resourceBusy.set(false); return; }
            current = job;
            final Set<String> categoryIds = job.kind.endsWith("CATEGORY")
                    ? store.trackedCategoryIds(job.kind) : Collections.<String>emptySet();
            final String mappedId = job.kind.startsWith("DELETE_")
                    ? store.mappedId(job.kind, job.name) : null;
            Bukkit.getScheduler().runTask(plugin, () -> dispatch(job, categoryIds, mappedId));
        } catch (Exception e) {
            plugin.getLogger().warning("Could not claim resource job: " + e.getMessage());
            // SQL lease safely recovers a claim if the callback never started.
            current = null;
            resourceBusy.set(false);
        }
    }

    private void scheduleMember(Intent intent) {
        currentIntent = intent;
        Bukkit.getScheduler().runTask(plugin, () -> dispatchMember(intent));
    }

    private void dispatchMember(Intent intent) {
        try {
            Guild guild = DiscordSRV.getPlugin().getMainGuild();
            if (guild == null || !guild.getId().equals(intent.guildId)) {
                deferMember(intent, "Discord guild is unavailable or changed");
                return;
            }
            Role role = guild.getRoleById(intent.roleId);
            if (role == null) {
                if (intent.desired) deferMember(intent, "Waiting for role to exist in Discord");
                else doneMember(intent); // Deleted role cannot grant any access.
                return;
            }
            if (intent.desired && !role.getPermissions().isEmpty()) {
                failMember(intent, "Refusing to grant guild permissions through TDC role " + role.getId());
                return;
            }
            Member member = guild.getMemberById(intent.memberId);
            if (member != null && intent.desired) {
                applyMember(intent, guild, member, role);
            } else {
                // Do not infer missing role membership from an incomplete JDA
                // member cache; check the API instead.
                guild.retrieveMemberById(intent.memberId).queue(
                    found -> applyMember(intent, guild, found, role),
                    failure -> {
                        if (!intent.desired && isUnknownMember(failure)) doneMember(intent);
                        else if (intent.desired && isUnknownMember(failure))
                            deferMember(intent, "Member not yet in Discord guild");
                        else failMember(intent, failure);
                    });
            }
        } catch (Throwable e) {
            failMember(intent, e);
        }
    }

    private static boolean isUnknownMember(Throwable failure) {
        return failure instanceof ErrorResponseException
                && ((ErrorResponseException) failure).getErrorCode() == 10007;
    }

    private void applyMember(Intent intent, Guild guild, Member member, Role role) {
        try {
            boolean hasRole = member.getRoles().contains(role);
            if (hasRole == intent.desired) {
                doneMember(intent);
                return;
            }
            if (intent.desired) {
                guild.addRoleToMember(member, role).queue(
                        success -> doneMember(intent),
                        failure -> failMember(intent, failure));
            } else {
                guild.removeRoleFromMember(member, role).queue(
                        success -> doneMember(intent),
                        failure -> failMember(intent, failure));
            }
        } catch (Throwable e) {
            failMember(intent, e);
        }
    }

    private void doneMember(Intent intent) {
        finalizeMemberAsync(intent, () -> {
            memberStore.succeed(intent);
            // Verify the eventual remote result, including after a lease
            // expired and an older asynchronous grant arrived too late.
            if (intent.desired) store.enqueue("AUDIT_MEMBER", intent.memberId);
        }, "Completed " + intent);
    }

    private void deferMember(Intent intent, String reason) {
        finalizeMemberAsync(intent, () -> memberStore.defer(intent, reason), "Deferred " + intent + ": " + reason);
    }

    private void failMember(Intent intent, Throwable error) {
        failMember(intent, error.getClass().getSimpleName() + ": " + error.getMessage());
    }

    private void failMember(Intent intent, String reason) {
        finalizeMemberAsync(intent, () -> memberStore.fail(intent, reason), "Failed " + intent + ": " + reason);
    }

    private void finalizeMemberAsync(Intent intent, SQLAction action, String message) {
        if (!plugin.isEnabled()) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                action.run();
                if (!intent.desired && message.startsWith("Failed")) plugin.getLogger().severe(message);
                else plugin.getLogger().info(message);
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not persist membership intent " + intent
                        + ": " + e.getMessage());
            } finally {
                if (currentIntent == intent) {
                    currentIntent = null;
                    memberBusy.set(false);
                }
            }
        });
    }

    private void dispatch(final Job job, Set<String> categoryIds, String mappedId) {
        try {
            Guild guild = DiscordSRV.getPlugin().getMainGuild();
            if (guild == null) {
                defer(job, "DiscordSRV guild is not ready");
                return;
            }
            if (job.kind.startsWith("DELETE_")) {
                dispatchDeletion(job, guild, mappedId);
                return;
            }
            switch (job.kind) {
                case "AUDIT_MEMBER": {
                    // On a clean reinstall JDA's cache may omit linked users.
                    // Fetch the member from Discord instead of trusting cache.
                    guild.retrieveMemberById(job.name).queue(
                        member -> Bukkit.getScheduler().runTask(plugin,
                            () -> inspectLinkedMember(job, guild, member)),
                        failure -> {
                            if (isUnknownMember(failure)) discard(job);
                            else fail(job, failure.getClass().getSimpleName()
                                    + ": " + failure.getMessage());
                        });
                    break;
                }
                case "TOWN_ROLE": {
                    Town town = TownyUniverse.getInstance().getTown(job.name);
                    if (town == null) { discard(job); return; }
                    Role role = uniqueSafeRole(guild, "town-" + job.name);
                    if (role != null) { success(job, role.getId(), null); return; }
                    TDCManager.createDiscordRole(town, id -> result(job, id, null));
                    break;
                }
                case "NATION_ROLE": {
                    Nation nation = TownyUniverse.getInstance().getNation(job.name);
                    if (nation == null) { discard(job); return; }
                    Role role = uniqueSafeRole(guild, "nation-" + job.name);
                    if (role != null) { success(job, role.getId(), null); return; }
                    TDCManager.createDiscordRole(nation, id -> result(job, id, null));
                    break;
                }
                case "TOWN_CATEGORY":
                case "NATION_CATEGORY": {
                    for (Category category : guild.getCategoriesByName(job.name, true)) {
                        if (privateCategory(guild, category)
                                && !categoryIds.contains(category.getId())) {
                            // Reconcile a Discord category created just before a crash.
                            success(job, category.getId(), null);
                            return;
                        }
                    }
                    TDCManager.createChatCategory(job.name, id -> result(job, id, null));
                    break;
                }
                case "TOWN_TEXT":
                case "NATION_TEXT":
                case "TOWN_VOICE":
                case "NATION_VOICE":
                    dispatchChannel(job, guild);
                    break;
                default:
                    fail(job, "Unrecognized task kind: " + job.kind);
            }
        } catch (Exception e) {
            fail(job, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }



    private void dispatchDeletion(Job job, Guild guild, String mappedId) {
        try {
            boolean town = job.kind.startsWith("DELETE_TOWN");
            boolean nation = job.kind.startsWith("DELETE_NATION");
            if (!town && !nation) { fail(job, "Unknown deletion task"); return; }

            // A deleted name may have been recreated or a rename rolled back.
            if (town && TownyUniverse.getInstance().getTown(job.name) != null) {
                discard(job); return;
            }
            if (nation && TownyUniverse.getInstance().getNation(job.name) != null) {
                discard(job); return;
            }
            if (mappedId == null || !mappedId.matches("[0-9]{1,24}")) {
                discard(job); // No confirmed SQL ownership mapping.
                return;
            }
            if (job.kind.endsWith("ROLE")) {
                Role role = guild.getRoleById(mappedId);
                if (role == null) { deleted(job, mappedId); return; }
                String expected = (town ? "town-" : "nation-") + job.name;
                if (!role.getName().equalsIgnoreCase(expected)) {
                    fail(job, "Role " + mappedId + " renamed/mismatched; refusing automatic deletion");
                    return;
                }
                role.delete().queue(ok -> deleted(job,mappedId), error -> fail(job,
                        "Discord role deletion error: " + error.getMessage()));
                return;
            }
            if (job.kind.endsWith("TEXT")) {
                TextChannel channel = guild.getTextChannelById(mappedId);
                if (channel == null) { deleted(job,mappedId); return; }
                String category = plugin.getConfig().getString(
                        "category." + (town ? "Town" : "Nation") + ".Name", "");
                if (!channel.getName().equalsIgnoreCase(job.name) ||
                        channel.getParent() == null ||
                        !channel.getParent().getName().equalsIgnoreCase(category)) {
                    fail(job, "Text channel " + mappedId + " was repurposed; refusing deletion");
                    return;
                }
                channel.delete().queue(ok -> deleted(job,mappedId), error -> fail(job,
                        "Discord channel deletion error: " + error.getMessage()));
                return;
            }
            if (job.kind.endsWith("VOICE")) {
                VoiceChannel channel = guild.getVoiceChannelById(mappedId);
                if (channel == null) { deleted(job,mappedId); return; }
                String category = plugin.getConfig().getString(
                        "category." + (town ? "Town" : "Nation") + ".Name", "");
                if (!channel.getName().equalsIgnoreCase(job.name) ||
                        channel.getParent() == null ||
                        !channel.getParent().getName().equalsIgnoreCase(category)) {
                    fail(job, "Voice channel " + mappedId + " was repurposed; refusing deletion");
                    return;
                }
                channel.delete().queue(ok -> deleted(job,mappedId), error -> fail(job,
                        "Discord channel deletion error: " + error.getMessage()));
                return;
            }
            fail(job,"Unsupported deletion kind " + job.kind);
        } catch (Throwable error) {
            fail(job,"Deletion dispatch error: " + error.getMessage());
        }
    }

    private void deleted(Job job, String remoteId) {
        finalizeAsync(job, () -> store.succeedDeletion(job,remoteId), "Deleted " + job);
    }

    /**
     * TDC-owned guild membership audit. Role IDs from historic SQL mappings
     * and current Towny entities establish ownership; a prefix alone does not.
     * Snapshot Towny on the main thread; persistence stays asynchronous.
     */
    private void inspectLinkedMember(Job job, Guild guild, Member member) {
        try {
            UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager()
                    .getLinkedAccounts().get(job.name);
            if (uuid == null && !plugin.getConfig().getBoolean(
                    "security.revokeUnlinkedAccounts", true)) {
                discard(job);
                return;
            }
            com.palmergames.bukkit.towny.object.Resident resident = uuid == null
                    ? null : TownyUniverse.getInstance().getResident(uuid);
            final Set<String> desired = new HashSet<String>();
            if (resident != null && resident.hasTown()) {
                try {
                    Town town = resident.getTown();
                    if (town != null) {
                        Role r = uniqueSafeRole(guild, "town-" + town.getName());
                        if (r != null) desired.add(r.getId());
                        if (town.hasNation()) {
                            Nation nation = town.getNation();
                            Role n = uniqueSafeRole(guild, "nation-" + nation.getName());
                            if (n != null) desired.add(n.getId());
                        }
                    }
                } catch (com.palmergames.bukkit.towny.exceptions.NotRegisteredException ex) {
                    defer(job, "Towny changed during membership audit");
                    return;
                }
            }
            final Set<String> actual = new HashSet<String>();
            final Map<String,String> roleNames = new HashMap<String,String>();
            final Set<String> townNames = new HashSet<String>();
            final Set<String> nationNames = new HashSet<String>();
            for (Town town : TownyUniverse.getInstance().getTowns()) townNames.add(town.getName());
            for (Nation nation : TownyUniverse.getInstance().getNations()) nationNames.add(nation.getName());
            for (Role role : member.getRoles()) {
                actual.add(role.getId());
                roleNames.put(role.getId(), role.getName());
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Set<String> owned = new HashSet<String>(store.mappedIds("TOWN_ROLE").values());
                    owned.addAll(store.mappedIds("NATION_ROLE").values());
                    List<Grant> observed = new ArrayList<Grant>();
                    for (String id : actual) {
                        String name = roleNames.get(id);
                        boolean recognized = owned.contains(id) ||
                                (name.startsWith("town-") && townNames.contains(name.substring(5))) ||
                                (name.startsWith("nation-") && nationNames.contains(name.substring(7)));
                        if (!recognized) continue;
                        observed.add(new Grant(job.name, id));
                        if (!desired.contains(id))
                            memberStore.desire(guild.getId(), job.name, id, false);
                    }
                    memberStore.observe(guild.getId(), observed);
                    for (String id : desired) if (!actual.contains(id))
                        memberStore.desire(guild.getId(), job.name, id, true);
                    store.remove(job);
                    plugin.getLogger().info("Audited linked Discord member " + job.name);
                } catch (Exception e) {
                    try { store.fail(job, "Member audit failed: " + e.getMessage()); }
                    catch (SQLException db) {
                        plugin.getLogger().severe("Member audit SQL error " + db.getMessage());
                    }
                } finally {
                    if (current == job) {
                        current = null;
                        resourceBusy.set(false);
                    }
                }
            });
        } catch (Throwable error) {
            fail(job, "Member audit dispatch error: " + error.getMessage());
        }
    }

    private void dispatchChannel(Job job, Guild guild) {
        boolean town = job.kind.startsWith("TOWN");
        boolean text = job.kind.endsWith("TEXT");
        String prefix = town ? "town-" : "nation-";
        Role role = uniqueSafeRole(guild, prefix + job.name);
        if (role == null) {
            defer(job, "Waiting for Discord role " + prefix + job.name);
            return;
        }
        String categoryName = plugin.getConfig().getString(
                "category." + (town ? "Town" : "Nation") + ".Name", "");
        List<Category> categories = new ArrayList<Category>();
        for (Category category : guild.getCategoriesByName(categoryName, true)) {
            if (privateCategory(guild, category)) categories.add(category);
        }
        if (categories.isEmpty()) {
            defer(job, "Waiting for " + categoryName + " category");
            return;
        }

        // After an interrupted API callback, reuse the previously created
        // channel instead of making duplicates. Match both parent and role.
        if (text) {
            for (TextChannel channel : guild.getTextChannelsByName(job.name, true)) {
                if (channel.getParent() != null
                        && categoryName.equalsIgnoreCase(channel.getParent().getName())
                        && channel.getPermissionOverride(role) != null
                        && channel.getPermissionOverride(guild.getPublicRole()) != null
                        && channel.getPermissionOverride(guild.getPublicRole())
                            .getDenied().contains(Permission.VIEW_CHANNEL)) {
                    success(job, channel.getId(), channel.getParent().getId());
                    return;
                }
            }
        } else {
            for (VoiceChannel channel : guild.getVoiceChannelsByName(job.name, true)) {
                if (channel.getParent() != null
                        && categoryName.equalsIgnoreCase(channel.getParent().getName())
                        && channel.getPermissionOverride(role) != null) {
                    success(job, channel.getId(), channel.getParent().getId());
                    return;
                }
            }
        }

        int maxChannels = Math.max(1, Math.min(50, plugin.getConfig().getInt("category.SubChannelsMax", 50)));
        Category target = null;
        for (Category category : categories) {
            if (category.getChannels().size() < maxChannels) {
                target = category;
                break;
            }
        }
        if (target == null) {
            defer(job, "All " + categoryName + " categories are full; waiting for the next category");
            return;
        }
        final String parent = target.getId();
        Consumer<String> finished = id -> result(job, id, parent);
        if (town && text) TDCManager.createTownTextChannel(job.name, role.getId(), parent, finished::accept);
        else if (town) TDCManager.createTownVoiceChannel(job.name, role.getId(), parent, finished::accept);
        else if (text) TDCManager.createNationTextChannel(job.name, role.getId(), parent, finished::accept);
        else TDCManager.createNationVoiceChannel(job.name, role.getId(), parent, finished::accept);
    }

    private static boolean privateCategory(Guild guild, Category category) {
        return category.getPermissionOverride(guild.getPublicRole()) != null
                && category.getPermissionOverride(guild.getPublicRole())
                    .getDenied().contains(Permission.VIEW_CHANNEL);
    }

    private static Role uniqueSafeRole(Guild guild, String name) {
        List<Role> roles = guild.getRolesByName(name, true);
        if (roles.size() > 1)
            throw new IllegalStateException("Multiple Discord roles named " + name
                    + "; manual administrator mapping is required");
        if (roles.isEmpty()) return null;
        Role role = roles.get(0);
        if (!role.getPermissions().isEmpty())
            throw new IllegalStateException("Discord role " + name
                    + " has guild-level permissions; refusing to adopt");
        return role;
    }

    private static <T> T first(List<T> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    private void result(Job job, String id, String parent) {
        if (id == null || id.trim().isEmpty()) fail(job, "Discord REST action failed; see DiscordSRV logs");
        else success(job, id, parent);
    }

    private void success(Job job, String id, String parent) {
        finalizeAsync(job, () -> store.succeed(job, id, parent), "Completed " + job);
    }

    private void discard(Job job) {
        // A deleted Towny entity must never cause an unbounded retry loop.
        finalizeAsync(job, () -> store.remove(job), "Discarded missing Towny entity " + job);
    }

    private void defer(Job job, String reason) {
        finalizeAsync(job, () -> store.defer(job, reason), "Deferred " + job + ": " + reason);
    }

    private void fail(Job job, String reason) {
        finalizeAsync(job, () -> store.fail(job, reason), "Failed " + job + ": " + reason);
    }

    private interface SQLAction { void run() throws SQLException; }

    private void finalizeAsync(final Job job, final SQLAction action, final String message) {
        if (!plugin.isEnabled()) return; // lease will be recovered after restart
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override public void run() {
                try {
                    action.run();
                    plugin.getLogger().info(message);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Could not persist Discord job " + job
                            + ": " + e.getMessage() + "; lease will be retried");
                } finally {
                    // A callback may arrive after a newer attempt has started.
                    if (current == job) {
                        current = null;
                        resourceBusy.set(false);
                    }
                }
            }
        });
    }

    public void cancel() {
        scheduler.cancel();
        // RUNNING leases are intentionally left for recovery on next startup.
    }
}
