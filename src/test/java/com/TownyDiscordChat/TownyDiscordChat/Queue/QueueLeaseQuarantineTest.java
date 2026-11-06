package com.TownyDiscordChat.TownyDiscordChat.Queue;

import com.TownyDiscordChat.TownyDiscordChat.Storage.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An expired REST lease is not evidence that its HTTP callback finished.
 * Excluding that ID lets higher-priority unrelated revocations proceed.
 */
class QueueLeaseQuarantineTest {
    @TempDir Path temp;

    @Test void anExpiredCreationDoesNotBlockPriorityDeletionOrGetClaimedTwice() throws Exception {
        long now=System.currentTimeMillis();
        try (Connection c=Database.openSQLite(temp.resolve("resources.db").toFile());
             Statement st=c.createStatement()) {
            st.executeUpdate("CREATE TABLE tdc_discord_jobs ("
                    +"id INTEGER PRIMARY KEY,job_key VARCHAR(191) UNIQUE,"
                    +"kind VARCHAR(40),resource_name VARCHAR(128),"
                    +"status VARCHAR(16),priority SMALLINT,attempts INT,"
                    +"available_at BIGINT,lease_until BIGINT)");
            st.executeUpdate("INSERT INTO tdc_discord_jobs VALUES "
                    +"(1,'TOWN_TEXT:A','TOWN_TEXT','A','RUNNING',100,1,0,"
                    +(now-1)+"),"
                    +"(2,'DELETE_TOWN_ROLE:B','DELETE_TOWN_ROLE','B','PENDING',10,0,0,0),"
                    +"(3,'TOWN_ROLE:C','TOWN_ROLE','C','PENDING',100,0,0,0)");
            DiscordJobStore.Job first=DiscordJobStore.claim(c,now,now+300000L,
                    Collections.singleton(1L));
            assertNotNull(first);
            assertEquals(2L,first.id,"Deletion must not wait for a hung create");
            DiscordJobStore.Job second=DiscordJobStore.claim(c,now,now+300000L,
                    Collections.singleton(1L));
            assertNotNull(second);
            assertEquals(3L,second.id);
            try (ResultSet rs=st.executeQuery(
                    "SELECT status,attempts FROM tdc_discord_jobs WHERE id=1")) {
                assertTrue(rs.next());
                assertEquals("RUNNING",rs.getString(1));
                assertEquals(1,rs.getInt(2),"Unresolved create was not reissued");
            }
        }
    }

    @Test void aHungGrantCannotStarveAnUnrelatedSecurityRevocation() throws Exception {
        long now=System.currentTimeMillis();
        try (Connection c=Database.openSQLite(temp.resolve("members.db").toFile());
             Statement st=c.createStatement()) {
            st.executeUpdate("CREATE TABLE tdc_member_intents ("
                    +"id INTEGER PRIMARY KEY,guild_id VARCHAR(24),"
                    +"member_id VARCHAR(24),role_id VARCHAR(24),desired INT,"
                    +"revision BIGINT,priority SMALLINT,status VARCHAR(16),"
                    +"attempts INT,available_at BIGINT,lease_until BIGINT)");
            st.executeUpdate("INSERT INTO tdc_member_intents VALUES "
                    +"(1,'123456789012345678','111111111111111111',"
                    +"'222222222222222222',1,1,100,'RUNNING',1,0,"+(now-1)+"),"
                    +"(2,'123456789012345678','333333333333333333',"
                    +"'222222222222222222',0,1,0,'PENDING',0,0,0),"
                    +"(3,'123456789012345678','444444444444444444',"
                    +"'222222222222222222',1,1,100,'PENDING',0,0,0)");
            MemberIntentStore.Intent revoke=MemberIntentStore.claim(c,now,now+300000L,
                    true,Collections.singleton(1L));
            assertNotNull(revoke);
            assertEquals(2L,revoke.id);
            assertFalse(revoke.desired);
            MemberIntentStore.Intent grant=MemberIntentStore.claim(c,now,now+300000L,
                    false,Collections.singleton(1L));
            assertNotNull(grant);
            assertEquals(3L,grant.id);
            try (ResultSet rs=st.executeQuery(
                    "SELECT attempts FROM tdc_member_intents WHERE id=1")) {
                assertTrue(rs.next());
                assertEquals(1,rs.getInt(1),
                        "Late grant must not have acquired a duplicate lease");
            }
        }
    }
}
