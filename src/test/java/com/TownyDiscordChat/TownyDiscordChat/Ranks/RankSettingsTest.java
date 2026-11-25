package com.TownyDiscordChat.TownyDiscordChat.Ranks;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankSettingsTest {
    @Test
    void missingSectionMeansDisabled() {
        RankSettings settings = RankSettings.fromConfig(new YamlConfiguration());

        assertFalse(settings.enabled());
        assertTrue(settings.townRanks().isEmpty());
        assertTrue(settings.nationRanks().isEmpty());
    }

    @Test
    void blankRankNamesAreDropped() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(RankSettings.ENABLED_KEY, true);
        config.set(RankSettings.TOWN_RANKS_KEY, Arrays.asList(" assistant ", "", "sheriff"));
        config.set(RankSettings.NATION_RANKS_KEY, Collections.singletonList("  "));

        RankSettings settings = RankSettings.fromConfig(config);

        assertTrue(settings.enabled());
        assertEquals(Arrays.asList("assistant", "sheriff"), settings.townRanks());
        assertTrue(settings.nationRanks().isEmpty());
    }
}
