package com.TownyDiscordChat.TownyDiscordChat.Queue;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.Ranks.RankSlot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Rank permissions this plugin granted. Only recorded grants are ever
 * revoked, so moderation an admin gave by hand is left alone. Call off the
 * Minecraft server thread.
 */
public final class RankGrantStore {
    private static final String TABLE = "tdc_rank_grants";
    private static final int SCHEMA_VERSION = 6;

    private final DiscordJobStore jobs;

    public RankGrantStore(Main plugin) {
        this.jobs = new DiscordJobStore(plugin);
    }

    public void initialize() throws SQLException {
        try (Connection connection = jobs.connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                    + "guild_id VARCHAR(24) NOT NULL,"
                    + "channel_kind VARCHAR(16) NOT NULL,"
                    + "channel_id VARCHAR(24) NOT NULL,"
                    + "member_id VARCHAR(24) NOT NULL,"
                    + "granted_at BIGINT NOT NULL,"
                    + "PRIMARY KEY(guild_id,channel_id,member_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            statement.executeUpdate("INSERT IGNORE INTO tdc_schema_migrations "
                    + "(version,applied_at,description) VALUES "
                    + "(" + SCHEMA_VERSION + "," + System.currentTimeMillis() + ","
                    + "'Rank permissions granted on town and nation channels')");
        }
    }

    public Set<RankSlot> recorded(String guildId) throws SQLException {
        Set<RankSlot> slots = new HashSet<RankSlot>();
        try (Connection connection = jobs.connect(); PreparedStatement query = connection.prepareStatement(
                "SELECT channel_kind,channel_id,member_id FROM " + TABLE + " WHERE guild_id=?")) {
            query.setString(1, guildId);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    slots.add(new RankSlot(rows.getString(1), rows.getString(2), rows.getString(3)));
                }
            }
        }
        return slots;
    }

    public boolean isRecorded(String guildId, RankSlot slot) throws SQLException {
        try (Connection connection = jobs.connect(); PreparedStatement query = connection.prepareStatement(
                "SELECT 1 FROM " + TABLE + " WHERE guild_id=? AND channel_id=? AND member_id=?")) {
            query.setString(1, guildId);
            query.setString(2, slot.channelId());
            query.setString(3, slot.memberId());
            try (ResultSet rows = query.executeQuery()) {
                return rows.next();
            }
        }
    }

    public void record(String guildId, RankSlot slot) throws SQLException {
        try (Connection connection = jobs.connect(); PreparedStatement insert = connection.prepareStatement(
                "INSERT IGNORE INTO " + TABLE
                        + " (guild_id,channel_kind,channel_id,member_id,granted_at) VALUES (?,?,?,?,?)")) {
            insert.setString(1, guildId);
            insert.setString(2, slot.channelKind());
            insert.setString(3, slot.channelId());
            insert.setString(4, slot.memberId());
            insert.setLong(5, System.currentTimeMillis());
            insert.executeUpdate();
        }
    }

    public void forget(String guildId, RankSlot slot) throws SQLException {
        try (Connection connection = jobs.connect(); PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM " + TABLE + " WHERE guild_id=? AND channel_id=? AND member_id=?")) {
            delete.setString(1, guildId);
            delete.setString(2, slot.channelId());
            delete.setString(3, slot.memberId());
            delete.executeUpdate();
        }
    }
}
