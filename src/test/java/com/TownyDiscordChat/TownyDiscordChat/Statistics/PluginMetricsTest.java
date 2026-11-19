package com.TownyDiscordChat.TownyDiscordChat.Statistics;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PluginMetricsTest {

    @Test
    public void runningWithVerifiedDiscordReportsRunning() {
        assertEquals(drilldown("Running", "Running"),
                PluginMetrics.statusDrilldown(PluginStatus.RUNNING, true));
    }

    @Test
    public void runningBeforeDiscordIsVerifiedReportsWaiting() {
        assertEquals(drilldown("Running", PluginMetrics.WAITING_FOR_DISCORD),
                PluginMetrics.statusDrilldown(PluginStatus.RUNNING, false));
    }

    @Test
    public void safeModeReportsItsCauseUnderOneGroup() {
        assertEquals(drilldown("Safe mode", "Upgraded config awaiting review"),
                PluginMetrics.statusDrilldown(PluginStatus.CONFIG_REVIEW, false));
    }

    @Test
    public void everyStatusHasAFixedLabel() {
        for (PluginStatus status : PluginStatus.values()) {
            assertTrue(!status.chartGroup().isEmpty() && !status.chartLabel().isEmpty(),
                    status.name());
        }
    }

    @Test
    public void missingDependencyIsReportedAsNotInstalled() {
        assertEquals(PluginMetrics.NOT_INSTALLED, PluginMetrics.dependencyVersion(null));
    }

    @Test
    public void featuresAreEmptyBeforeConfigurationLoads() {
        assertTrue(PluginMetrics.enabledFeatures(null).isEmpty());
    }

    @Test
    public void onlyEnabledFeaturesAreCounted() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("town.CreateTextChannelForRole", true);
        configuration.set("town.CreateVoiceChannelForRole", false);
        configuration.set("log.Enabled", true);

        Map<String, Integer> features = PluginMetrics.enabledFeatures(configuration);

        Set<String> expected = new HashSet<>(Arrays.asList(
                "Town text channels", "Discord activity log"));
        assertEquals(expected, features.keySet());
        assertTrue(features.values().stream().allMatch(count -> count == 1));
    }

    private static Map<String, Map<String, Integer>> drilldown(String group, String label) {
        return Collections.singletonMap(group, Collections.singletonMap(label, 1));
    }
}
