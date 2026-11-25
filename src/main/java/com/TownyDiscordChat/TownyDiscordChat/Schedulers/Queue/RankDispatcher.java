package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Queue;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.Queue.DiscordJobStore.Job;
import com.TownyDiscordChat.TownyDiscordChat.Queue.QueueMessages;
import com.TownyDiscordChat.TownyDiscordChat.Queue.RankGrantStore;
import com.TownyDiscordChat.TownyDiscordChat.Queue.StableEntityStore;
import com.TownyDiscordChat.TownyDiscordChat.Ranks.RankHolders;
import com.TownyDiscordChat.TownyDiscordChat.Ranks.RankPermissions;
import com.TownyDiscordChat.TownyDiscordChat.Ranks.RankSettings;
import com.TownyDiscordChat.TownyDiscordChat.Ranks.RankSlot;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.GuildChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.PermissionOverride;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Converges one member's moderation permissions on one town or nation
 * channel. Leadership is re-checked against Towny when the job runs, so a
 * stale job can never grant access the snapshot no longer supports.
 */
final class RankDispatcher {
    private static final String GRANTED = "GRANTED";
    private static final String REVOKED = "REVOKED";
    private static final String CLEARED = "CLEARED";

    /** How the queue finishes a rank job. */
    interface Outcome {
        void done(String message);

        void skip(String reason);

        void defer(String reason);

        void fail(String reason);
    }

    /** SQL-backed facts loaded off the server thread before dispatch. */
    static final class Target {
        final Job job;
        final String guildId;
        final RankSlot slot;
        final Set<String> owners;
        final UUID player;
        final boolean recorded;

        Target(Job job, String guildId, RankSlot slot, Set<String> owners, UUID player, boolean recorded) {
            this.job = job;
            this.guildId = guildId;
            this.slot = slot;
            this.owners = owners;
            this.player = player;
            this.recorded = recorded;
        }
    }

    private final Main plugin;
    private final RankGrantStore grants;
    private final StableEntityStore stable;

    RankDispatcher(Main plugin, RankGrantStore grants, StableEntityStore stable) {
        this.plugin = plugin;
        this.grants = grants;
        this.stable = stable;
    }

    /** Runs on the async claim thread. */
    Target load(String guildId, Job job) throws SQLException {
        RankSlot slot = RankSlot.fromJob(job.kind, job.name);
        if (slot == null) {
            return new Target(job, guildId, null, Collections.<String>emptySet(), null, false);
        }
        Set<String> owners = stable.ownersOfId(guildId, slot.channelKind(), slot.channelId());
        UUID player = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(slot.memberId());
        return new Target(job, guildId, slot, owners, player, grants.isRecorded(guildId, slot));
    }

    /** Runs on the global region thread. */
    void dispatch(Guild guild, Target target, Outcome outcome) {
        if (target == null || target.slot == null) {
            outcome.skip("not a rank job written by this plugin");
            return;
        }
        if (!guild.getId().equals(target.guildId)) {
            outcome.defer("Discord guild changed while the job was queued");
            return;
        }
        GuildChannel channel = guild.getGuildChannelById(target.slot.channelId());
        if (channel == null) {
            forget(target, outcome, message(CLEARED, target, null, guild) + " (channel no longer exists)");
            return;
        }
        if (shouldHold(target)) {
            grant(guild, channel, target, outcome);
        } else {
            revoke(guild, channel, target, outcome);
        }
    }

    private boolean shouldHold(Target target) {
        RankSettings settings = RankSettings.fromConfig(plugin.getConfig());
        if (!settings.enabled() || target.player == null || target.owners.size() != 1) {
            return false;
        }
        UUID owner = parseUuid(target.owners.iterator().next());
        if (owner == null) {
            return false;
        }
        if (target.slot.isTown()) {
            Town town = TownyUniverse.getInstance().getTown(owner);
            return town != null && RankHolders.townLeaders(town, settings).contains(target.player);
        }
        Nation nation = TownyUniverse.getInstance().getNation(owner);
        return nation != null && RankHolders.nationLeaders(nation, settings).contains(target.player);
    }

