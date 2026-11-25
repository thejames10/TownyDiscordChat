package com.TownyDiscordChat.TownyDiscordChat.ChatBridge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Verified town and nation text channels from the latest reconcile snapshot,
 * looked up in both directions without touching SQL on a chat thread.
 */
public final class ChannelDirectory {
    /** Which kind of Towny group a channel belongs to. */
    public enum GroupType {
        TOWN, NATION
    }

    /** The Towny town or nation that owns a bridged channel. */
    public static final class Group {
        private final GroupType type;
        private final UUID uuid;

        Group(GroupType type, UUID uuid) {
            this.type = type;
            this.uuid = uuid;
        }

        public GroupType type() {
            return type;
        }

        public UUID uuid() {
            return uuid;
        }
    }

    private static final ChannelDirectory EMPTY = new ChannelDirectory(
            Collections.<String, String>emptyMap(), Collections.<String, Group>emptyMap());

    private final Map<String, String> channelByGroup;
    private final Map<String, Group> groupByChannel;

    private ChannelDirectory(Map<String, String> channelByGroup, Map<String, Group> groupByChannel) {
        this.channelByGroup = channelByGroup;
        this.groupByChannel = groupByChannel;
    }

    public static ChannelDirectory empty() {
        return EMPTY;
    }

    /**
     * @param townNames Towny UUID to current town name
     * @param nationNames Towny UUID to current nation name
     * @param channelIds verified channel IDs keyed by "TOWN_TEXT:TownName" and similar
     */
    public static ChannelDirectory of(Map<String, String> townNames, Map<String, String> nationNames,
                                      Map<String, String> channelIds) {
        Map<String, String> channelByGroup = new HashMap<String, String>();
        Map<String, Group> groupByChannel = new HashMap<String, Group>();
        add(GroupType.TOWN, townNames, channelIds, channelByGroup, groupByChannel);
        add(GroupType.NATION, nationNames, channelIds, channelByGroup, groupByChannel);
        return new ChannelDirectory(channelByGroup, groupByChannel);
    }

    private static void add(GroupType type, Map<String, String> names, Map<String, String> channelIds,
                            Map<String, String> channelByGroup, Map<String, Group> groupByChannel) {
        for (Map.Entry<String, String> entry : names.entrySet()) {
            String channelId = channelIds.get(type.name() + "_TEXT:" + entry.getValue());
            if (channelId == null) {
                continue;
            }
            UUID uuid = UUID.fromString(entry.getKey());
            channelByGroup.put(key(type, uuid), channelId);
            groupByChannel.put(channelId, new Group(type, uuid));
        }
    }

    public String textChannelFor(GroupType type, UUID uuid) {
        return channelByGroup.get(key(type, uuid));
    }

    /** Null when the channel is not a bridged town or nation channel. */
    public Group groupFor(String channelId) {
        return groupByChannel.get(channelId);
    }

    private static String key(GroupType type, UUID uuid) {
        return type.name() + ":" + uuid;
    }
}
