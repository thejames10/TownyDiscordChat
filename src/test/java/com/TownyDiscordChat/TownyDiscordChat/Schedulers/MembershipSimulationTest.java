package com.TownyDiscordChat.TownyDiscordChat.Schedulers;

import com.TownyDiscordChat.TownyDiscordChat.Queue.MemberIntentStore.Grant;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * In-memory Discord guild/member simulation using production membership decisions.
 * No bot token, linked user account, Discord API, or Minecraft server is required.
 */
class MembershipSimulationTest {
    private static final String MEMBER_ID = "111111111111111111";
    private static final String TOWN_A = "222222222222222222";
    private static final String NATION_A = "333333333333333333";
    private static final String TOWN_B = "444444444444444444";
    private static final String NATION_B = "555555555555555555";
    private static final String ORIGINAL_ROLE = "666666666666666666";

    @Test
    void linkingJoiningLeavingRejoiningAndUnlinkingMaintainPrivateAccess() {
        SimulatedDiscord guild = new SimulatedDiscord();
        guild.sync(null, null);
        guild.assertAccess(ORIGINAL_ROLE);
        guild.sync(TOWN_A, NATION_A);
        guild.assertAccess(ORIGINAL_ROLE, TOWN_A, NATION_A);
        guild.sync(null, null);
        guild.assertAccess(ORIGINAL_ROLE);
        guild.sync(TOWN_A, NATION_A);
        guild.assertAccess(ORIGINAL_ROLE, TOWN_A, NATION_A);
        guild.sync(null, null);
        guild.assertAccess(ORIGINAL_ROLE);
    }

    @Test
    void movingBetweenTownsAndNationsRevokesStaleAccess() {
        SimulatedDiscord guild = new SimulatedDiscord();
        guild.sync(TOWN_A, NATION_A);
        guild.sync(TOWN_B, NATION_B);
        guild.assertAccess(ORIGINAL_ROLE, TOWN_B, NATION_B);
        guild.sync(TOWN_A, NATION_B);
        guild.assertAccess(ORIGINAL_ROLE, TOWN_A, NATION_B);
    }

    @Test
    void unchangedMembershipDoesNotDuplicateRoles() {
        SimulatedDiscord guild = new SimulatedDiscord();
        guild.sync(TOWN_A, NATION_A);
        guild.sync(TOWN_A, NATION_A);
        guild.assertAccess(ORIGINAL_ROLE, TOWN_A, NATION_A);
        assertEquals(3, guild.roles.size());
    }

    @Test
    void blockedOrUnknownRolesAreNeverGranted() {
        Map<String, String> town = Collections.singletonMap(MEMBER_ID, TOWN_A);
        Map<String, String> nation = Collections.singletonMap(MEMBER_ID, NATION_A);
        Map<String, String> safe = new HashMap<String, String>();
        safe.put(TOWN_A, "town-Alpha");
        safe.put(NATION_A, "nation-Alpha");
        assertEquals(1, MembershipPlan.desiredPairs(
                town, nation, safe, Collections.singleton(NATION_A)).size());
        safe.remove(TOWN_A);
        assertTrue(MembershipPlan.desiredPairs(town, nation, safe,
                Collections.singleton(NATION_A)).isEmpty());
    }

    @Test
    void cachedRoleCancelsAnOldRevocationWithoutNewRestMutation() {
        List<MembershipPlan.Update> updates = MembershipPlan.updates(
                Collections.singleton(MEMBER_ID + "|" + TOWN_A),
                Collections.singleton(MEMBER_ID + "|" + TOWN_A),
                Collections.singletonList(new Grant(MEMBER_ID, TOWN_A)));
        assertEquals(1, updates.size());
        assertEquals(MembershipPlan.Change.SUPERSEDE_GRANT, updates.get(0).change);
    }

    @Test
    void trackedDiscordRoleMustBeRevokedWhenLinkDisappears() {
        List<MembershipPlan.Update> updates = MembershipPlan.updates(
                Collections.<String>emptySet(),
                Collections.<String>emptySet(),
                Collections.singletonList(new Grant(MEMBER_ID, TOWN_A)));
        assertEquals(1, updates.size());
        assertEquals(MembershipPlan.Change.REVOKE, updates.get(0).change);
    }

    private static final class SimulatedDiscord {
        private final Set<String> roles = new HashSet<String>();
        private final Map<String, String> safeNames = new HashMap<String, String>();
        private final Set<String> managedRoles = new HashSet<String>();

        SimulatedDiscord() {
            roles.add(ORIGINAL_ROLE);
            safeNames.put(TOWN_A, "town-Alpha");
            safeNames.put(TOWN_B, "town-Beta");
            safeNames.put(NATION_A, "nation-Alpha");
            safeNames.put(NATION_B, "nation-Beta");
        }

        void sync(String townRole, String nationRole) {
            Map<String, String> towns = roleMapping(townRole);
            Map<String, String> nations = roleMapping(nationRole);
            Set<String> desired = MembershipPlan.desiredPairs(towns, nations,
                    safeNames, Collections.<String>emptySet());
            List<Grant> tracked = new ArrayList<Grant>();
            for (String roleId : managedRoles) {
                tracked.add(new Grant(MEMBER_ID, roleId));
            }
            Set<String> cached = new HashSet<String>();
            for (String roleId : roles) {
                cached.add(MEMBER_ID + "|" + roleId);
            }
            for (MembershipPlan.Update update : MembershipPlan.updates(
                    desired, cached, tracked)) {
                apply(update);
            }
        }

        private void apply(MembershipPlan.Update update) {
            if (update.change == MembershipPlan.Change.REVOKE) {
                roles.remove(update.roleId);
                managedRoles.remove(update.roleId);
            } else if (update.change == MembershipPlan.Change.GRANT) {
                roles.add(update.roleId);
                managedRoles.add(update.roleId);
            }
        }

        private Map<String, String> roleMapping(String roleId) {
            if (roleId == null) {
                return Collections.emptyMap();
            }
            return Collections.singletonMap(MEMBER_ID, roleId);
        }

        void assertAccess(String... expected) {
            assertEquals(new HashSet<String>(Arrays.asList(expected)), roles);
            assertTrue(roles.contains(ORIGINAL_ROLE));
            assertFalse(managedRoles.contains(ORIGINAL_ROLE));
        }
    }
}
