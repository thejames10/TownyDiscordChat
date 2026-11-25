package com.TownyDiscordChat.TownyDiscordChat.ChatBridge;

import com.TownyDiscordChat.TownyDiscordChat.ChatBridge.ChannelDirectory.Group;
import com.TownyDiscordChat.TownyDiscordChat.ChatBridge.ChannelDirectory.GroupType;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChannelDirectoryTest {
    private static final UUID TOWN = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID NATION = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void looksUpChannelsInBothDirections() {
        Map<String, String> channels = new HashMap<String, String>();
        channels.put("TOWN_TEXT:Hyrule", "100");
        channels.put("TOWN_VOICE:Hyrule", "101");
        channels.put("NATION_TEXT:Haven", "200");

        ChannelDirectory directory = ChannelDirectory.of(
                Collections.singletonMap(TOWN.toString(), "Hyrule"),
                Collections.singletonMap(NATION.toString(), "Haven"), channels);

        assertEquals("100", directory.textChannelFor(GroupType.TOWN, TOWN));
        assertEquals("200", directory.textChannelFor(GroupType.NATION, NATION));
        Group town = directory.groupFor("100");
        assertEquals(GroupType.TOWN, town.type());
        assertEquals(TOWN, town.uuid());
        assertNull(directory.groupFor("101"), "voice channels are not bridged");
    }

    @Test
    void unverifiedChannelsAreNotBridged() {
        ChannelDirectory directory = ChannelDirectory.of(
                Collections.singletonMap(TOWN.toString(), "Hyrule"),
                Collections.<String, String>emptyMap(), Collections.<String, String>emptyMap());

        assertNull(directory.textChannelFor(GroupType.TOWN, TOWN));
        assertNull(ChannelDirectory.empty().groupFor("100"));
    }
}
