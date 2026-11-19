package com.TownyDiscordChat.TownyDiscordChat.Statistics;

/**
 * Why the plugin is or is not synchronizing, grouped for the bStats status chart.
 * Labels are fixed so the chart never carries server-specific error text.
 */
public enum PluginStatus {
    STARTING(Group.STARTING, "Starting"),
    RUNNING(Group.RUNNING, "Running"),
    STORAGE_MIGRATION(Group.STORAGE_MIGRATION, "SQLite to MySQL migration"),
    MISSING_DEPENDENCY(Group.SAFE_MODE, "Missing or incompatible dependency"),
    CONFIG_UPGRADE_FAILED(Group.SAFE_MODE, "Config upgrade failed"),
    CONFIG_REVIEW(Group.SAFE_MODE, "Upgraded config awaiting review"),
    INVALID_CONFIG(Group.SAFE_MODE, "Invalid config.yml"),
    DATABASE_UNAVAILABLE(Group.SAFE_MODE, "Database connection failed"),
    DATABASE_SCHEMA_FAILED(Group.SAFE_MODE, "Database schema failed");

    private static final class Group {
        private static final String STARTING = "Starting";
        private static final String RUNNING = "Running";
        private static final String STORAGE_MIGRATION = "Storage migration";
        private static final String SAFE_MODE = "Safe mode";
    }

    private final String chartGroup;
    private final String chartLabel;

    PluginStatus(String chartGroup, String chartLabel) {
        this.chartGroup = chartGroup;
        this.chartLabel = chartLabel;
    }

    public String chartGroup() {
        return chartGroup;
    }

    public String chartLabel() {
        return chartLabel;
    }
}
