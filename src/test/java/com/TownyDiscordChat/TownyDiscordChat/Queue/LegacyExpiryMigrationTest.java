package com.TownyDiscordChat.TownyDiscordChat.Queue;

import com.TownyDiscordChat.TownyDiscordChat.Storage.Database;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class LegacyExpiryMigrationTest {
    @TempDir Path directory;

    @Test void upgradesActiveRowsWithoutChangingExpiredOrLegacyQueueHistory() throws Exception {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("storage.type","sqlite");
        cfg.set("storage.sqlite.file","legacy.db");
        Database db = new Database(cfg,directory.toFile());
        try (Connection connection=db.openConnection(); Statement st=connection.createStatement()) {
            st.executeUpdate("CREATE TABLE tdc_schema_migrations "
                    +"(version INT PRIMARY KEY,applied_at BIGINT NOT NULL,"
                    +"description VARCHAR(190) NOT NULL)");
            st.executeUpdate("CREATE TABLE tdc_town_roles "
                    +"(townRoleName VARCHAR(100) PRIMARY KEY,townRoleId VARCHAR(100),Expired VARCHAR(100))");
            st.executeUpdate("CREATE TABLE tdc_nation_voice_channels "
                    +"(nationRoleName VARCHAR(100) PRIMARY KEY,nationVoiceChannelId VARCHAR(100),Expired VARCHAR(100))");
            st.executeUpdate("CREATE TABLE tdc_queued_tasks "
                    +"(Time VARCHAR(32) PRIMARY KEY,Expired VARCHAR(100))");
            st.executeUpdate("INSERT INTO tdc_town_roles VALUES "
                    +"('active','123456789012345678','false'),"
                    +"('active-zero','123456789012345679','0'),"
                    +"('retired','123456789012345680','true'),"
                    +"('already-normalized','123456789012345681',NULL)");
            st.executeUpdate("INSERT INTO tdc_nation_voice_channels VALUES "
                    +"('Nation','123456789012345682','false')");
            st.executeUpdate("INSERT INTO tdc_queued_tasks VALUES ('1','false')");

            LegacyExpiryMigration.migrate(connection);
            LegacyExpiryMigration.migrate(connection);

            try (ResultSet rs=st.executeQuery("SELECT COUNT(*) FROM tdc_town_roles "
                    +"WHERE Expired IS NULL")) {
                assertTrue(rs.next());
                assertEquals(3,rs.getInt(1));
            }
            try (ResultSet rs=st.executeQuery("SELECT Expired FROM tdc_town_roles "
                    +"WHERE townRoleName='retired'")) {
                assertTrue(rs.next());
                assertEquals("true",rs.getString(1));
            }
            try (ResultSet rs=st.executeQuery("SELECT Expired FROM tdc_nation_voice_channels")) {
                assertTrue(rs.next());
                assertNull(rs.getString(1));
            }
            try (ResultSet rs=st.executeQuery("SELECT Expired FROM tdc_queued_tasks")) {
                assertTrue(rs.next());
                assertEquals("false",rs.getString(1),
                        "Legacy queued tasks must not be overwritten by mapping migration");
            }
            try (ResultSet rs=st.executeQuery("SELECT COUNT(*) FROM tdc_schema_migrations "
                    +"WHERE version=5")) {
                assertTrue(rs.next());
                assertEquals(1,rs.getInt(1));
            }
        }
    }
}
