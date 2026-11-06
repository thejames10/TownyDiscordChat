package com.TownyDiscordChat.TownyDiscordChat.Storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/** Optional MySQL service integration test, enabled in the CI migration job. */
class StorageMigrationTest {
    @TempDir Path temp;

    @Test void copiesMappedResourcesAndDeadJobsWithoutDiscordApiCalls() throws Exception {
        String url=System.getenv("TDC_TEST_MYSQL_URL");
        Assumptions.assumeTrue(url!=null && !url.trim().isEmpty(),
                "Requires MySQL CI service");
        String username=System.getenv("TDC_TEST_MYSQL_USER");
        String password=System.getenv("TDC_TEST_MYSQL_PASSWORD");
        YamlConfiguration cfg=new YamlConfiguration();
        cfg.set("storage.type","sqlite");
        cfg.set("storage.sqlite.file","source.db");
        Database source=new Database(cfg,temp.toFile());
        try (Connection sqlite=source.openConnection(); Statement st=sqlite.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS tdc_schema_migrations "
                    +"(version INT PRIMARY KEY, applied_at BIGINT NOT NULL,description VARCHAR(190) NOT NULL)");
            st.executeUpdate("INSERT INTO tdc_schema_migrations(version,applied_at,description)"
                    +" VALUES (2,1,'Legacy queue imported')");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS tdc_town_roles "
                    +"(townRoleName VARCHAR(100),townRoleId VARCHAR(100),Expired VARCHAR(100),"
                    +" PRIMARY KEY(townRoleName))");
            st.executeUpdate("INSERT INTO tdc_town_roles VALUES "
                    +"('NewTown','889900112233445566',NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS tdc_towny_identity ("
                    +"guild_id VARCHAR(24) NOT NULL,kind VARCHAR(8) NOT NULL,"
                    +"towny_uuid VARCHAR(36) NOT NULL,last_name VARCHAR(128) NOT NULL,"
                    +"role_id VARCHAR(24),text_id VARCHAR(24),voice_id VARCHAR(24),"
                    +"PRIMARY KEY(guild_id,kind,towny_uuid),"
                    +"KEY idx_identity_name(guild_id,kind,last_name)) ENGINE=InnoDB");
            st.executeUpdate("INSERT INTO tdc_towny_identity VALUES "
                    +"('123456789012345678','TOWN','11111111-1111-1111-1111-111111111111',"
                    +"'NewTown','889900112233445566',NULL,NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS tdc_discord_jobs ("
                    +"id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    +"job_key VARCHAR(191) NOT NULL UNIQUE,kind VARCHAR(40) NOT NULL,"
                    +"resource_name VARCHAR(128) NOT NULL,status VARCHAR(16) NOT NULL,"
                    +"attempts INT NOT NULL DEFAULT 0,available_at BIGINT NOT NULL DEFAULT 0,"
                    +"lease_until BIGINT NOT NULL DEFAULT 0,"
                    +"INDEX idx_ready(status,available_at,lease_until)) ENGINE=InnoDB");
            st.executeUpdate("INSERT INTO tdc_discord_jobs "
                    +"(id,job_key,kind,resource_name,status,attempts,available_at,lease_until)"
                    +" VALUES (21,'DELETE_TOWN_ROLE:Old','DELETE_TOWN_ROLE','Old','DEAD',9,12345,0)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS tdc_member_intents ("
                    +"id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    +"guild_id VARCHAR(24) NOT NULL,member_id VARCHAR(24) NOT NULL,"
                    +"role_id VARCHAR(24) NOT NULL,desired TINYINT(1) NOT NULL,"
                    +"revision BIGINT NOT NULL DEFAULT 1,priority SMALLINT NOT NULL DEFAULT 100,"
                    +"attempts INT NOT NULL DEFAULT 0,available_at BIGINT NOT NULL DEFAULT 0,"
                    +"lease_until BIGINT NOT NULL DEFAULT 0,"
                    +"status VARCHAR(16) NOT NULL DEFAULT 'PENDING',"
                    +"UNIQUE KEY uq_member_role(guild_id,member_id,role_id)) ENGINE=InnoDB");
            st.executeUpdate("INSERT INTO tdc_member_intents "
                    +"(id,guild_id,member_id,role_id,desired,revision,priority,status)"
                    +" VALUES (9,'123456789012345678','111122223333444455','889900112233445566',"
                    +"0,6,0,'PENDING')");
        }
        StorageMigration.Result result=StorageMigration.sqliteToMySQL(source,url,
                username,password,Logger.getLogger("StorageMigrationTest"));
        assertEquals(5,result.tables);
        assertEquals(5,result.rows);
        try (Connection mysql=Database.openMySQL(url,username,password);
             Statement st=mysql.createStatement()) {
            try (ResultSet rs=st.executeQuery(
                    "SELECT id,status,attempts FROM tdc_discord_jobs WHERE id=21")) {
                assertTrue(rs.next());
                assertEquals(21,rs.getLong(1));
                assertEquals("DEAD",rs.getString(2));
                assertEquals(9,rs.getInt(3));
            }
            try (ResultSet rs=st.executeQuery(
                    "SELECT townRoleId FROM tdc_town_roles WHERE townRoleName='NewTown'")) {
                assertTrue(rs.next());
                assertEquals("889900112233445566",rs.getString(1));
            }
            try (ResultSet rs=st.executeQuery(
                    "SELECT revision,desired FROM tdc_member_intents WHERE id=9")) {
                assertTrue(rs.next());
                assertEquals(6,rs.getLong(1));
                assertFalse(rs.getBoolean(2));
            }
            try (ResultSet rs=st.executeQuery(
                    "SELECT COUNT(*) FROM tdc_schema_migrations WHERE version=6")) {
                assertTrue(rs.next());
                assertEquals(1,rs.getInt(1));
            }
        }
        assertThrows(java.sql.SQLException.class,()->StorageMigration.sqliteToMySQL(
                source,url,username,password,null),"Refuse merge into nonempty MySQL");
        try (Connection sqlite=source.openConnection();
             Statement st=sqlite.createStatement();
             ResultSet rs=st.executeQuery("SELECT COUNT(*) FROM tdc_town_roles")) {
            assertTrue(rs.next());
            assertEquals(1,rs.getInt(1),"SQLite source must remain intact");
        }
    }
}
