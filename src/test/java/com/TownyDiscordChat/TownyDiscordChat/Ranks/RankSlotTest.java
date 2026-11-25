package com.TownyDiscordChat.TownyDiscordChat.Ranks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankSlotTest {
    private static final String CHANNEL_ID = "123456789012345678";
    private static final String MEMBER_ID = "876543210987654321";

    @Test
    void jobRoundTripKeepsEveryField() {
        RankSlot slot = new RankSlot("NATION_VOICE", CHANNEL_ID, MEMBER_ID);

        RankSlot parsed = RankSlot.fromJob(slot.jobKind(), slot.jobName());

        assertEquals(slot, parsed);
        assertEquals("RANK_NATION_VOICE", slot.jobKind());
        assertFalse(parsed.isTown());
        assertFalse(parsed.isText());
    }

    @Test
    void jobNamesFitTheQueueColumns() {
        RankSlot slot = new RankSlot("TOWN_TEXT", "12345678901234567890", "12345678901234567890");

        assertTrue(slot.jobKind().length() <= 40);
        assertTrue(slot.jobName().length() <= 128);
    }

    @Test
    void foreignJobsAreRejected() {
        assertNull(RankSlot.fromJob("TOWN_TEXT", CHANNEL_ID + ":" + MEMBER_ID));
        assertNull(RankSlot.fromJob("RANK_TOWN_ROLE", CHANNEL_ID + ":" + MEMBER_ID));
        assertNull(RankSlot.fromJob("RANK_TOWN_TEXT", "Bradfordshire"));
        assertNull(RankSlot.fromJob("RANK_TOWN_TEXT", CHANNEL_ID + ":" + MEMBER_ID + ":1"));
        assertNull(RankSlot.fromJob(null, null));
    }
}
