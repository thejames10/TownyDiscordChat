package com.TownyDiscordChat.TownyDiscordChat.Ranks;

import java.util.HashSet;
import java.util.Set;

/** Decides which rank slots need a queue job to converge. */
public final class RankPlan {
    private RankPlan() {
    }

    /**
     * Desired slots that Discord does not already settle, plus slots this
     * plugin granted that are no longer desired. Revocation never reaches
     * permissions an admin set by hand, because those were never recorded.
     */
    public static Set<RankSlot> slotsToSync(Set<RankSlot> desired, Set<RankSlot> settled,
                                            Set<RankSlot> recorded) {
        Set<RankSlot> changes = new HashSet<RankSlot>(desired);
        changes.removeAll(settled);
        for (RankSlot slot : recorded) {
            if (!desired.contains(slot)) {
                changes.add(slot);
            }
        }
        return changes;
    }
}
