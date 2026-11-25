package com.TownyDiscordChat.TownyDiscordChat.ChatBridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatTextTest {
    private static final int LIMIT = 256;

    @Test
    void minecraftColourCodesAreRemoved() {
        assertEquals("hello world", ChatText.forDiscord("&ahello §lworld", LIMIT));
        assertEquals("fish & chips", ChatText.forDiscord("fish & chips", LIMIT));
    }

    @Test
    void markdownIsEscapedForDiscord() {
        assertEquals("\\*\\*bold\\*\\* \\_x\\_ \\`code\\` \\> quote",
                ChatText.forDiscord("**bold** _x_ `code` > quote", LIMIT));
    }

    @Test
    void mentionsCannotPing() {
        String text = ChatText.forDiscord("@everyone look <@123>", LIMIT);

        assertFalse(text.contains("@everyone"));
        assertFalse(text.contains("<@123>"));
        assertTrue(text.startsWith("@​everyone"));
    }

    @Test
    void discordTextBecomesOneLineWithoutSectionSigns() {
        assertEquals("first second ? third",
                ChatText.forMinecraft("first\nsecond §?\r\n  third", LIMIT));
    }

    @Test
    void longTextIsCutWithAnEllipsis() {
        String cut = ChatText.forMinecraft("abcdefghijklmnopqrstuvwxyz", 16);

        assertEquals(16, cut.length());
        assertTrue(cut.endsWith("…"));
    }

    @Test
    void truncationNeverSplitsAnEmoji() {
        String cut = ChatText.truncate("aaaa😀bbbb", 6);

        assertEquals("aaaa…", cut);
    }

    @Test
    void formatFillsPlaceholdersOnce() {
        assertEquals("**Steve**: say {player}",
                ChatText.format("**{player}**: {message}", "Steve", "say {player}"));
    }
}
