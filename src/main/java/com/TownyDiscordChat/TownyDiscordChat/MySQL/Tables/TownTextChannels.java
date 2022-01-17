package com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQLGetter;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TownTextChannels extends SQLGetter {

    private final Main plugin;

    private final String TABLE = "town_text_channels";

    public TownTextChannels(Main plugin) {
        this.plugin = plugin;
    }

    public void createTable() {
        PreparedStatement ps;
        try {
            ps = plugin.SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE + " "
                    + "(townRoleName VARCHAR(100),townTextChannelName VARCHAR(100),townTextChannelId VARCHAR(100), townTextChannelParentCategoryName VARCHAR(100), townTextChannelParentCategoryId VARCHAR(100), doCleanUp VARCHAR(100),PRIMARY KEY (townRoleName))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createEntry(String townRoleName, String townTextChannelName, String townTextChannelId,
                            String townTextChannelParentCategoryName, String townTextChannelParentCategoryId) {
        try {
            if (!entryExists(townRoleName, "townRoleName")) {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT IGNORE INTO " + TABLE + " "
                        + "(townRoleName,townTextChannelName,townTextChannelId,townTextChannelParentCategoryName,townTextChannelParentCategoryId,doCleanUp) VALUES (?,?,?,?,?,?)");
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
        deleteEntry(searchValue, searchColName, TABLE);
    }

    public boolean entryExists(String searchValue, String searchColName) {
        return entryExists(searchValue, searchColName, TABLE);
    }

    public void updateEntry(String setValue, String setCol, String searchValue, String searchColName) {
        updateEntry(setValue, setCol, searchValue, searchColName, TABLE);
    }

    public @Nullable String getEntry(String selectCol, String searchValue, String searchColName) {
        return  getEntry(selectCol, searchValue, searchColName, TABLE);
    }
}