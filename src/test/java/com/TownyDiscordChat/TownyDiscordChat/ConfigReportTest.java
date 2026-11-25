package com.TownyDiscordChat.TownyDiscordChat;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigReportTest {
    @Test
    public void obsoleteSettingsAreNamedWithTheirNewHome() {
        YamlConfiguration file = new YamlConfiguration();
        file.set("messages.Prefix", "&2[TDC]");
        file.set("messages.Role.Add.Success", "&7Added role!");
        file.set("messages.TimeZone", "UTC");
        assertEquals(Arrays.asList("messages.Role", "messages.Prefix (now in messages.yml)"),
                ConfigReport.obsoleteSettings(file));
    }

    @Test
    public void sectionsOnlyInTheDefaultsAreMissing() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("queue.pollSeconds", 1);
        defaults.set("updates.check", true);
        YamlConfiguration file = new YamlConfiguration();
        file.set("queue.pollSeconds", 2);
        file.setDefaults(defaults);
        assertEquals(Collections.singletonList("updates"), ConfigReport.missingSections(file));
    }
}
