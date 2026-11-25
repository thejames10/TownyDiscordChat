package com.TownyDiscordChat.TownyDiscordChat.Ranks;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankPlanTest {
    private static final RankSlot MAYOR = new RankSlot("TOWN_TEXT", "100", "1");
    private static final RankSlot ASSISTANT = new RankSlot("TOWN_TEXT", "100", "2");
    private static final RankSlot FORMER_MAYOR = new RankSlot("TOWN_TEXT", "100", "3");
    private static final RankSlot ADMIN_GRANTED = new RankSlot("TOWN_TEXT", "100", "4");

    @Test
    void grantsOnlyWhatDiscordDoesNotAlreadySettle() {
        Set<RankSlot> changes = RankPlan.slotsToSync(slots(MAYOR, ASSISTANT), slots(MAYOR),
                Collections.<RankSlot>emptySet());

        assertEquals(slots(ASSISTANT), changes);
    }

    @Test
    void revokesRecordedGrantsThatAreNoLongerDesired() {
        Set<RankSlot> changes = RankPlan.slotsToSync(slots(MAYOR), slots(MAYOR, FORMER_MAYOR),
                slots(MAYOR, FORMER_MAYOR));

        assertEquals(slots(FORMER_MAYOR), changes);
    }

    @Test
    void neverTouchesPermissionsItDidNotGrant() {
        Set<RankSlot> changes = RankPlan.slotsToSync(Collections.<RankSlot>emptySet(),
                slots(ADMIN_GRANTED), Collections.<RankSlot>emptySet());

        assertEquals(Collections.<RankSlot>emptySet(), changes);
    }

    @Test
    void disablingRanksRevokesEveryRecordedGrant() {
        Set<RankSlot> changes = RankPlan.slotsToSync(Collections.<RankSlot>emptySet(),
                Collections.<RankSlot>emptySet(), slots(MAYOR, ASSISTANT));

        assertEquals(slots(MAYOR, ASSISTANT), changes);
    }

    private static Set<RankSlot> slots(RankSlot... slots) {
        return new HashSet<RankSlot>(Arrays.asList(slots));
    }
}
