package com.TownyDiscordChat.TownyDiscordChat.ChatBridge;

import com.TownyDiscordChat.TownyDiscordChat.ChatBridge.ChannelDirectory.GroupType;
import org.bukkit.configuration.ConfigurationSection;

/** The chatBridge section of config.yml. */
public final class ChatBridgeSettings {
    public static final String ENABLED_KEY = "chatBridge.enabled";
    public static final String TOWN_KEY = "chatBridge.town";
    public static final String NATION_KEY = "chatBridge.nation";
    public static final String WEBHOOKS_KEY = "chatBridge.useWebhooks";
    public static final String FORMAT_KEY = "chatBridge.toDiscordFormat";
    public static final String MAX_LENGTH_KEY = "chatBridge.maxMessageLength";
    public static final String DEFAULT_FORMAT = "**{player}**: {message}";
    public static final int MIN_LENGTH = 16;
    public static final int MAX_LENGTH = 900;
    private static final int DEFAULT_LENGTH = 256;

    private final boolean enabled;
    private final boolean town;
    private final boolean nation;
    private final boolean useWebhooks;
    private final String toDiscordFormat;
    private final int maxMessageLength;

    private ChatBridgeSettings(ConfigurationSection config) {
        this.enabled = config.getBoolean(ENABLED_KEY, false);
        this.town = config.getBoolean(TOWN_KEY, true);
        this.nation = config.getBoolean(NATION_KEY, true);
        this.useWebhooks = config.getBoolean(WEBHOOKS_KEY, true);
        this.toDiscordFormat = config.getString(FORMAT_KEY, DEFAULT_FORMAT);
        this.maxMessageLength = Math.max(MIN_LENGTH, Math.min(MAX_LENGTH,
                config.getInt(MAX_LENGTH_KEY, DEFAULT_LENGTH)));
    }

    public static ChatBridgeSettings fromConfig(ConfigurationSection config) {
        return new ChatBridgeSettings(config);
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean bridges(GroupType type) {
        return enabled && (type == GroupType.TOWN ? town : nation);
    }

    /** The bridged group for a TownyChat channel type name, or null when it is not bridged. */
    public GroupType bridgedType(String townyChatChannelType) {
        for (GroupType type : GroupType.values()) {
            if (type.name().equals(townyChatChannelType) && bridges(type)) {
                return type;
            }
        }
        return null;
    }

    public boolean useWebhooks() {
        return useWebhooks;
    }

    public String toDiscordFormat() {
        return toDiscordFormat;
    }

    public int maxMessageLength() {
        return maxMessageLength;
    }
}
