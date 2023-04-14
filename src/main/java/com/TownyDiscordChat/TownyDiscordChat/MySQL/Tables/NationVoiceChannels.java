package com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class NationVoiceChannels extends SQL {

    private final Main plugin;

    private final String TABLE = "tdc_nation_voice_channels";

    public NationVoiceChannels(Main plugin) {
        this.plugin = plugin;
    }

    public void createTable() {
        PreparedStatement ps;
        try {
            ps = plugin.SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE + " "
                    + "(nationRoleName VARCHAR(100),nationVoiceChannelName VARCHAR(100),nationVoiceChannelId VARCHAR(100), nationVoiceChannelParentCategoryName VARCHAR(100), nationVoiceChannelParentCategoryId VARCHAR(100), Expired VARCHAR(100),PRIMARY KEY (nationRoleName))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createEntry(String nationRoleName, String nationVoiceChannelName, String nationVoiceChannelId,
                            String nationVoiceChannelParentCategoryName, String nationVoiceChannelParentCategoryId) {
        try {
            if (!entryExists(nationRoleName, "townRoleName")) {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT IGNORE INTO " + TABLE + " "
                        + "(nationRoleName,nationVoiceChannelName,nationVoiceChannelId,nationVoiceChannelParentCategoryName,nationVoiceChannelParentCategoryId,Expired) VALUES (?,?,?,?,?,?)");
                ps.setString(1, nationRoleName);
                ps.setString(2, nationVoiceChannelName);
                ps.setString(3, nationVoiceChannelId);
                ps.setString(4, nationVoiceChannelParentCategoryName);
                ps.setString(5, nationVoiceChannelParentCategoryId);
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

    public @Nullable List<String> getAllColumnEntries(String selectCol) {
        return getAllColumnEntries(selectCol, TABLE);
    }
}