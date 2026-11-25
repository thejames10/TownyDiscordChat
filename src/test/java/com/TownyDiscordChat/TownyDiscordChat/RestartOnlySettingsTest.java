package com.TownyDiscordChat.TownyDiscordChat;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RestartOnlySettingsTest {
    private static YamlConfiguration config(String storageType, int pollSeconds, String townCategory) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("storage.type", storageType);
        config.set("SQL.host", "localhost");
        config.set("queue.pollSeconds", pollSeconds);
        config.set("category.Town.Name", townCategory);
        return config;
    }

    @Test
    public void storageAndTimerChangesNeedARestart() {
        assertEquals(Arrays.asList("queue.pollSeconds", "storage.type"),
                RestartOnlySettings.changed(config("sqlite", 1, "TOWNS"), config("mysql", 5, "TOWNS")));
    }

    @Test
    public void otherSettingsApplyOnReload() {
        assertEquals(Collections.emptyList(),
                RestartOnlySettings.changed(config("sqlite", 1, "TOWNS"), config("sqlite", 1, "CITIES")));
    }

    @Test
    public void removedStorageSettingsCount() {
        YamlConfiguration before = config("sqlite", 1, "TOWNS");
        YamlConfiguration after = config("sqlite", 1, "TOWNS");
        after.set("SQL.host", null);
        assertEquals(Collections.singletonList("SQL.host"), RestartOnlySettings.changed(before, after));
    }
}
