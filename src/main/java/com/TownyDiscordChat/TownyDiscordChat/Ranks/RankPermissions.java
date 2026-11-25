package com.TownyDiscordChat.TownyDiscordChat.Ranks;

import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.PermissionOverride;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** The channel permissions a town or nation leader receives. */
public final class RankPermissions {
    private static final Set<Permission> TEXT = Collections.unmodifiableSet(
            EnumSet.of(Permission.MESSAGE_MANAGE));
    private static final Set<Permission> VOICE = Collections.unmodifiableSet(EnumSet.of(
            Permission.VOICE_MUTE_OTHERS, Permission.VOICE_DEAF_OTHERS, Permission.VOICE_MOVE_OTHERS));
    private static final Set<Permission> ALL;

    static {
        EnumSet<Permission> all = EnumSet.noneOf(Permission.class);
        all.addAll(TEXT);
        all.addAll(VOICE);
        ALL = Collections.unmodifiableSet(all);
    }

    private RankPermissions() {
    }

    public static Set<Permission> forSlot(RankSlot slot) {
        return slot.isText() ? TEXT : VOICE;
    }

    /** What the bot itself needs, since Discord only lets it grant permissions it has. */
    public static Set<Permission> all() {
        return ALL;
    }

    public static boolean allows(PermissionOverride override, Set<Permission> managed) {
        return override != null && override.getAllowed().containsAll(managed);
    }

    /** An explicit deny is an admin decision that rank sync must not override. */
    public static boolean deniesAny(PermissionOverride override, Set<Permission> managed) {
        if (override == null) {
            return false;
        }
        for (Permission permission : managed) {
            if (override.getDenied().contains(permission)) {
                return true;
            }
        }
        return false;
    }

    public static boolean settled(PermissionOverride override, Set<Permission> managed) {
        return allows(override, managed) || deniesAny(override, managed);
    }
}
