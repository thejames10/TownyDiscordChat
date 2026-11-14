package com.TownyDiscordChat.TownyDiscordChat.Schedulers;

import com.TownyDiscordChat.TownyDiscordChat.Queue.MemberIntentStore.Grant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure membership decisions used by the real reconciler and offline Discord simulations.
 * No JDA session, network request, Bukkit scheduler or live guild is required.
 */
public final class MembershipPlan {
    public enum Change {
        GRANT,
        REVOKE,
        SUPERSEDE_GRANT
    }

    public static final class Update {
        public final String memberId;
        public final String roleId;
        public final Change change;

        Update(String memberId, String roleId, Change change) {
            this.memberId = memberId;
            this.roleId = roleId;
            this.change = change;
        }
    }

    private MembershipPlan() {
    }

    public static Set<String> desiredPairs(Map<String, String> townRoles,
                                            Map<String, String> nationRoles,
                                            Map<String, String> safeNames,
                                            Set<String> blockedRoleIds) {
        Set<String> desired = new HashSet<String>();
        addExpected(desired, townRoles, safeNames, blockedRoleIds, "town-");
        addExpected(desired, nationRoles, safeNames, blockedRoleIds, "nation-");
        return desired;
    }

    private static void addExpected(Set<String> desired, Map<String, String> roles,
                                    Map<String, String> safeNames, Set<String> blocked,
                                    String prefix) {
        for (Map.Entry<String, String> member : roles.entrySet()) {
            String roleId = member.getValue();
            String roleName = safeNames.get(roleId);
            if (roleName == null || !roleName.startsWith(prefix)
                    || blocked.contains(roleId)) {
                continue;
            }
            desired.add(member.getKey() + "|" + roleId);
        }
    }

    public static List<Update> updates(Set<String> desired, Set<String> cached,
                                       List<Grant> tracked) {
        List<Update> changes = new ArrayList<Update>();
        for (Grant grant : tracked) {
            String key = grant.memberId + "|" + grant.roleId;
            if (!desired.contains(key)) {
                changes.add(new Update(grant.memberId, grant.roleId, Change.REVOKE));
            }
        }
        for (String key : desired) {
            int separator = key.indexOf('|');
            if (separator <= 0 || separator == key.length() - 1) {
                throw new IllegalArgumentException("Invalid member-role pair");
            }
            Change change = cached.contains(key) ? Change.SUPERSEDE_GRANT : Change.GRANT;
            changes.add(new Update(key.substring(0, separator),
                    key.substring(separator + 1), change));
        }
        return Collections.unmodifiableList(changes);
    }
}
