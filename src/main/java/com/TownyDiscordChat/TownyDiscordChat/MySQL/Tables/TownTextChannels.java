package com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class TownTextChannels extends SQL {

    private final Main plugin;

    public TownTextChannels(Main plugin) {
        this.plugin = plugin;
    }

    public void createTable() {
        PreparedStatement ps;
        try {
            ps = plugin.SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE_TOWN_TEXT_CHANNELS + " "
                    + "(townRoleName VARCHAR(100),townTextChannelName VARCHAR(100),townTextChannelId VARCHAR(100), townTextChannelParentCategoryName VARCHAR(100), townTextChannelParentCategoryId VARCHAR(100), Expired VARCHAR(100),PRIMARY KEY (townRoleName))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createEntry(String townRoleName, String townTextChannelName, String townTextChannelId,
                            String townTextChannelParentCategoryName, String townTextChannelParentCategoryId) {
        try {
            if (!entryExists(townRoleName, "townRoleName")) {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT IGNORE INTO " + TABLE_TOWN_TEXT_CHANNELS + " "
                        + "(townRoleName,townTextChannelName,townTextChannelId,townTextChannelParentCategoryName,townTextChannelParentCategoryId,Expired) VALUES (?,?,?,?,?,?)");
                ps.setString(1, townRoleName);
                ps.setString(2, townTextChannelName);
                ps.setString(3, townTextChannelId);
                ps.setString(4, townTextChannelParentCategoryName);
                ps.setString(5, townTextChannelParentCategoryId);
                ps.setString(6, "false");
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteEntry(String searchValue, String searchColName) {
        deleteEntry(searchValue, searchColName, TABLE_TOWN_TEXT_CHANNELS);
    }

    public boolean entryExists(String searchValue, String searchColName) {
        return entryExists(searchValue, searchColName, TABLE_TOWN_TEXT_CHANNELS);
    }

    public void updateEntry(String setValue, String setCol, String searchValue, String searchColName) {
        updateEntry(setValue, setCol, searchValue, searchColName, TABLE_TOWN_TEXT_CHANNELS);
    }

    public @Nullable String getEntry(String selectCol, String searchValue, String searchColName) {
        return  getEntry(selectCol, searchValue, searchColName, TABLE_TOWN_TEXT_CHANNELS);
    }

    public @Nullable List<String> getAllColumnEntries(String selectCol) {
        return getAllColumnEntries(selectCol, TABLE_TOWN_TEXT_CHANNELS);
    }
}