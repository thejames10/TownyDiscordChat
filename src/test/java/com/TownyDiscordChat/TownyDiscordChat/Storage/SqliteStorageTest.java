package com.TownyDiscordChat.TownyDiscordChat.Storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class SqliteStorageTest {
    @TempDir Path temp;

    private Database sqlite() {
        YamlConfiguration config=new YamlConfiguration();
        config.set("storage.type","auto");
        config.set("storage.sqlite.file","test.db");
        config.set("SQL.database","blank");
        config.set("SQL.username","blank");
        config.set("SQL.password","blank");
        return new Database(config,temp.toFile());
    }

    @Test void autoDefaultsToSQLiteWhenMysqlIsNotConfigured() throws Exception {
        Database db=sqlite();
        assertEquals(Database.Backend.SQLITE,db.getBackend());
        try (Connection c=db.openConnection(); Statement st=c.createStatement();
             ResultSet rs=st.executeQuery("PRAGMA journal_mode")) {
            assertTrue(rs.next());
            assertEquals("wal",rs.getString(1).toLowerCase());
        }
    }

    @Test void existingMySQLConfigurationSelectsMySQLWithoutSwitchingData() {
        YamlConfiguration config=new YamlConfiguration();
        config.set("storage.type","auto");
        config.set("SQL.host","localhost");
        config.set("SQL.database","legacy_database");
        config.set("SQL.username","legacy_user");
        config.set("SQL.password","secret");
        assertEquals(Database.Backend.MYSQL,new Database(config,temp.toFile()).getBackend());
    }

    @Test void sqlitePreservesMysqlJobSchemaAndIgnoreSemantics() throws Exception {
        try (Connection c=sqlite().openConnection(); Statement st=c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS tdc_discord_jobs ("
                    +"id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    +"job_key VARCHAR(191) NOT NULL UNIQUE,"
                    +"kind VARCHAR(40) NOT NULL,"
                    +"resource_name VARCHAR(128) NOT NULL,"
                    +"status VARCHAR(16) NOT NULL DEFAULT 'PENDING',"
                    +"INDEX idx_ready (status,resource_name)"
                    +") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            st.executeUpdate("INSERT IGNORE INTO tdc_discord_jobs (job_key,kind,resource_name)"
                    +" VALUES ('TOWN_ROLE:Alpha','TOWN_ROLE','Alpha')");
            st.executeUpdate("INSERT IGNORE INTO tdc_discord_jobs (job_key,kind,resource_name)"
                    +" VALUES ('TOWN_ROLE:Alpha','TOWN_ROLE','Alpha')");
            try (ResultSet rs=st.executeQuery("SELECT COUNT(*),MIN(id) FROM tdc_discord_jobs")) {
                assertTrue(rs.next());
                assertEquals(1,rs.getInt(1));
                assertEquals(1,rs.getInt(2));
            }
        }
    }

    @Test void sqliteCoalescesMembershipWithNestedMysqlIfAndUpsert() throws Exception {
        try (Connection c=sqlite().openConnection(); Statement st=c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS tdc_member_intents ("
                    +"id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    +"guild_id VARCHAR(24) NOT NULL,"
                    +"member_id VARCHAR(24) NOT NULL,"
                    +"role_id VARCHAR(24) NOT NULL,"
                    +"desired TINYINT(1) NOT NULL,"
                    +"revision BIGINT NOT NULL DEFAULT 1,"
                    +"status VARCHAR(16) NOT NULL DEFAULT 'PENDING',"
                    +"attempts INT NOT NULL DEFAULT 0,"
                    +"available_at BIGINT NOT NULL DEFAULT 0,"
                    +"priority SMALLINT NOT NULL DEFAULT 100,"
                    +"UNIQUE KEY uq_member_role (guild_id,member_id,role_id),"
                    +"KEY idx_member_claim (desired,status,priority,available_at)"
                    +") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            String insert="INSERT INTO tdc_member_intents "
                    +"(guild_id,member_id,role_id,desired) VALUES (?,?,?,?) "
                    +"ON DUPLICATE KEY UPDATE "
                    +"status=IF(desired<>VALUES(desired),IF(status='RUNNING','RUNNING','PENDING'),status),"
                    +"attempts=IF(desired<>VALUES(desired),0,attempts),"
                    +"revision=IF(desired<>VALUES(desired),revision+1,revision),"
                    +"desired=VALUES(desired)";
            try (PreparedStatement ps=c.prepareStatement(insert)) {
                ps.setString(1,"123456789012345678");
                ps.setString(2,"222222222222222222");
                ps.setString(3,"333333333333333333");
                ps.setBoolean(4,true);
                ps.executeUpdate();
                ps.setBoolean(4,false);
                ps.executeUpdate();
                ps.setBoolean(4,false);
                ps.executeUpdate();
            }
            try (ResultSet rs=st.executeQuery("SELECT desired,revision,COUNT(*) FROM tdc_member_intents")) {
                assertTrue(rs.next());
                assertFalse(rs.getBoolean(1));
                assertEquals(2,rs.getLong(2));
                assertEquals(1,rs.getInt(3));
            }
        }
    }

    @Test void sqlitePathCannotEscapePluginDataFolder() {
        YamlConfiguration cfg=new YamlConfiguration();
        cfg.set("storage.type","sqlite");
        cfg.set("storage.sqlite.file","../../other.db");
        assertThrows(IllegalArgumentException.class,()->new Database(cfg,temp.toFile()));
    }

    @Test void dialectRefusesUnknownIfSyntaxRatherThanEmittingBadQueries() {
        assertThrows(IllegalArgumentException.class,
                ()->SqlDialect.sqlite("UPDATE x SET a=IF(a,0)"));
    }
}
