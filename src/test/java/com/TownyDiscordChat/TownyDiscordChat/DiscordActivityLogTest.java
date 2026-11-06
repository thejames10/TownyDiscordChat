package com.TownyDiscordChat.TownyDiscordChat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiscordActivityLogTest {
    @Test void avoidsMentionsAndMultilineSpoofing() {
        String formatted=DiscordActivityLog.format("CHANGE",
                "Created town @everyone\n[ADMIN] fake message @here");
        assertTrue(formatted.startsWith("[TDC CHANGE] Created town "));
        assertFalse(formatted.contains("@everyone"));
        assertFalse(formatted.contains("@here"));
        assertFalse(formatted.contains("\n"));
        assertTrue(formatted.contains("@\u200Beveryone"));
    }

    @Test void neverExceedsDiscordMessageLimit() {
        StringBuilder huge=new StringBuilder();
        for (int i=0;i<3000;i++) huge.append('A');
        String formatted=DiscordActivityLog.format("ERROR",huge.toString());
        assertTrue(formatted.length()<2000);
        assertTrue(formatted.endsWith("..."));
    }
}
