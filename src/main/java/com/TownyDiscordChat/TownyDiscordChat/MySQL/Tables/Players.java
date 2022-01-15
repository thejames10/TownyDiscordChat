package com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQLGetter;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class Players implements SQLGetter {

    /*private static final BiMap<String, Integer> biMap = HashBiMap.create();
    biMap.put("UUID", 1);
    biMap.put("discordUserId", 2);
    biMap.put("townRoleName", 3);
    biMap.put("townRoleId", 4);
    biMap.put("townTextChannelId", 5);
    biMap.put("townVoiceChannelId", 6);
    biMap.put("nationTextChannelId", 7);
    biMap.put("nationVoiceChannelId", 8);

    private Integer getIndex (String colName) { return biMap.get(colName); }
    private String getColName (int index) { return biMap.inverse().get(index); }*/

    private final Main plugin;

    private final String TABLE = "players";

    public Players(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void createTable() {
        PreparedStatement ps;
        try {
            ps = plugin.SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE + " "
                    + "(UUID VARCHAR(100),discordUserId VARCHAR(100),townRoleName VARCHAR(100),townRoleId VARCHAR(100),townTextChannelId VARCHAR(100),townVoiceChannelId VARCHAR(100),nationTextChannelId VARCHAR(100),nationVoiceChannelId VARCHAR(100),isMayor VARCHAR(100),doCleanUp VARCHAR(100),PRIMARY KEY (UUID))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createEntry (UUID UUID, String discordUserId, String townRoleName,
                             String townRoleId, String townTextChannelId, String townVoiceChannelId,
                             String nationTextChannelId, String nationVoiceChannelId, String isMayor, String isKing) {
        createEntry(UUID.toString(), discordUserId, townRoleName, townRoleId, townTextChannelId,
                townVoiceChannelId, nationTextChannelId, nationVoiceChannelId, isMayor, isKing);
    }

    public void createEntry(String UUID, String discordUserId, String townRoleName,
                            String townRoleId, String townTextChannelId, String townVoiceChannelId,
                            String nationTextChannelId, String nationVoiceChannelId, String isMayor, String isKing) {
        try {
            if (!entryExists(discordUserId, "discordUserId")) {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT IGNORE INTO " + TABLE + " "
                        + "(UUID,discordUserId,townRoleName,townRoleId,townTextChannelId,townVoiceChannelId,nationTextChannelId,nationVoiceChannelId,isMayor,isKing,doCleanUp) VALUES (?,?,?,?,?,?,?,?,?,?,?)");
                ps.setString(1, UUID);
                ps.setString(2, discordUserId);
                ps.setString(3, townRoleName);
                ps.setString(4, townRoleId);
                ps.setString(5, townTextChannelId);
                ps.setString(6, townVoiceChannelId);
                ps.setString(7, nationTextChannelId);
                ps.setString(8, nationVoiceChannelId);
                ps.setString(9, isMayor);
                ps.setString(10, isKing);
                ps.setString(10, "false");
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteEntry(String searchValue, String searchColName) {
        if (entryExists(searchValue, searchColName)) {
            try {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("DELETE FROM " + TABLE + " "
                        + "WHERE " + searchColName + "=?");
                ps.setString(1, searchValue);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean entryExists(String searchValue, String searchColName) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("SELECT * FROM " + TABLE + " WHERE " + searchColName + "=?");
            ps.setString(1, searchValue);

            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                // entry is found
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void updateEntry(String setValue, String setCol, String searchValue, String searchColName) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("UPDATE " + TABLE + " SET " + setCol + "=? WHERE " + searchColName + "=?");
            ps.setString(1, setValue);
            ps.setString(2, searchValue);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @Nullable String getEntry(String selectCol, String searchValue, String searchColName) {
        if (entryExists(searchValue, searchColName)) {
            try {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("SELECT " + selectCol + " FROM " + TABLE + " WHERE " + searchColName + "=?");
                ps.setString(1, searchValue);

                ResultSet resultSet = ps.executeQuery();
                if (resultSet.next()) { return resultSet.getString(1); };
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
