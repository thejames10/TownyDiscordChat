package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Queue;

import com.TownyDiscordChat.TownyDiscordChat.Core.TDCManager;
import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.RoleSafety;
import com.TownyDiscordChat.TownyDiscordChat.Queue.DiscordJobStore;
import com.TownyDiscordChat.TownyDiscordChat.Queue.StableEntityStore;
import com.TownyDiscordChat.TownyDiscordChat.Queue.DiscordJobStore.Job;
import com.TownyDiscordChat.TownyDiscordChat.Queue.MemberIntentStore;
import com.TownyDiscordChat.TownyDiscordChat.Queue.MemberIntentStore.Intent;
import com.TownyDiscordChat.TownyDiscordChat.Queue.MemberIntentStore.Grant;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.requests.GatewayIntent;
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
    private final StableEntityStore stable;
    private final MemberIntentStore memberStore;
    private final BukkitTask scheduler;
    private final AtomicBoolean resourceBusy = new AtomicBoolean(false);
    private final AtomicBoolean memberBusy = new AtomicBoolean(false);
    private volatile Job current;
    private volatile Intent currentIntent;
    private volatile boolean warnedResourceLease;
    private volatile boolean warnedMemberLease;
    // Expired database leases may still have an unfinished REST request.
    // Quarantine only those job IDs; cap uncertainty to avoid flooding.
    private static final int MAX_UNRESOLVED_MEMBERS = 16;
    private static final int MAX_UNRESOLVED_RESOURCES = 4;
    private final java.util.concurrent.ConcurrentMap<Long,Intent> unresolvedMembers =
            new java.util.concurrent.ConcurrentHashMap<Long,Intent>();
    private final java.util.concurrent.ConcurrentMap<Long,Job> unresolvedResources =
            new java.util.concurrent.ConcurrentHashMap<Long,Job>();

    public Queue(long startDelaySeconds, long intervalDelaySeconds, Main plugin) {
        this.plugin = plugin;
        this.store = new DiscordJobStore(plugin);
        this.stable = new StableEntityStore(plugin);
        this.memberStore = new MemberIntentStore(plugin);
        long delay = Math.max(1, startDelaySeconds);
        long period = Math.max(1, intervalDelaySeconds);
        this.scheduler = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                new Runnable() {
                    @Override public void run() { tick(); }
                }, delay * 20L, period * 20L);
    }

    private void tick() {
        if (!plugin.isGuildValidated()) return;
        // Independent membership and resource lanes. Security revocations
        // never wait behind slow channel/category creation callbacks.
        tickMembers();
        tickResources();
    }

    private void tickMembers() {
        Intent active = currentIntent;
        if (active != null && System.currentTimeMillis() >= active.leaseUntil) {
            if (unresolvedMembers.size()>=MAX_UNRESOLVED_MEMBERS) {
                plugin.reportAdminWarning("Too many unresolved Discord membership callbacks. "
                        + "Pause new requests and inspect DiscordSRV before restarting.");
                return;
            }
            unresolvedMembers.putIfAbsent(active.id,active);
            currentIntent = null;
            memberBusy.set(false);
            if (!warnedMemberLease) {
                warnedMemberLease=true;
                plugin.reportAdminWarning("A membership REST callback exceeded its lease. "
                        + "Only that job ID is quarantined; unrelated revocations continue.");
            }
        }
        if (unresolvedMembers.size()>=MAX_UNRESOLVED_MEMBERS) return;
        if (!memberBusy.compareAndSet(false, true)) return;
        try {
            Set<Long> excluded=new HashSet<Long>(unresolvedMembers.keySet());
            Intent intent = memberStore.claim(true,excluded);
            if (intent == null) intent = memberStore.claim(false,excluded);
            if (intent == null) { memberBusy.set(false); return; }
            Set<String> owners = new HashSet<String>();
            owners.addAll(stable.ownersOfId(intent.guildId,"TOWN_ROLE",intent.roleId));
            owners.addAll(stable.ownersOfId(intent.guildId,"NATION_ROLE",intent.roleId));
            scheduleMember(intent,owners);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not claim membership intent: " + e.getMessage());
            currentIntent = null;
            memberBusy.set(false);
        }
    }

    private void tickResources() {
        Job active = current;
        if (active != null && System.currentTimeMillis() >= active.leaseUntil) {
            if (unresolvedResources.size()>=MAX_UNRESOLVED_RESOURCES) {
                plugin.reportAdminWarning("Too many unresolved Discord resource callbacks. "
                        + "Pause new requests and inspect Discord before restarting.");
                return;
            }
            unresolvedResources.putIfAbsent(active.id,active);
            current = null;
            resourceBusy.set(false);
            if (!warnedResourceLease) {
                warnedResourceLease=true;
                plugin.reportAdminWarning("A resource REST callback exceeded its lease. "
                        + "Only that job ID is quarantined; other deletions can proceed.");
            }
        }
        if (unresolvedResources.size()>=MAX_UNRESOLVED_RESOURCES) return;
        if (!resourceBusy.compareAndSet(false, true)) return;
        try {
            final Job job = store.claim(new HashSet<Long>(unresolvedResources.keySet()));
            if (job == null) { resourceBusy.set(false); return; }
            current = job;
            final Set<String> categoryIds = job.kind.endsWith("CATEGORY")
                    ? store.trackedCategoryIds(job.kind) : Collections.<String>emptySet();
            final Guild guild = DiscordSRV.getPlugin().getMainGuild();
            String mapped = job.kind.startsWith("DELETE_")
                    ? store.mappedId(job.kind, job.name) : null;
            if (mapped == null && guild != null && job.kind.startsWith("DELETE_")) {
                final String type = job.kind.contains("TOWN_") ? "TOWN" : "NATION";
                final String component = job.kind.endsWith("ROLE") ? "ROLE"
                        : job.kind.endsWith("TEXT") ? "TEXT" : "VOICE";
                mapped=stable.uniqueIdForName(guild.getId(),type,job.name,component);
            }
            final String mappedId = mapped;
            final Set<String> stableOwners = job.kind.startsWith("DELETE_") && guild != null
                    ? stable.ownersOfId(guild.getId(),job.kind,mappedId)
                    : Collections.<String>emptySet();
            final StableEntityStore.Entity identity = job.kind.startsWith("RENAME_") && guild != null
                    ? stable.find(guild.getId(),job.kind.contains("_TOWN_") ? "TOWN" : "NATION",job.name)
                    : null;
            final Set<String> auditIds = "AUDIT_GUILD".equals(job.kind)
                    ? mappedRolesForAudit() : Collections.<String>emptySet();
            // Check immutable ownership before processing a named resource.
            // A different UUID may have acquired the same name after deletion.
            final Set<String> nameOwners = (guild != null && !job.kind.startsWith("DELETE_")
                    && !job.kind.startsWith("RENAME_")
                    && (job.kind.startsWith("TOWN_") || job.kind.startsWith("NATION_"))
                    && !job.kind.endsWith("CATEGORY"))
                    ? stable.ownersOfName(guild.getId(),
                            job.kind.startsWith("TOWN_") ? "TOWN" : "NATION",job.name)
                    : Collections.<String>emptySet();
            Bukkit.getScheduler().runTask(plugin, () -> dispatch(job, categoryIds, mappedId,
                    stableOwners,identity,auditIds,nameOwners));
        } catch (Exception e) {
            plugin.getLogger().warning("Could not claim resource job: " + e.getMessage());
            // SQL lease safely recovers a claim if the callback never started.
            current = null;
            resourceBusy.set(false);
        }
    }

    private void scheduleMember(Intent intent, Set<String> owners) {
        currentIntent = intent;
        Bukkit.getScheduler().runTask(plugin, () -> dispatchMember(intent,owners));
    }

    private void dispatchMember(Intent intent, Set<String> owners) {
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
            if (intent.desired && !authorizedNow(intent,role,owners)) {
                if (owners.isEmpty()) {
                    deferMember(intent,"No stable Towny UUID mapping exists for role " + role.getId());
                } else {
                    // The queued grant may refer to a town that renamed,
                    // was deleted or removed this resident in the meantime.
                    finalizeMemberAsync(intent, () -> {
                        memberStore.desire(intent.guildId,intent.memberId,intent.roleId,false);
                        memberStore.succeed(intent);
                    }, "Cancelled obsolete grant for " + intent.memberId);
                }
                return;
            }
            String permissionProblem=RoleSafety.problem(role);
            if (permissionProblem != null) {
                plugin.reportAdminWarning("Tracked role " + role.getId()
                        + " cannot be changed automatically: " + permissionProblem);
                failMember(intent, "Unsafe role; " + permissionProblem);
                return;
            }
            if (!intent.desired && owners.isEmpty()) {
                // A name prefix by itself is not ownership proof. Once a
                // legacy role is linked to a Towny UUID reconciliation can
                // safely revoke it, even when the town no longer exists.
                plugin.reportAdminWarning("Tracked role " + role.getId()
                        + " has no verified Towny UUID owner. Refusing automated revocation.");
                failMember(intent,"Cannot revoke unverified Discord role; manual review required");
                return;
            }
            Member member = guild.getMemberById(intent.memberId);
            if (member != null && intent.desired) {
                applyMember(intent, guild, member, role, owners);
            } else {
                // Do not infer missing role membership from an incomplete JDA
                // member cache; check the API instead. JDA completion runs
                // off-thread: return to Bukkit before touching Towny state.
                guild.retrieveMemberById(intent.memberId).queue(
                    found -> {
                        if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin,
                                () -> applyMember(intent, guild, found, role, owners));
                    },
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

    /**
     * Revalidate authority immediately before submitting an access grant.
     * A name-reused role owned by a deleted town is never granted to the
     * replacement town, even if Discord still caches it under the same name.
     */
    private boolean authorizedNow(Intent intent, Role role, Set<String> owners) {
        if (owners.isEmpty()) return false;
        String roleName = role.getName();
        boolean townRole = roleName.startsWith("town-");
        boolean nationRole = roleName.startsWith("nation-");
        if (!townRole && !nationRole) return false;
        try {
            java.util.Map<String,UUID> accounts=DiscordSRV.getPlugin()
                    .getAccountLinkManager().getLinkedAccounts();
            if (accounts==null) return false;
            UUID uuid=accounts.get(intent.memberId);
            if (uuid==null) return false;
            com.palmergames.bukkit.towny.object.Resident resident =
                    TownyUniverse.getInstance().getResident(uuid);
            if (resident==null || !resident.hasTown()) return false;
            Town town=resident.getTown();
            if (town==null) return false;
            if (townRole) return owners.contains(town.getUUID().toString());
            if (!town.hasNation()) return false;
            Nation nation=town.getNation();
            return nation != null && owners.contains(nation.getUUID().toString());
        } catch (com.palmergames.bukkit.towny.exceptions.NotRegisteredException e) {
            return false;
        }
    }

    private static boolean isUnknownMember(Throwable failure) {
        return failure instanceof ErrorResponseException
                && ((ErrorResponseException) failure).getErrorCode() == 10007;
    }

    private void applyMember(Intent intent, Guild guild, Member member, Role role,
                             Set<String> owners) {
        try {
            // A kick/unlink may occur while Discord's REST lookup is in flight.
            // Verify again immediately before queuing a permission grant.
            if (intent.desired && !authorizedNow(intent, role, owners)) {
                finalizeMemberAsync(intent, () -> {
                    memberStore.desire(intent.guildId,intent.memberId,intent.roleId,false);
                    memberStore.succeed(intent);
                }, "Cancelled grant after Towny membership changed: " + intent.memberId);
                return;
            }
            // Discord role settings may change during the asynchronous REST
            // lookup. Never grant or revoke a now-privileged/repurposed role.
            String permissionProblem=RoleSafety.problem(role);
            if (permissionProblem != null ||
                    !(role.getName().startsWith("town-")
                    || role.getName().startsWith("nation-"))) {
                failMember(intent,"Unsafe role at membership dispatch: "
                        + (permissionProblem == null ? "role name repurposed" : permissionProblem));
                return;
            }
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
                unresolvedMembers.remove(intent.id,intent);
                if (currentIntent == intent) {
                    currentIntent = null;
                    memberBusy.set(false);
                }
                if (unresolvedMembers.isEmpty()) warnedMemberLease=false;
            }
        });
    }

    private void dispatch(final Job job, Set<String> categoryIds, String mappedId,
                          Set<String> stableOwners, StableEntityStore.Entity identity,
                          Set<String> auditIds, Set<String> nameOwners) {
        try {
            Guild guild = DiscordSRV.getPlugin().getMainGuild();
            if (guild == null) {
                defer(job, "DiscordSRV guild is not ready");
                return;
            }
            if (job.kind.startsWith("DELETE_")) {
                dispatchDeletion(job, guild, mappedId, stableOwners);
                return;
            }
            if (job.kind.startsWith("RENAME_")) {
                dispatchRename(job,guild,identity);
                return;
            }
            if ((job.kind.startsWith("TOWN_") || job.kind.startsWith("NATION_"))
                    && !job.kind.endsWith("CATEGORY")) {
                String uuid = currentTownyUuid(job.kind,job.name);
                if (uuid == null) {
                    recoverDeletedCreate(job,guild,nameOwners);
                    return;
                }
                if (nameOwners.size()!=1 || !nameOwners.contains(uuid)) {
                    if (nameOwners.size()>1)
                        plugin.reportAdminWarning("Ambiguous Towny UUID ownership of "
                                + job.kind + "/" + job.name
                                + "; repair duplicate stable identity rows before creating.");
                    defer(job, "Waiting for unique verified Towny UUID ownership of "
                            + job.kind + "/" + job.name);
                    return;
                }
            }
            switch (job.kind) {
                case "AUDIT_GUILD": {
                    dispatchFullGuildAudit(job,guild,auditIds);
                    break;
                }
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
                    int configuredMax = Math.max(1,Math.min(50,
                            plugin.getConfig().getInt("category.SubChannelsMax",50)));
                    for (Category category : guild.getCategoriesByName(job.name, true)) {
                        if (!privateCategory(guild, category)) continue;
                        if (!categoryIds.contains(category.getId())
                                || category.getChannels().size() < configuredMax) {
                            // Adopt an untracked resource OR acknowledge an
                            // already-satisfied legacy/migration request.
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



    /**
     * A Towny object may disappear while its remote CREATE was in flight.
     * When recovering an expired lease, never drop proof of its newly created
     * Discord object. Verify a unique private resource, persist its ID under
     * the deleted UUID, then reconcile promptly to delete it by ID.
     *
     * This never sends a new REST creation request.
     */
    private void recoverDeletedCreate(Job job,Guild guild,Set<String> nameOwners) {
        if (nameOwners.isEmpty()) {
            discard(job);
            return;
        }
        if (nameOwners.size()!=1) {
            fail(job,"Multiple stable UUID owners for deleted "+job.kind+"/"+job.name);
            plugin.reportAdminWarning("Ambiguous deleted Towny identity "+job.name
                    + "; manual cleanup required.");
            return;
        }
        boolean town=job.kind.startsWith("TOWN_");
        String roleName=(town ? "town-" : "nation-")+job.name;
        if (job.kind.endsWith("ROLE")) {
            Role role=uniqueSafeRole(guild,roleName);
            if (role==null) discard(job);
            else orphanFound(job,role.getId(),null);
            return;
        }
        String categoryName=plugin.getConfig().getString(
                "category."+(town ? "Town" : "Nation")+".Name","");
        Role role=uniqueSafeRole(guild,roleName);
        if (job.kind.endsWith("TEXT")) {
            List<TextChannel> matches=new ArrayList<TextChannel>();
            for (TextChannel channel:guild.getTextChannelsByName(job.name,true))
                if (channel.getParent()!=null
                        && categoryName.equalsIgnoreCase(channel.getParent().getName()))
                    matches.add(channel);
            if (matches.isEmpty()) { discard(job); return; }
            if (matches.size()!=1 || role==null || !safeExistingText(guild,matches.get(0),role)) {
                fail(job,"Unsafe/ambiguous orphan text channel for "+job.name);
                plugin.reportAdminWarning("Unverified orphan Discord text channel "
                        +job.name+" needs manual review.");
                return;
            }
            TextChannel channel=matches.get(0);
            orphanFound(job,channel.getId(),channel.getParent().getId());
            return;
        }
        if (job.kind.endsWith("VOICE")) {
            List<VoiceChannel> matches=new ArrayList<VoiceChannel>();
            for (VoiceChannel channel:guild.getVoiceChannelsByName(job.name,true))
                if (channel.getParent()!=null
                        && categoryName.equalsIgnoreCase(channel.getParent().getName()))
                    matches.add(channel);
            if (matches.isEmpty()) { discard(job); return; }
            if (matches.size()!=1 || role==null || !safeExistingVoice(guild,matches.get(0),role)) {
                fail(job,"Unsafe/ambiguous orphan voice channel for "+job.name);
                plugin.reportAdminWarning("Unverified orphan Discord voice channel "
                        +job.name+" needs manual review.");
                return;
            }
            VoiceChannel channel=matches.get(0);
            orphanFound(job,channel.getId(),channel.getParent().getId());
            return;
        }
        fail(job,"Unknown removed-Towny create type "+job.kind);
    }

    private void orphanFound(Job job,String remoteId,String parentId) {
        finalizeAsync(job, () -> {
            store.succeed(job,remoteId,parentId);
            if (plugin.reconciler!=null) plugin.reconciler.requestSoon();
        }, "Recovered resource belonging to deleted Towny identity: "+job);
    }

    private static boolean safeExistingText(Guild guild,TextChannel channel,Role role) {
        return channel.getParent()!=null && privateCategory(guild,channel.getParent())
                && channel.getPermissionOverride(guild.getPublicRole())!=null
                && channel.getPermissionOverride(guild.getPublicRole()).getDenied()
                    .contains(Permission.VIEW_CHANNEL)
                && channel.getPermissionOverride(role)!=null
                && channel.getPermissionOverride(role).getAllowed().contains(Permission.VIEW_CHANNEL);
    }

    private static boolean safeExistingVoice(Guild guild,VoiceChannel channel,Role role) {
        return channel.getParent()!=null && privateCategory(guild,channel.getParent())
                && channel.getPermissionOverride(guild.getPublicRole())!=null
                && channel.getPermissionOverride(guild.getPublicRole()).getDenied()
                    .contains(Permission.VIEW_CHANNEL)
                && channel.getPermissionOverride(role)!=null
                && channel.getPermissionOverride(role).getAllowed().contains(Permission.VIEW_CHANNEL);
    }

    private String currentTownyUuid(String kind, String name) {
        if (kind.startsWith("TOWN_")) {
            Town town=TownyUniverse.getInstance().getTown(name);
            return town==null ? null : town.getUUID().toString();
        }
        Nation nation=TownyUniverse.getInstance().getNation(name);
        return nation==null ? null : nation.getUUID().toString();
    }

    private Set<String> mappedRolesForAudit() throws SQLException {
        Set<String> roles = new HashSet<String>();
        roles.addAll(store.mappedIds("TOWN_ROLE").values());
        roles.addAll(store.mappedIds("NATION_ROLE").values());
        roles.remove(null);
        return roles;
    }

    /**
     * Guild member chunking is the only complete supported enumeration of
     * uncached users. It requires Discord's privileged GUILD_MEMBERS intent.
     * Never confuse Guild#getMembers() with a full guild snapshot.
     */
    private void dispatchFullGuildAudit(Job job, Guild guild, Set<String> recordedIds) {
        if (!guild.getId().equals(job.name)) {
            discard(job);
            return;
        }
        if (!guild.getJDA().getGatewayIntents().contains(GatewayIntent.GUILD_MEMBERS)) {
            String warning = "Complete Discord membership audit requires Server Members Intent "
                    + "enabled for the bot AND GUILD_MEMBERS intent enabled in DiscordSRV. "
                    + "Linked accounts continue to receive individual audits.";
            plugin.reportAdminWarning(warning);
            fail(job,warning);
            return;
        }
        try {
            final Set<String> owned = new HashSet<String>();
            for (String recordedId : recordedIds) {
                Role role = guild.getRoleById(recordedId);
                if (role != null && RoleSafety.safeForMembership(role)
                        && (role.getName().startsWith("town-")
                        || role.getName().startsWith("nation-")))
                    owned.add(recordedId);
            }
            // A first-time installation has no SQL mappings. Only explicitly
            // named, non-privileged town/nation roles may be auto-adopted.
            Set<String> townNames = new HashSet<String>();
            Set<String> nationNames = new HashSet<String>();
            for (Town town : TownyUniverse.getInstance().getTowns()) townNames.add(town.getName());
            for (Nation nation : TownyUniverse.getInstance().getNations()) nationNames.add(nation.getName());
            for (Role role : guild.getRoles()) {
                if (!RoleSafety.safeForMembership(role)) continue;
                String name = role.getName();
                if (((name.startsWith("town-") && townNames.contains(name.substring(5)))
                        || (name.startsWith("nation-") && nationNames.contains(name.substring(7))))
                        && guild.getRolesByName(name,true).size()==1)
                    owned.add(role.getId());
            }
            final List<Grant> observed = Collections.synchronizedList(new ArrayList<Grant>());
            guild.loadMembers(member -> {
                for (Role role : member.getRoles()) {
                    if (owned.contains(role.getId()))
                        observed.add(new Grant(member.getId(),role.getId()));
                }
            }).onSuccess(complete -> {
                if (!plugin.isEnabled()) return;
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        memberStore.observe(guild.getId(),observed);
                        store.finishFullAudit(job);
                        plugin.getLogger().info("Complete guild audit recorded "
                                + observed.size() + " managed grants. Reconciliation will revoke stale access.");
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (plugin.reconciler != null) plugin.reconciler.requestSoon();
                        });
                    } catch (SQLException e) {
                        plugin.getLogger().severe("Could not checkpoint guild audit: " + e.getMessage());
                    } finally {
                        unresolvedResources.remove(job.id,job);
                        if (current == job) {
                            current = null;
                            resourceBusy.set(false);
                        }
                        if (unresolvedResources.isEmpty()) warnedResourceLease=false;
                    }
                });
            }).onError(error -> {
                plugin.reportAdminWarning("Full Discord guild member audit failed: "
                        + error.getMessage());
                fail(job,"Guild chunking failed: " + error.getMessage());
            });
        } catch (Throwable error) {
            plugin.reportAdminWarning("Full guild audit unavailable: " + error.getMessage());
            fail(job,"Guild audit unavailable: " + error.getMessage());
        }
    }

    /**
     * Rename the existing Discord resource in-place. Towny UUID, not the
     * mutable name, is the job key; role/channel Discord IDs remain unchanged.
     */
    private void dispatchRename(Job job, Guild guild, StableEntityStore.Entity entity) {
        if (entity == null) { discard(job); return; }
        String kind = job.kind.contains("_TOWN_") ? "TOWN" : "NATION";
        String component = job.kind.endsWith("ROLE") ? "ROLE"
                : job.kind.endsWith("TEXT") ? "TEXT" : "VOICE";
        String prefix = "TOWN".equals(kind) ? "town-" : "nation-";
        boolean valid = false;
        if ("TOWN".equals(kind)) {
            for (Town town : TownyUniverse.getInstance().getTowns()) {
                if (job.name.equals(town.getUUID().toString())
                        && entity.name.equals(town.getName())) { valid=true; break; }
            }
        } else {
            for (Nation nation : TownyUniverse.getInstance().getNations()) {
                if (job.name.equals(nation.getUUID().toString())
                        && entity.name.equals(nation.getName())) { valid=true; break; }
            }
        }
        if (!valid) { discard(job); return; }
        String id=entity.idFor(component);
        if (id==null) { discard(job); return; }
        try {
            if ("ROLE".equals(component)) {
                Role role=guild.getRoleById(id);
                if (role==null) { discard(job); return; }
                if (!RoleSafety.safeForMembership(role) || !role.getName().startsWith(prefix)) {
                    fail(job,"Refusing unsafe role rename: " + id); return;
                }
                String desired=prefix+entity.name;
                if (role.getName().equals(desired)) { discard(job); return; }
                role.getManager().setName(desired).queue(done -> discard(job),
                        error -> fail(job,"Discord role rename: " + error.getMessage()));
            } else {
                Category expected=null;
                String categoryName=plugin.getConfig().getString(
                        "category." + ("TOWN".equals(kind) ? "Town" : "Nation") + ".Name","");
                if ("TEXT".equals(component)) {
                    TextChannel channel=guild.getTextChannelById(id);
                    if (channel==null) { discard(job); return; }
                    expected=channel.getParent();
                    if (!safeRenameChannel(guild,expected,categoryName,
                            channel.getPermissionOverride(guild.getPublicRole())!=null
                            && channel.getPermissionOverride(guild.getPublicRole())
                                .getDenied().contains(Permission.VIEW_CHANNEL))) {
                        fail(job,"Unsafe text channel rename: " + id); return;
                    }
                    Role accessRole = entity.roleId == null ? null : guild.getRoleById(entity.roleId);
                    if (accessRole == null || channel.getPermissionOverride(accessRole) == null
                            || !channel.getPermissionOverride(accessRole).getAllowed()
                                .contains(Permission.VIEW_CHANNEL)) {
                        fail(job,"Channel role override no longer matches stable Towny identity: " + id);
                        return;
                    }
                    if (channel.getName().equals(entity.name)) { discard(job); return; }
                    channel.getManager().setName(entity.name).queue(done -> discard(job),
                            error -> fail(job,"Discord text rename: " + error.getMessage()));
                } else {
                    VoiceChannel channel=guild.getVoiceChannelById(id);
                    if (channel==null) { discard(job); return; }
                    expected=channel.getParent();
                    if (!safeRenameChannel(guild,expected,categoryName,
                            channel.getPermissionOverride(guild.getPublicRole())!=null
                            && channel.getPermissionOverride(guild.getPublicRole())
                                .getDenied().contains(Permission.VIEW_CHANNEL))) {
                        fail(job,"Unsafe voice channel rename: " + id); return;
                    }
                    Role accessRole = entity.roleId == null ? null : guild.getRoleById(entity.roleId);
                    if (accessRole == null || channel.getPermissionOverride(accessRole) == null
                            || !channel.getPermissionOverride(accessRole).getAllowed()
                                .contains(Permission.VIEW_CHANNEL)) {
                        fail(job,"Channel role override no longer matches stable Towny identity: " + id);
                        return;
                    }
                    if (channel.getName().equals(entity.name)) { discard(job); return; }
                    channel.getManager().setName(entity.name).queue(done -> discard(job),
                            error -> fail(job,"Discord voice rename: " + error.getMessage()));
                }
            }
        } catch (Throwable e) {
            fail(job,"Rename dispatch failed: " + e.getMessage());
        }
    }

    private static boolean safeRenameChannel(Guild guild,Category parent,
                                             String expectedCategory,boolean publicDenied) {
        return publicDenied && parent!=null
                && parent.getName().equalsIgnoreCase(expectedCategory)
                && privateCategory(guild,parent);
    }

    private void dispatchDeletion(Job job, Guild guild, String mappedId, Set<String> stableOwners) {
        try {
            boolean town = job.kind.startsWith("DELETE_TOWN");
            boolean nation = job.kind.startsWith("DELETE_NATION");
            if (!town && !nation) { fail(job, "Unknown deletion task"); return; }

            // A deleted name may have been recreated or a rename rolled back.
            // New Towny UUIDs may reuse the same name. Deletion is governed
            // by the stable UUID that owns the recorded remote ID, not by
            // whether a different town currently shares its old name.
            if (mappedId == null || !mappedId.matches("[0-9]{1,24}")) {
                discard(job); // No confirmed SQL ownership mapping.
                return;
            }
            if (stableOwners.isEmpty()) {
                fail(job, "No stable Towny UUID owns " + mappedId + "; manual cleanup required");
                return;
            }
            for (String owner : stableOwners) {
                if (town) {
                    for (Town existing : TownyUniverse.getInstance().getTowns())
                        if (owner.equals(existing.getUUID().toString())) {
                            discard(job); return; // Renamed town, NEVER delete.
                        }
                } else {
                    for (Nation existing : TownyUniverse.getInstance().getNations())
                        if (owner.equals(existing.getUUID().toString())) {
                            discard(job); return; // Renamed nation, NEVER delete.
                        }
                }
            }
            if (job.kind.endsWith("ROLE")) {
                Role role = guild.getRoleById(mappedId);
                if (role == null) { deleted(job, mappedId); return; }
                String expected = (town ? "town-" : "nation-") + job.name;
                if (!RoleSafety.safeForMembership(role) || !role.getName().equalsIgnoreCase(expected)) {
                    fail(job, "Role " + mappedId + " repurposed/mismatched; refusing automatic deletion");
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
                        !channel.getParent().getName().equalsIgnoreCase(category) ||
                        !privateCategory(guild, channel.getParent()) ||
                        channel.getPermissionOverride(guild.getPublicRole()) == null ||
                        !channel.getPermissionOverride(guild.getPublicRole()).getDenied()
                            .contains(Permission.VIEW_CHANNEL)) {
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
                        !channel.getParent().getName().equalsIgnoreCase(category) ||
                        !privateCategory(guild, channel.getParent()) ||
                        channel.getPermissionOverride(guild.getPublicRole()) == null ||
                        !channel.getPermissionOverride(guild.getPublicRole()).getDenied()
                            .contains(Permission.VIEW_CHANNEL)) {
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
            // Snapshot immutable Towny identities on the server thread.
            // Name lookup here would wrongly revoke during an in-place rename.
            String townUuid = null, nationUuid = null;
            if (resident != null && resident.hasTown()) {
                try {
                    Town town = resident.getTown();
                    if (town != null) {
                        townUuid = town.getUUID().toString();
                        if (town.hasNation()) {
                            Nation nation = town.getNation();
                            if (nation != null) nationUuid = nation.getUUID().toString();
                        }
                    }
                } catch (com.palmergames.bukkit.towny.exceptions.NotRegisteredException ex) {
                    defer(job, "Towny changed during membership audit");
                    return;
                }
            }
            final String expectedTownUuid = townUuid;
            final String expectedNationUuid = nationUuid;
            final Map<String,String> safeRoleNamesById = new HashMap<String,String>();
            final Set<String> uniqueSafeRoleIds = new HashSet<String>();
            for (Role candidate : guild.getRoles()) {
                if (!RoleSafety.safeForMembership(candidate)) continue;
                String name=candidate.getName();
                safeRoleNamesById.put(candidate.getId(),name);
                if (guild.getRolesByName(name,true).size()==1)
                    uniqueSafeRoleIds.add(candidate.getId());
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
                    final Set<String> desired = new HashSet<String>();
                    if (expectedTownUuid != null) {
                        StableEntityStore.Entity expected = stable.find(
                                guild.getId(), "TOWN", expectedTownUuid);
                        if (expected != null && expected.roleId != null
                                && safeRoleNamesById.containsKey(expected.roleId)
                                && safeRoleNamesById.get(expected.roleId).startsWith("town-"))
                            desired.add(expected.roleId);
                    }
                    if (expectedNationUuid != null) {
                        StableEntityStore.Entity expected = stable.find(
                                guild.getId(), "NATION", expectedNationUuid);
                        if (expected != null && expected.roleId != null
                                && safeRoleNamesById.containsKey(expected.roleId)
                                && safeRoleNamesById.get(expected.roleId).startsWith("nation-"))
                            desired.add(expected.roleId);
                    }
                    List<Grant> observed = new ArrayList<Grant>();
                    for (String id : actual) {
                        String name = roleNames.get(id);
                        boolean recognized = safeRoleNamesById.containsKey(id)
                                && (owned.contains(id)
                                || (name != null && uniqueSafeRoleIds.contains(id)
                                && ((name.startsWith("town-") && townNames.contains(name.substring(5)))
                                || (name.startsWith("nation-") && nationNames.contains(name.substring(7))))));
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
                    unresolvedResources.remove(job.id,job);
                    if (current == job) {
                        current = null;
                        resourceBusy.set(false);
                    }
                    if (unresolvedResources.isEmpty()) warnedResourceLease=false;
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

        // An interrupted callback may have created an object just before
        // timeout. Inspect ALL same-name objects in our managed category.
        // A single safe one is adopted; any ambiguous or unsafe candidate
        // is an operator conflict, not permission to create a duplicate.
        if (text) {
            List<TextChannel> candidates = new ArrayList<TextChannel>();
            for (TextChannel channel : guild.getTextChannelsByName(job.name, true)) {
                if (channel.getParent() != null
                        && categoryName.equalsIgnoreCase(channel.getParent().getName()))
                    candidates.add(channel);
            }
            if (candidates.size()>1)
                throw new IllegalStateException("Ambiguous Discord text channels for "
                        + job.kind + "/" + job.name + "; manual resolution required");
            if (!candidates.isEmpty()) {
                TextChannel channel=candidates.get(0);
                if (!privateCategory(guild,channel.getParent())
                        || channel.getPermissionOverride(guild.getPublicRole())==null
                        || !channel.getPermissionOverride(guild.getPublicRole()).getDenied()
                            .contains(Permission.VIEW_CHANNEL)
                        || channel.getPermissionOverride(role)==null
                        || !channel.getPermissionOverride(role).getAllowed()
                            .contains(Permission.VIEW_CHANNEL))
                    throw new IllegalStateException("Unsafe existing Discord text channel "
                            + channel.getId() + "; refusing duplicate creation");
                success(job,channel.getId(),channel.getParent().getId());
                return;
            }
        } else {
            List<VoiceChannel> candidates = new ArrayList<VoiceChannel>();
            for (VoiceChannel channel : guild.getVoiceChannelsByName(job.name, true)) {
                if (channel.getParent() != null
                        && categoryName.equalsIgnoreCase(channel.getParent().getName()))
                    candidates.add(channel);
            }
            if (candidates.size()>1)
                throw new IllegalStateException("Ambiguous Discord voice channels for "
                        + job.kind + "/" + job.name + "; manual resolution required");
            if (!candidates.isEmpty()) {
                VoiceChannel channel=candidates.get(0);
                if (!privateCategory(guild,channel.getParent())
                        || channel.getPermissionOverride(guild.getPublicRole())==null
                        || !channel.getPermissionOverride(guild.getPublicRole()).getDenied()
                            .contains(Permission.VIEW_CHANNEL)
                        || channel.getPermissionOverride(role)==null
                        || !channel.getPermissionOverride(role).getAllowed()
                            .contains(Permission.VIEW_CHANNEL))
                    throw new IllegalStateException("Unsafe existing Discord voice channel "
                            + channel.getId() + "; refusing duplicate creation");
                success(job,channel.getId(),channel.getParent().getId());
                return;
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
        String problem=RoleSafety.problem(role);
        if (problem != null)
            throw new IllegalStateException("Discord role " + name
                    + " is unsafe to adopt: " + problem);
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
                    unresolvedResources.remove(job.id,job);
                    if (current == job) {
                        current = null;
                        resourceBusy.set(false);
                    }
                    if (unresolvedResources.isEmpty()) warnedResourceLease=false;
                }
            }
        });
    }

    public void cancel() {
        scheduler.cancel();
        // RUNNING leases are intentionally left for recovery on next startup.
    }
}
