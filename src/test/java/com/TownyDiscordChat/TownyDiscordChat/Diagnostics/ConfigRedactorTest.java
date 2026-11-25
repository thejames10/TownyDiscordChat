package com.TownyDiscordChat.TownyDiscordChat.Diagnostics;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigRedactorTest {
    @Test
    public void credentialsAreHiddenAndOtherValuesShown() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("SQL.host", "db.internal");
        config.set("SQL.username", "tdc");
        config.set("SQL.password", "hunter2");
        config.set("SQL.database", "towny");
        config.set("updates.apiToken", "abc");
        config.set("storage.type", "mysql");

        String report = String.join("\n", ConfigRedactor.lines(config));

        assertFalse(report.contains("db.internal"));
        assertFalse(report.contains("hunter2"));
        assertFalse(report.contains("abc"));
        assertTrue(report.contains("SQL.password: (set, hidden)"));
        assertTrue(report.contains("SQL.database: towny"));
        assertTrue(report.contains("storage.type: mysql"));
    }

    @Test
    public void emptyCredentialsAreReportedAsNotSet() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("SQL.password", "");
        assertTrue(ConfigRedactor.lines(config).contains("SQL.password: (not set)"));
    }

    @Test
    public void missingConfigIsExplained() {
        List<String> lines = ConfigRedactor.lines(null);
        assertTrue(lines.get(0).contains("not loaded"));
    }
}
