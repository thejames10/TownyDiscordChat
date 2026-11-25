package com.TownyDiscordChat.TownyDiscordChat.Ranks;

import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.PermissionOverride;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankPermissionsTest {
    private static final RankSlot TEXT = new RankSlot("TOWN_TEXT", "1", "2");
    private static final RankSlot VOICE = new RankSlot("NATION_VOICE", "1", "2");

    @Test
    void textAndVoiceGetDifferentPermissions() {
        assertEquals(EnumSet.of(Permission.MESSAGE_MANAGE), RankPermissions.forSlot(TEXT));
        assertTrue(RankPermissions.forSlot(VOICE).contains(Permission.VOICE_MOVE_OTHERS));
        assertFalse(RankPermissions.forSlot(VOICE).contains(Permission.MESSAGE_MANAGE));
        assertTrue(RankPermissions.all().containsAll(RankPermissions.forSlot(VOICE)));
    }

    @Test
    void partialVoicePermissionsStillNeedAGrant() {
        Set<Permission> managed = RankPermissions.forSlot(VOICE);
        PermissionOverride partial = override(EnumSet.of(Permission.VOICE_MOVE_OTHERS),
                EnumSet.noneOf(Permission.class));

        assertFalse(RankPermissions.settled(partial, managed));
        assertFalse(RankPermissions.settled(null, managed));
    }

    @Test
    void fullAllowOrAnyDenyIsSettled() {
        Set<Permission> managed = RankPermissions.forSlot(VOICE);
        PermissionOverride full = override(EnumSet.copyOf(managed), EnumSet.noneOf(Permission.class));
        PermissionOverride denied = override(EnumSet.noneOf(Permission.class),
                EnumSet.of(Permission.VOICE_DEAF_OTHERS));

        assertTrue(RankPermissions.settled(full, managed));
        assertTrue(RankPermissions.deniesAny(denied, managed));
        assertTrue(RankPermissions.settled(denied, managed));
    }

    private static PermissionOverride override(EnumSet<Permission> allowed, EnumSet<Permission> denied) {
        return (PermissionOverride) Proxy.newProxyInstance(PermissionOverride.class.getClassLoader(),
                new Class<?>[] {PermissionOverride.class}, (self, method, arguments) -> {
                    if ("getAllowed".equals(method.getName())) {
                        return allowed;
                    }
                    return "getDenied".equals(method.getName()) ? denied : null;
                });
    }
}
