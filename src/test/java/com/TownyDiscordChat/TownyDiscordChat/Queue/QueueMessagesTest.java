package com.TownyDiscordChat.TownyDiscordChat.Queue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueueMessagesTest {
    @Test void explainsDiscordCreateVsAdoptionWithoutClaimingBothAreCreations() {
        String created=QueueMessages.resource("CREATED","TOWN_VOICE",
                "Bradfordshire","123456",31,1);
        String adopted=QueueMessages.resource("ADOPTED","TOWN_ROLE",
                "Bradfordshire","789012",30,4);
        assertTrue(created.contains("CREATED town voice channel \"Bradfordshire\""));
        assertTrue(created.contains("Discord ID 123456"));
        assertTrue(created.contains("job #31, attempt 1"));
        assertTrue(adopted.contains("ADOPTED town role \"Bradfordshire\""));
        assertFalse(adopted.contains("CREATED"));
    }

    @Test void distinguishesRenameSkipAndDeletion() {
        assertTrue(QueueMessages.resource("RENAMED","RENAME_NATION_TEXT",
                "Haven", "123", 7,2).contains("RENAMED nation text channel"));
        assertTrue(QueueMessages.resource("SKIPPED","RENAME_TOWN_TEXT",
                "Hyrule",null,8,1).contains("SKIPPED town text channel"));
        assertTrue(QueueMessages.resource("DELETED","DELETE_TOWN_ROLE",
                "OldTown","456",9,1).contains("DELETED town role"));
    }

    @Test void membershipChangesNameDiscordRoleAndAction() {
        assertTrue(QueueMessages.member(true,"member-id",
                "town-Bradfordshire",11,1).contains("GRANTED town-Bradfordshire"));
        assertTrue(QueueMessages.member(false,"member-id",
                "town-Bradfordshire",12,2).contains("REVOKED town-Bradfordshire"));
    }
}
