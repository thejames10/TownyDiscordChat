package com.TownyDiscordChat.TownyDiscordChat.Statistics;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Registers the custom bStats charts. Each chart id must match a chart added on
 * the plugin's bStats page with the same type, or bStats discards the data.
 */
public final class PluginMetrics {

    public static final String STATUS_CHART = "plugin_status";
    public static final String STORAGE_CHART = "storage_type";
    public static final String TOWNY_VERSION_CHART = "towny_version";
    public static final String DISCORDSRV_VERSION_CHART = "discordsrv_version";
    public static final String FEATURES_CHART = "enabled_features";

    static final String NOT_INSTALLED = "Not installed";
    static final String WAITING_FOR_DISCORD = "Waiting for Discord server";
    private static final String TOWNY_PLUGIN = "Towny";
    private static final String DISCORDSRV_PLUGIN = "DiscordSRV";
    private static final int ONE_SERVER = 1;

    private static final Map<String, String> FEATURE_KEYS = createFeatureKeys();

    private PluginMetrics() {
    }

    /** Live plugin state, read each time bStats submits (every 30 minutes). */
    public interface State {
        PluginStatus status();

        boolean guildValidated();

        String storageType();

        ConfigurationSection configuration();
    }

    public static void register(Metrics metrics, PluginManager plugins, State state) {
        metrics.addCustomChart(new DrilldownPie(STATUS_CHART,
                () -> statusDrilldown(state.status(), state.guildValidated())));
        metrics.addCustomChart(new SimplePie(STORAGE_CHART, state::storageType));
        metrics.addCustomChart(new SimplePie(TOWNY_VERSION_CHART,
                () -> dependencyVersion(plugins.getPlugin(TOWNY_PLUGIN))));
        metrics.addCustomChart(new SimplePie(DISCORDSRV_VERSION_CHART,
                () -> dependencyVersion(plugins.getPlugin(DISCORDSRV_PLUGIN))));
        metrics.addCustomChart(new AdvancedPie(FEATURES_CHART,
                () -> enabledFeatures(state.configuration())));
    }

    static Map<String, Map<String, Integer>> statusDrilldown(PluginStatus status,
                                                            boolean guildValidated) {
        String label = status == PluginStatus.RUNNING && !guildValidated
                ? WAITING_FOR_DISCORD
                : status.chartLabel();
        return Collections.singletonMap(status.chartGroup(),
                Collections.singletonMap(label, ONE_SERVER));
    }

    static String dependencyVersion(Plugin dependency) {
        if (dependency == null) {
            return NOT_INSTALLED;
        }
        return dependency.getDescription().getVersion();
    }

    /** Empty until config.yml has loaded; bStats skips an empty chart. */
    static Map<String, Integer> enabledFeatures(ConfigurationSection configuration) {
        Map<String, Integer> enabled = new LinkedHashMap<>();
        if (configuration == null) {
            return enabled;
        }
        for (Map.Entry<String, String> feature : FEATURE_KEYS.entrySet()) {
            if (configuration.getBoolean(feature.getValue(), false)) {
                enabled.put(feature.getKey(), ONE_SERVER);
            }
        }
        return enabled;
    }

    private static Map<String, String> createFeatureKeys() {
        Map<String, String> features = new LinkedHashMap<>();
        features.put("Town text channels", "town.CreateTextChannelForRole");
        features.put("Town voice channels", "town.CreateVoiceChannelForRole");
        features.put("Nation text channels", "nation.CreateTextChannelForRole");
        features.put("Nation voice channels", "nation.CreateVoiceChannelForRole");
        features.put("Discord activity log", "log.Enabled");
        features.put("Revoke unlinked accounts", "security.revokeUnlinkedAccounts");
        return Collections.unmodifiableMap(features);
    }

    /** Adapts a supplier pair for callers that keep state in fields. */
    public static State state(Supplier<PluginStatus> status, BooleanSupplier guildValidated,
                              Supplier<String> storageType,
                              Supplier<ConfigurationSection> configuration) {
        return new State() {
            @Override
            public PluginStatus status() {
                return status.get();
            }

            @Override
            public boolean guildValidated() {
                return guildValidated.getAsBoolean();
            }

            @Override
            public String storageType() {
                return storageType.get();
            }

            @Override
            public ConfigurationSection configuration() {
                return configuration.get();
            }
        };
    }
}
