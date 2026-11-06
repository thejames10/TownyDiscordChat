package com.TownyDiscordChat.TownyDiscordChat.Queue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Migration 5: legacy pre-queue mapping helpers wrote Expired='false', but
 * modern reconciliation originally searched only Expired IS NULL. Normalize
 * active rows once, without changing Expired='true' records or old queue
 * history, so upgraded installations retain their Discord snowflake IDs.
 */
public final class LegacyExpiryMigration {
    private static final List<String> LEGACY_MAPPING_TABLES = Arrays.asList(
            "tdc_log_category", "tdc_log_text_channel", "tdc_players",
            "tdc_town_roles", "tdc_nation_roles",
            "tdc_town_categories", "tdc_nation_categories",
            "tdc_town_text_channels", "tdc_nation_text_channels",
            "tdc_town_voice_channels", "tdc_nation_voice_channels");

    private LegacyExpiryMigration() { }

    public static void migrate(Connection connection) throws SQLException {
        boolean originallyAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT version FROM tdc_schema_migrations WHERE version=5 FOR UPDATE");
                 ResultSet existing = ps.executeQuery()) {
                if (existing.next()) {
                    connection.commit();
                    return;
                }
            }
            for (String table : LEGACY_MAPPING_TABLES) {
                boolean exists;
                try (ResultSet tables = connection.getMetaData().getTables(
                        connection.getCatalog(),null,table,null)) {
                    exists=tables.next();
                }
                if (!exists) continue;
                // These are a fixed allowlist of internally managed tables.
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE " + table + " SET Expired=NULL "
                        + "WHERE LOWER(TRIM(Expired)) IN ('false','0','')")) {
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT IGNORE INTO tdc_schema_migrations "
                    + "(version,applied_at,description) VALUES "
                    + "(5,?,'Normalize active legacy mapping Expired flags')")) {
                ps.setLong(1,System.currentTimeMillis());
                ps.executeUpdate();
            }
            connection.commit();
        } catch (SQLException error) {
            connection.rollback();
            throw error;
        } finally {
            connection.setAutoCommit(originallyAutoCommit);
        }
    }
}
