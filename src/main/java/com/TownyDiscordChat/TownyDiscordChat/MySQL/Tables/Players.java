package com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class Players extends SQL {

    private final Main plugin;

    public static final String TABLE = "tdc_players";

    public Players(Main plugin) {
        this.plugin = plugin;
    }

    public void createTable() {
        PreparedStatement ps;
        try {
            ps = plugin.SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE + " "
                    + "(UUID VARCHAR(100),discordUserId VARCHAR(100),townRoleName VARCHAR(100),nationRoleName VARCHAR(100),isMayor VARCHAR(100),isKing VARCHAR(100),Expired VARCHAR(100),PRIMARY KEY (UUID))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createEntry (UUID UUID, String discordUserId, String townRoleName,
                             String nationRoleName, String isMayor, String isKing) {
        createEntry(UUID.toString(), discordUserId, townRoleName, nationRoleName, isMayor, isKing);
    }

    public void createEntry(String UUID, String discordUserId, String townRoleName,
                            String nationRoleName, String isMayor, String isKing) {
        try {
            if (!entryExists(discordUserId, "discordUserId")) {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT IGNORE INTO " + TABLE + " "
                        + "(UUID,discordUserId,townRoleName,nationRoleName,isMayor,isKing,Expired) VALUES (?,?,?,?,?,?,?)");
                ps.setString(1, UUID);
                ps.setString(2, discordUserId);
                ps.setString(3, townRoleName);
                ps.setString(4, nationRoleName);
                ps.setString(5, isMayor);
                ps.setString(6, isKing);
                ps.setString(7, "false");
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
        return getEntry(selectCol, searchValue, searchColName, TABLE);
    }

    public @Nullable List<String> getAllColumnEntries(String selectCol) {
        return getAllColumnEntries(selectCol, TABLE);
    }
}
