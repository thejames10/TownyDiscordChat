package com.TownyDiscordChat.TownyDiscordChat;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SetupValidatorTest {
    private YamlConfiguration configured() throws Exception {
        try (InputStream stream=getClass().getClassLoader().getResourceAsStream("config.yml")) {
            assertNotNull(stream);
            YamlConfiguration yaml=new YamlConfiguration();
            yaml.load(new InputStreamReader(stream,StandardCharsets.UTF_8));
            yaml.set("SQL.database","tdc_test");
            yaml.set("SQL.username","tdc_test");
            yaml.set("SQL.password","test_password");
            return yaml;
        }
    }

    @Test public void validDefaultsAllowAutomaticIds() throws Exception {
        YamlConfiguration yaml=configured();
        assertTrue(SetupValidator.validateLocal(yaml).valid(),
                SetupValidator.validateLocal(yaml).errors().toString());
        assertEquals("0",yaml.getString("town.TextCategoryId"));
    }

    @Test public void placeholdersBlockStartup() throws Exception {
        YamlConfiguration yaml=configured();
        yaml.set("SQL.database","blank");
        assertFalse(SetupValidator.validateLocal(yaml).valid());
    }

    @Test public void invalidPortAndHexColorFail() throws Exception {
        YamlConfiguration yaml=configured();
        yaml.set("SQL.port","not-a-port");
        yaml.set("town.RoleCreateColorCode","blue");
        SetupValidator.Result result=SetupValidator.validateLocal(yaml);
        assertFalse(result.valid());
        assertTrue(result.errors().toString().contains("SQL.port"));
        assertTrue(result.errors().toString().contains("town.RoleCreateColorCode"));
    }

    @Test public void sameTownAndNationCategoryIsUnsafe() throws Exception {
        YamlConfiguration yaml=configured();
        yaml.set("category.Nation.Name",yaml.getString("category.Town.Name"));
        assertFalse(SetupValidator.validateLocal(yaml).valid());
    }

    @Test public void unsupportedCategoryToggleFailsInsteadOfSilentlyIgnored() throws Exception {
        YamlConfiguration yaml=configured();
        yaml.set("town.UseCategoryForText",false);
        assertFalse(SetupValidator.validateLocal(yaml).valid());
    }

    @Test public void legacyNonZeroIdsAreChecked() throws Exception {
        YamlConfiguration yaml=configured();
        yaml.set("town.TextCategoryId","banana");
        assertFalse(SetupValidator.validateLocal(yaml).valid());
        yaml.set("town.TextCategoryId","123456789012345678");
        assertTrue(SetupValidator.validateLocal(yaml).valid());
        assertFalse(SetupValidator.validateLocal(yaml).warnings().isEmpty());
    }

    @Test public void brokenYamlFileDoesNotSilentlyLoadDefaults() throws Exception {
        Path file=Files.createTempFile("tdc-invalid-config",".yml");
        try {
            Files.write(file, "queue: [unterminated".getBytes(StandardCharsets.UTF_8));
            assertFalse(SetupValidator.validateFile(file.toFile()).valid());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test public void autoStorageDoesNotRequireMySQLCredentials() throws Exception {
        YamlConfiguration yaml=configured();
        yaml.set("storage.type","auto");
        yaml.set("SQL.database","blank");
        yaml.set("SQL.username","blank");
        yaml.set("SQL.password","blank");
        assertTrue(SetupValidator.validateLocal(yaml).valid(),
                SetupValidator.validateLocal(yaml).errors().toString());
    }

    @Test public void mysqlBackendStillRequiresCredentials() throws Exception {
        YamlConfiguration yaml=configured();
        yaml.set("storage.type","mysql");
        yaml.set("SQL.database","blank");
        assertFalse(SetupValidator.validateLocal(yaml).valid());
    }

    @Test public void migrationMustUseExplicitSqliteMode() throws Exception {
        YamlConfiguration yaml=configured();
        yaml.set("storage.type","mysql");
        yaml.set("storage.migrateToMySQL",true);
        assertFalse(SetupValidator.validateLocal(yaml).valid());
    }

    @Test public void sqlitePathTraversalIsBlocked() throws Exception {
        YamlConfiguration yaml=configured();
        yaml.set("storage.type","sqlite");
        yaml.set("storage.sqlite.file","../outside.db");
        assertFalse(SetupValidator.validateLocal(yaml).valid());
    }

    @Test public void badTimeZoneAndDatePatternFail() throws Exception {
        YamlConfiguration yaml=configured();
        yaml.set("messages.TimeZone","NoSuch/Zone");
        yaml.set("messages.DateFormat","unmatched-quote '");
        assertFalse(SetupValidator.validateLocal(yaml).valid());
    }
}
