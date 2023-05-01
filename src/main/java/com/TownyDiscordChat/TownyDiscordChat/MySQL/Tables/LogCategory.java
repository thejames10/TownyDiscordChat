package com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class LogCategory extends SQL {

    private final Main plugin;

    public LogCategory(Main plugin) {
        this.plugin = plugin;
    }

    public void createTable() {
        PreparedStatement ps;
        try {
            ps = plugin.SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE_LOG_CATEGORY + " "
                    + "(logCategoryName VARCHAR(100),logCategoryId VARCHAR(100),Expired VARCHAR(100),PRIMARY KEY (logCategoryId))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createEntry(String logCategoryName, String logCategoryId) {
        try {
            if (!entryExists(logCategoryId, "townCategoryId")) {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT IGNORE INTO " + TABLE_LOG_CATEGORY + " "
                        + "(logCategoryName,logCategoryId,Expired) VALUES (?,?,?)");
                ps.setString(1, logCategoryName);
                ps.setString(2, logCategoryId);
                ps.setString(3, "false");
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteEntry(String searchValue, String searchColName) {
        deleteEntry(searchValue, searchColName, TABLE_LOG_CATEGORY);
    }

    public boolean entryExists(String searchValue, String searchColName) {
        return entryExists(searchValue, searchColName, TABLE_LOG_CATEGORY);
    }

    public void updateEntry(String setValue, String setCol, String searchValue, String searchColName) {
        updateEntry(setValue, setCol, searchValue, searchColName, TABLE_LOG_CATEGORY);
    }

    public @Nullable String getEntry(String selectCol, String searchValue, String searchColName) {
        return getEntry(selectCol, searchValue, searchColName, TABLE_LOG_CATEGORY);
    }

    public @Nullable List<String> getAllColumnEntries(String selectCol) {
        return getAllColumnEntries(selectCol, TABLE_LOG_CATEGORY);
    }
}