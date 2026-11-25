package com.TownyDiscordChat.TownyDiscordChat.Ranks;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.GuildChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.PermissionOverride;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Rank slots Towny wants and the ones Discord already settles. Capture on the global thread. */
public final class RankSnapshot {
    private static final String[] CHANNEL_COMPONENTS = {"TEXT", "VOICE"};

    private final Set<RankSlot> desired = new HashSet<RankSlot>();
    private final Set<RankSlot> settled = new HashSet<RankSlot>();

    private RankSnapshot() {
    }

    /**
     * @param channelIds verified channel IDs keyed by "TOWN_TEXT:TownName" and similar
     * @param discordByPlayer linked Discord account for each Minecraft UUID
     * @param guildMembers Discord member IDs currently in the guild
     */
    public static RankSnapshot capture(Guild guild, RankSettings settings,
                                       Map<String, String> channelIds,
                                       Map<UUID, String> discordByPlayer,
                                       Set<String> guildMembers) {
        RankSnapshot snapshot = new RankSnapshot();
        if (!settings.enabled()) {
            return snapshot;
        }
        for (Town town : TownyUniverse.getInstance().getTowns()) {
            Set<String> members = memberIds(RankHolders.townLeaders(town, settings),
                    discordByPlayer, guildMembers);
            snapshot.addGroup(guild, "TOWN", town.getName(), members, channelIds);
        }
        for (Nation nation : TownyUniverse.getInstance().getNations()) {
            Set<String> members = memberIds(RankHolders.nationLeaders(nation, settings),
                    discordByPlayer, guildMembers);
            snapshot.addGroup(guild, "NATION", nation.getName(), members, channelIds);
        }
        return snapshot;
    }

    public Set<RankSlot> desired() {
        return Collections.unmodifiableSet(desired);
    }

    public Set<RankSlot> settled() {
        return Collections.unmodifiableSet(settled);
    }

    private static Set<String> memberIds(Set<UUID> players, Map<UUID, String> discordByPlayer,
                                         Set<String> guildMembers) {
        Set<String> members = new HashSet<String>();
        for (UUID player : players) {
            String memberId = discordByPlayer.get(player);
            if (memberId != null && guildMembers.contains(memberId)) {
                members.add(memberId);
            }
        }
        return members;
    }

    private void addGroup(Guild guild, String type, String name, Set<String> members,
                          Map<String, String> channelIds) {
        for (String component : CHANNEL_COMPONENTS) {
            String kind = type + "_" + component;
            String channelId = channelIds.get(kind + ":" + name);
            if (channelId == null) {
                continue;
            }
            for (String memberId : members) {
                desired.add(new RankSlot(kind, channelId, memberId));
            }
            addSettled(guild.getGuildChannelById(channelId), kind, channelId);
        }
    }

    private void addSettled(GuildChannel channel, String kind, String channelId) {
        if (channel == null) {
            return;
        }
        for (PermissionOverride override : channel.getMemberPermissionOverrides()) {
            RankSlot slot = new RankSlot(kind, channelId, override.getId());
            if (RankPermissions.settled(override, RankPermissions.forSlot(slot))) {
                settled.add(slot);
            }
        }
    }
}