    private void grant(Guild guild, GuildChannel channel, Target target, Outcome outcome) {
        Set<Permission> managed = RankPermissions.forSlot(target.slot);
        PermissionOverride override = memberOverride(channel, target.slot.memberId());
        if (RankPermissions.deniesAny(override, managed)) {
            outcome.skip("an explicit deny on " + channel.getName() + " blocks moderation for this member");
            return;
        }
        if (RankPermissions.allows(override, managed)) {
            outcome.skip("member already has these permissions on " + channel.getName());
            return;
        }
        // Record before granting: a crash between the two then leaves a
        // harmless record, never an untracked permission nobody revokes.
        plugin.scheduler().runAsync(() -> {
            try {
                grants.record(target.guildId, target.slot);
                plugin.scheduler().runGlobal(() -> applyGrant(guild, channel, target, outcome));
            } catch (SQLException error) {
                outcome.defer("could not record the rank grant: " + error.getMessage());
            }
        });
    }

    private void applyGrant(Guild guild, GuildChannel channel, Target target, Outcome outcome) {
        long allow = Permission.getRaw(RankPermissions.forSlot(target.slot));
        PermissionOverride override = memberOverride(channel, target.slot.memberId());
        if (override != null) {
            finish(override.getManager().grant(allow), message(GRANTED, target, channel, guild), outcome);
            return;
        }
        guild.retrieveMemberById(target.slot.memberId()).queue(
                member -> finish(channel.createPermissionOverride(member).setAllow(allow),
                        message(GRANTED, target, channel, guild), outcome),
                error -> {
                    if (Queue.isUnknownMember(error)) {
                        forget(target, outcome, message(CLEARED, target, channel, guild) + " (member left)");
                    } else {
                        outcome.defer("Discord member lookup failed: " + error.getMessage());
                    }
                });
    }

    private void revoke(Guild guild, GuildChannel channel, Target target, Outcome outcome) {
        if (!target.recorded) {
            outcome.skip("no moderation permissions from this plugin to remove");
            return;
        }
        long managed = Permission.getRaw(RankPermissions.forSlot(target.slot));
        PermissionOverride override = memberOverride(channel, target.slot.memberId());
        if (override == null || (override.getAllowedRaw() & managed) == 0) {
            forget(target, outcome, message(CLEARED, target, channel, guild) + " (already absent)");
            return;
        }
        removal(override, managed).queue(ok -> forget(target, outcome, message(REVOKED, target, channel, guild)),
                error -> outcome.fail("could not remove rank permissions: " + error.getMessage()));
    }

    /** Deletes an override that held nothing else, so no empty override is left behind. */
    private static RestAction<?> removal(PermissionOverride override, long managed) {
        boolean nothingElse = (override.getAllowedRaw() & ~managed) == 0 && override.getDeniedRaw() == 0;
        if (nothingElse) {
            return override.delete();
        }
        return override.getManager().clear(managed);
    }

    private void finish(RestAction<?> action, String message, Outcome outcome) {
        action.queue(ok -> outcome.done(message),
                error -> outcome.fail("Discord rejected the rank permission change: " + error.getMessage()));
    }

    private void forget(Target target, Outcome outcome, String message) {
        plugin.scheduler().runAsync(() -> {
            try {
                grants.forget(target.guildId, target.slot);
                outcome.done(message);
            } catch (SQLException error) {
                outcome.defer("could not update rank grant records: " + error.getMessage());
            }
        });
    }

    private static PermissionOverride memberOverride(GuildChannel channel, String memberId) {
        for (PermissionOverride override : channel.getMemberPermissionOverrides()) {
            if (override.getId().equals(memberId)) {
                return override;
            }
        }
        return null;
    }

    private static String message(String action, Target target, GuildChannel channel, Guild guild) {
        Member member = guild.getMemberById(target.slot.memberId());
        String memberName = member == null ? "Discord member " + target.slot.memberId()
                : "\"" + member.getEffectiveName() + "\" [Discord ID " + target.slot.memberId() + "]";
        String channelName = channel == null ? target.slot.channelId() : channel.getName();
        return QueueMessages.rank(action, target.slot.channelKind(), channelName, memberName,
                target.job.id, target.job.attempts);
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }
}
