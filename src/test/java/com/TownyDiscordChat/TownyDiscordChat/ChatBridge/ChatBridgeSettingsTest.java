package com.TownyDiscordChat.TownyDiscordChat.ChatBridge;

import com.TownyDiscordChat.TownyDiscordChat.ChatBridge.ChannelDirectory.GroupType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatBridgeSettingsTest {
    @Test
    void disabledBridgeRelaysNothing() {
        ChatBridgeSettings settings = ChatBridgeSettings.fromConfig(new YamlConfiguration());

        assertFalse(settings.enabled());
        assertNull(settings.bridgedType("TOWN"));
        assertEquals(ChatBridgeSettings.DEFAULT_FORMAT, settings.toDiscordFormat());
    }

    @Test
    void onlyTownAndNationChannelsCanBeBridged() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(ChatBridgeSettings.ENABLED_KEY, true);
        config.set(ChatBridgeSettings.NATION_KEY, false);
        ChatBridgeSettings settings = ChatBridgeSettings.fromConfig(config);

        assertEquals(GroupType.TOWN, settings.bridgedType("TOWN"));
        assertNull(settings.bridgedType("NATION"));
        assertNull(settings.bridgedType("GLOBAL"));
        assertNull(settings.bridgedType("ALLIANCE"));
    }

    @Test
    void messageLengthStaysWithinDiscordLimits() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(ChatBridgeSettings.MAX_LENGTH_KEY, 50000);

        assertEquals(ChatBridgeSettings.MAX_LENGTH, ChatBridgeSettings.fromConfig(config).maxMessageLength());
    }
}
