package com.TownyDiscordChat.TownyDiscordChat;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;

/**
 * Policy for roles which may be granted/revoked by automatic Towny membership.
 * A legacy town role can have ordinary guild permissions; rejecting every
 * nonempty permission set prevented upgrades from adopting existing roles.
 *
 * Use Discord's stable permission *bit positions*, not only the bundled JDA
 * enum: older DiscordSRV/JDA versions may not recognize newer benign flags.
 * Unknown future bits are NOT automatically trusted. Never modify the role's
 * permissions when adopting it; manual admin review is required otherwise.
 */
public final class RoleSafety {
    private RoleSafety() { }

    private static long bit(int offset) { return 1L << offset; }

    // Ordinary participant permissions, including newer Discord flags.
    // These do not include administration/moderation, managing other users'
    // content, private audit/insights, invites, or bypassing rate limits.
    private static final long PARTICIPANT_PERMISSIONS =
            bit(6)  | // Add Reactions
            bit(9)  | // Stream
            bit(10) | // View Channel
            bit(11) | // Send Messages
            bit(12) | // Send TTS Messages
            bit(14) | // Embed Links
            bit(15) | // Attach Files
            bit(16) | // Read Message History
            bit(18) | // Use External Emojis
            bit(20) | // Connect to voice
            bit(21) | // Speak
            bit(25) | // Use Voice Activity
            bit(26) | // Change own Nickname
            bit(31) | // Use Application Commands
            bit(32) | // Request to Speak
            bit(35) | // Create Public Threads
            bit(36) | // Create Private Threads
            bit(37) | // Use External Stickers
            bit(38) | // Send Messages in Threads
            bit(39) | // Use Activities
            bit(42) | // Use Soundboard
            bit(43) | // Create Expressions
            bit(44) | // Create Events
            bit(45) | // Use External Sounds
            bit(46) | // Send Voice Messages
            bit(49) | // Send Polls
            bit(50);  // Use External Apps

    public static long disallowedPermissionBits(long permissions) {
        return permissions & ~PARTICIPANT_PERMISSIONS;
    }

    public static boolean safePermissionBits(long permissions) {
        return disallowedPermissionBits(permissions) == 0L;
    }

    public static boolean safeForMembership(Role role) {
        return role != null && !role.isPublicRole() && !role.isManaged()
                && safePermissionBits(role.getPermissionsRaw());
    }

    public static String problem(Role role) {
        if (role == null) return "Role not found";
        if (role.isPublicRole()) return "@everyone role cannot be managed";
        if (role.isManaged()) return "integration-managed role cannot be managed";
        long blocked=disallowedPermissionBits(role.getPermissionsRaw());
        if (blocked != 0L)
            return "privileged or unrecognized Discord permission bits 0x"
                    + Long.toHexString(blocked) + " require administrator review";
        return null;
    }
}
