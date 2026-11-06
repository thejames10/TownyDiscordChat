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

    @Test void deletedTownIdentityCannotRetireBeforeInflightCreateFinishes() throws Exception {
        try (Connection c=Database.openSQLite(temp.resolve("retire.db").toFile());
             Statement st=c.createStatement()) {
            setup(c);
            st.executeUpdate("INSERT INTO tdc_towny_identity "
                    +"(guild_id,kind,towny_uuid,last_name) VALUES "
                    +"('123456789012345678','TOWN',"
                    +"'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','DeletedTown')");
            st.executeUpdate("INSERT INTO tdc_discord_jobs "
                    +"(id,job_key,kind,resource_name,status,attempts) VALUES "
                    +"(1,'TOWN_ROLE:DeletedTown','TOWN_ROLE','DeletedTown','RUNNING',1)");
            StableEntityStore.retireIfEmpty(c,"123456789012345678","TOWN",
                    "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
            try (ResultSet rs=st.executeQuery("SELECT COUNT(*) FROM tdc_towny_identity")) {
                assertTrue(rs.next());
                assertEquals(1,rs.getInt(1),
                        "Must retain stable ownership until late creation can be recorded");
            }
            st.executeUpdate("UPDATE tdc_discord_jobs SET status='PENDING' WHERE id=1");
            StableEntityStore.retireIfEmpty(c,"123456789012345678","TOWN",
                    "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
            try (ResultSet rs=st.executeQuery("SELECT COUNT(*) FROM tdc_towny_identity")) {
                assertTrue(rs.next());
                assertEquals(0,rs.getInt(1),"No remote ID and no running job: safe to retire");
            }
            try (ResultSet rs=st.executeQuery("SELECT COUNT(*) FROM tdc_discord_jobs")) {
                assertTrue(rs.next());
                assertEquals(0,rs.getInt(1),"Pending obsolete creations must be cancelled");
            }
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

    @Test void orphanCleanupUsesStableIdWhenLegacyRowIsMissing() throws Exception {
        try (Connection c=Database.openSQLite(temp.resolve("orphan.db").toFile());
             Statement st=c.createStatement()) {
            setup(c);
            st.executeUpdate("INSERT INTO tdc_towny_identity "
                    +"(guild_id,kind,towny_uuid,last_name,role_id,text_id,voice_id) VALUES "
                    +"('123456789012345678','TOWN','aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',"
                    +"'OldTown','111111111111111111','222222222222222222','333333333333333333')");
            assertEquals("111111111111111111",
                    StableEntityStore.uniqueIdForName(c,
                            "123456789012345678","TOWN","OldTown","ROLE"));
            assertEquals("222222222222222222",
                    StableEntityStore.uniqueIdForName(c,
                            "123456789012345678","TOWN","OldTown","TEXT"));
            assertEquals("333333333333333333",
                    StableEntityStore.uniqueIdForName(c,
                            "123456789012345678","TOWN","OldTown","VOICE"));
            assertNull(StableEntityStore.uniqueIdForName(c,
                    "123456789012345678","TOWN","Unknown","ROLE"));
            st.executeUpdate("INSERT INTO tdc_towny_identity "
                    +"(guild_id,kind,towny_uuid,last_name,role_id) VALUES "
                    +"('123456789012345678','TOWN','bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',"
                    +"'OldTown','444444444444444444')");
            assertThrows(java.sql.SQLException.class, () ->
                    StableEntityStore.uniqueIdForName(c,
                            "123456789012345678","TOWN","OldTown","ROLE"),
                    "Conflicting UUID owners cannot be picked by first-row order");
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
