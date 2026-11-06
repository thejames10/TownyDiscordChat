package com.TownyDiscordChat.TownyDiscordChat;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigUpgradeTest {
    @TempDir Path data;

    private String defaults() throws Exception {
        try (InputStream input=getClass().getClassLoader().getResourceAsStream("config.yml")) {
            assertNotNull(input,"Bundled configuration is required");
            java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();
            byte[] chunk=new byte[8192];
            int count;
            while ((count=input.read(chunk))!=-1) buffer.write(chunk,0,count);
            return new String(buffer.toByteArray(),StandardCharsets.UTF_8);
        }
    }

    private void backup(String yaml) throws Exception {
        Files.write(data.resolve("config.yml.bak"),yaml.getBytes(StandardCharsets.UTF_8));
    }

    @Test void freshBackupMigratesRecognizedValuesWithNewDefaultsAndComments() throws Exception {
        String old=String.join("\n",
                "SQL:",
                "  host: 'db.internal'",
                "  port: '3307'",
                "  database: 'old_tdc'",
                "  username: 'old_user'",
                "  password: 'c0l:on#safe'",
                "town:",
                "  RoleCreateColorCode: '0xffaabb'",
                "  TextCategoryId: '123456789012345678'",
                "category:",
                "  Town:",
                "    Name: 'OLD-TOWN'",
                "  Nation:",
                "    Name: 'OLD-NATION'",
                "messages:",
                "  Prefix: '&2OldTowny'",
                "  DiscordLogTextChannelId: 0",
                "  LegacyMessageThatNoLongerExists: 'ignored'",
                "Discord:",
                "  Api:",
                "    RateLimit: 1",
                "storage:",
                "  migrateToMySQL: true",
                "");
        backup(old);
        ConfigUpgrade.Result result=ConfigUpgrade.migrateIfNeeded(data.toFile(),defaults());
        assertTrue(result.migrated);
        assertTrue(result.copied.contains("SQL.database"));
        assertTrue(result.copied.contains("messages.Prefix"));
        assertTrue(result.ignored.contains("storage.migrateToMySQL"));
        assertTrue(result.ignored.contains("messages.LegacyMessageThatNoLongerExists"));
        assertTrue(result.ignored.contains("Discord.Api.RateLimit"));
        assertEquals(old,new String(Files.readAllBytes(data.resolve("config.yml.bak")),
                StandardCharsets.UTF_8),"Do not edit or rename the backup");
        String current=new String(Files.readAllBytes(data.resolve("config.yml")),
                StandardCharsets.UTF_8);
        assertTrue(current.contains("# Storage options:"),
                "Comments from the new bundled config must be retained");
        assertTrue(current.contains("# Durable SQLite/MySQL Discord reconciliation queue."));
        YamlConfiguration merged=new YamlConfiguration();
        merged.load(data.resolve("config.yml").toFile());
        assertEquals("old_tdc",merged.getString("SQL.database"));
        assertEquals("c0l:on#safe",merged.getString("SQL.password"));
        assertEquals("123456789012345678",merged.getString("town.TextCategoryId"));
        assertEquals("OLD-TOWN",merged.getString("category.Town.Name"));
        assertEquals("&2OldTowny",merged.getString("messages.Prefix"));
        assertEquals(180,merged.getInt("queue.reconcileSeconds"),
                "New defaults should remain for missing backup paths");
        assertFalse(merged.getBoolean("storage.migrateToMySQL"),
                "Never replay an offline DB transfer just because it was in a backup");
        assertEquals("auto",merged.getString("storage.type"),
                "Auto continues MySQL when old credentials are configured");
        assertTrue(SetupValidator.validateLocal(merged).valid());
    }

    @Test void existingConfigAlwaysWinsEvenWhenBackupIsValid() throws Exception {
        String active="town:\n  RoleCreateColorCode: '0x000000'\n";
        Files.write(data.resolve("config.yml"),active.getBytes(StandardCharsets.UTF_8));
        backup("town:\n  RoleCreateColorCode: '0xabcdef'\n");
        ConfigUpgrade.Result result=ConfigUpgrade.migrateIfNeeded(data.toFile(),defaults());
        assertFalse(result.migrated);
        assertEquals(active,new String(Files.readAllBytes(data.resolve("config.yml")),
                StandardCharsets.UTF_8));
    }

    @Test void missingBackupLeavesOrdinaryFirstStartAlone() throws Exception {
        ConfigUpgrade.Result result=ConfigUpgrade.migrateIfNeeded(data.toFile(),defaults());
        assertFalse(result.migrated);
        assertFalse(Files.exists(data.resolve("config.yml")));
    }

    @Test void malformedYamlFailsClosedWithoutPublishingConfig() throws Exception {
        backup("queue: [unterminated");
        assertThrows(java.io.IOException.class,
                ()->ConfigUpgrade.migrateIfNeeded(data.toFile(),defaults()));
        assertFalse(Files.exists(data.resolve("config.yml")));
        assertTrue(Files.exists(data.resolve("config.yml.bak")));
    }

    @Test void invalidTypesOrUnsupportedOldBehaviorFailClosed() throws Exception {
        backup("town:\n  UseCategoryForText: 'false'\n");
        assertThrows(java.io.IOException.class,
                ()->ConfigUpgrade.migrateIfNeeded(data.toFile(),defaults()));
        assertFalse(Files.exists(data.resolve("config.yml")));
        backup("town:\n  UseCategoryForText: false\n");
        assertThrows(java.io.IOException.class,
                ()->ConfigUpgrade.migrateIfNeeded(data.toFile(),defaults()),
                "Current secure category requirement must not be weakened");
        assertFalse(Files.exists(data.resolve("config.yml")));
    }

    @Test void mysqlSettingsCannotSilentlyFallBackToEmptySqlite() throws Exception {
        backup("SQL:\n  database: old_database\n");
        java.io.IOException problem=assertThrows(java.io.IOException.class,
                ()->ConfigUpgrade.migrateIfNeeded(data.toFile(),defaults()));
        assertTrue(problem.getMessage().contains("validation"));
        assertFalse(Files.exists(data.resolve("config.yml")));
    }

    @Test void noRecognizedSettingsIsNotTreatedAsSuccessfulMigration() throws Exception {
        backup("OldVersion:\n  ApiLimit: 10\n");
        assertThrows(java.io.IOException.class,
                ()->ConfigUpgrade.migrateIfNeeded(data.toFile(),defaults()));
        assertFalse(Files.exists(data.resolve("config.yml")));
    }

    @Test void quotedSpecialCharactersRoundTripWithoutYamlInjection() throws Exception {
        String message="Town's \"private\" chat: C:\\temp\\new\n  queue: fake";
        YamlConfiguration old=new YamlConfiguration();
        old.set("messages.Prefix",message);
        old.save(data.resolve("config.yml.bak").toFile());
        ConfigUpgrade.migrateIfNeeded(data.toFile(),defaults());
        YamlConfiguration merged=new YamlConfiguration();
        merged.load(data.resolve("config.yml").toFile());
        assertEquals(message,merged.getString("messages.Prefix"));
        assertEquals(1,merged.getInt("queue.pollSeconds"),
                "Embedded YAML-looking text must remain a single string");
    }
}
