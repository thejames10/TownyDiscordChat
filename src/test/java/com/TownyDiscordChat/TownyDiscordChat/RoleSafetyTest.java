package com.TownyDiscordChat.TownyDiscordChat;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class RoleSafetyTest {
    private static long bit(int index) { return 1L << index; }

    private static Role role(long raw, boolean managed, boolean publicRole) {
        return (Role) Proxy.newProxyInstance(Role.class.getClassLoader(),
                new Class<?>[]{Role.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getPermissionsRaw": return raw;
                        case "isManaged": return managed;
                        case "isPublicRole": return publicRole;
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    @Test void acceptsLegacyRoleWithOrdinaryParticipantPermissions() {
        // Mirrors the non-administrative permissions in the user's existing
        // Discord town role, including flags newer than some bundled JDA APIs.
        long ordinary = bit(43) | bit(38) | bit(35) | bit(36)
                | bit(37) | bit(46) | bit(42) | bit(45)
                | bit(39) | bit(44);
        assertTrue(RoleSafety.safePermissionBits(ordinary));
        assertTrue(RoleSafety.safeForMembership(role(ordinary,false,false)));
        assertNull(RoleSafety.problem(role(ordinary,false,false)));
        assertTrue(RoleSafety.safePermissionBits(0L));
    }

    @Test void refusesPrivilegeEscalationAndUnknownFuturePermissions() {
        // Moderation, administration, guild/channel management, audit access,
        // managed content, and any unknown future bits require manual review.
        int[] privileged = {0,1,2,3,4,5,7,13,17,19,22,23,24,27,28,29,30,33,34,40,41,48,51,52,60};
        for (int bit : privileged) {
            long raw=bit(bit) | bit(43);
            assertFalse(RoleSafety.safePermissionBits(raw), "Rejected bit " + bit);
            assertFalse(RoleSafety.safeForMembership(role(raw,false,false)));
            assertTrue(RoleSafety.problem(role(raw,false,false))
                    .contains("administrator review"));
        }
    }

    @Test void rejectsInheritedGuildDefaultsWithUnrecognizedHighBits() {
        long inheritedPermissions = bit(47) | bit(48);
        assertFalse(RoleSafety.safePermissionBits(inheritedPermissions));
        assertFalse(RoleSafety.safeForMembership(role(inheritedPermissions, false, false)));
        assertTrue(RoleSafety.problem(role(inheritedPermissions, false, false))
                .contains("0x1800000000000"));

        // New roles must use a zero bitmask rather than weakening this policy.
        assertTrue(RoleSafety.safeForMembership(role(0L, false, false)));
    }

    @Test void neverManagesEveryoneOrIntegrationRoles() {
        assertFalse(RoleSafety.safeForMembership(role(0L,true,false)));
        assertFalse(RoleSafety.safeForMembership(role(0L,false,true)));
        assertNotNull(RoleSafety.problem(role(0L,true,false)));
        assertNotNull(RoleSafety.problem(role(0L,false,true)));
        assertFalse(RoleSafety.safeForMembership(null));
    }
}
