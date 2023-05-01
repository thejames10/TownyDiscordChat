package com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class LogTextChannel extends SQL {

    private final Main plugin;

    public LogTextChannel(Main plugin) {
        this.plugin = plugin;
    }

    public void createTable() {
        PreparedStatement ps;
        try {
            ps = plugin.SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE_LOG_TEXT_CHANNEL + " "
                    + "(logTextChannelName VARCHAR(100),logTextChannelId VARCHAR(100),Expired VARCHAR(100),PRIMARY KEY (logTextChannelId))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createEntry(String logTextChannelName, String logTextChannelId) {
        try {
            if (!entryExists(logTextChannelId, "logTextChannelId")) {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT IGNORE INTO " + TABLE_LOG_TEXT_CHANNEL + " "
                        + "(logTextChannelName,logTextChannelId,Expired) VALUES (?,?,?)");
                ps.setString(1, logTextChannelName);
                ps.setString(2, logTextChannelId);
                ps.setString(3, "false");
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteEntry(String searchValue, String searchColName) {
        deleteEntry(searchValue, searchColName, TABLE_LOG_TEXT_CHANNEL);
    }

    public boolean entryExists(String searchValue, String searchColName) {
        return entryExists(searchValue, searchColName, TABLE_LOG_TEXT_CHANNEL);
    }

    public void updateEntry(String setValue, String setCol, String searchValue, String searchColName) {
        updateEntry(setValue, setCol, searchValue, searchColName, TABLE_LOG_TEXT_CHANNEL);
    }

    public @Nullable String getEntry(String selectCol, String searchValue, String searchColName) {
        return getEntry(selectCol, searchValue, searchColName, TABLE_LOG_TEXT_CHANNEL);
    }

    public @Nullable List<String> getAllColumnEntries(String selectCol) {
        return getAllColumnEntries(selectCol, TABLE_LOG_TEXT_CHANNEL);
    }
}