package com.TownyDiscordChat.TownyDiscordChat.Queue;

import com.TownyDiscordChat.TownyDiscordChat.Storage.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Database-level regression coverage for resource creation/rename safety,
 * name reuse, and fencing a late callback after lease recovery.
 */
class DiscordRaceSafetyTest {
    @TempDir Path temp;

    @Test void pendingAndRunningCreationsBlockRenameUntilCallbackCompletes() throws Exception {
        try (Connection c=Database.openSQLite(temp.resolve("queue.db").toFile());
             Statement st=c.createStatement()) {
            setup(c);
            st.executeUpdate("INSERT INTO tdc_discord_jobs "
                    +"(id,job_key,kind,resource_name,status,attempts) VALUES "
                    +"(1,'TOWN_ROLE:OldName','TOWN_ROLE','OldName','RUNNING',1)");
            assertTrue(StableEntityStore.hasRunningCreates(c,"TOWN","OldName"));
            assertFalse(StableEntityStore.hasRunningCreates(c,"NATION","OldName"));
            assertFalse(StableEntityStore.hasRunningCreates(c,"TOWN","OtherName"));
            st.executeUpdate("UPDATE tdc_discord_jobs SET status='PENDING' WHERE id=1");
            assertFalse(StableEntityStore.hasRunningCreates(c,"TOWN","OldName"),
                    "Rename may proceed once the callback has resolved/deferred");
            st.executeUpdate("UPDATE tdc_discord_jobs SET kind='TOWN_VOICE',"
                    +"job_key='TOWN_VOICE:OldName',status='RUNNING' WHERE id=1");
            assertTrue(StableEntityStore.hasRunningCreates(c,"TOWN","OldName"),
                    "Voice channel creates must also fence in-place rename");
        }
    }

    @Test void immutableOwnershipRejectsAnotherTownWithTheSameName() throws Exception {
        try (Connection c=Database.openSQLite(temp.resolve("owners.db").toFile());
             Statement st=c.createStatement()) {
            setup(c);
            st.executeUpdate("INSERT INTO tdc_towny_identity "
                    +"(guild_id,kind,towny_uuid,last_name) VALUES "
                    +"('123456789012345678','TOWN','aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','Shared'),"
                    +"('123456789012345678','NATION','bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb','Shared'),"
                    +"('222222222222222222','TOWN','cccccccc-cccc-cccc-cccc-cccccccccccc','Shared')");
            Set<String> owners=StableEntityStore.ownersOfName(c,
                    "123456789012345678","TOWN","Shared");
            assertEquals(1,owners.size());
            assertTrue(owners.contains("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
            assertFalse(owners.contains("cccccccc-cccc-cccc-cccc-cccccccccccc"));
            assertFalse(owners.contains("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
            assertTrue(StableEntityStore.ownersOfName(c,
                    "123456789012345678","TOWN","Replacement").isEmpty());
        }
    }

    @Test void delayedCreateCallbackCannotOverwriteNewerAttemptOrRequeuedJob() throws Exception {
        try (Connection c=Database.openSQLite(temp.resolve("leases.db").toFile());
             Statement st=c.createStatement()) {
            setup(c);
            st.executeUpdate("INSERT INTO tdc_discord_jobs "
                    +"(id,job_key,kind,resource_name,status,attempts) VALUES "
                    +"(42,'TOWN_TEXT:OldName','TOWN_TEXT','OldName','RUNNING',1)");
            DiscordJobStore.Job first=new DiscordJobStore.Job(
                    42,"TOWN_TEXT","OldName",1,1234L);
            DiscordJobStore.Job second=new DiscordJobStore.Job(
                    42,"TOWN_TEXT","OldName",2,5678L);
            assertTrue(DiscordJobStore.stillOwned(c,first));
            st.executeUpdate("UPDATE tdc_discord_jobs SET attempts=2 WHERE id=42");
            assertFalse(DiscordJobStore.stillOwned(c,first),
                    "Old REST callback must not persist a stale object ID");
            assertTrue(DiscordJobStore.stillOwned(c,second));
            st.executeUpdate("UPDATE tdc_discord_jobs SET status='PENDING' WHERE id=42");
            assertFalse(DiscordJobStore.stillOwned(c,second),
                    "A deferred job has no active callback ownership");
        }
    }

    private static void setup(Connection c) throws Exception {
        try (Statement st=c.createStatement()) {
            st.executeUpdate("CREATE TABLE tdc_discord_jobs ("
                    +"id INTEGER PRIMARY KEY,job_key VARCHAR(191) UNIQUE,"
                    +"kind VARCHAR(40),resource_name VARCHAR(128),"
                    +"status VARCHAR(16),attempts INT)");
            st.executeUpdate("CREATE TABLE tdc_towny_identity ("
                    +"guild_id VARCHAR(24),kind VARCHAR(8),towny_uuid VARCHAR(36),"
                    +"last_name VARCHAR(128),role_id VARCHAR(24),"
                    +"text_id VARCHAR(24),voice_id VARCHAR(24),"
                    +"PRIMARY KEY(guild_id,kind,towny_uuid))");
        }
    }
}
