package com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class TownRoles extends SQL {

    private final Main plugin;

    public TownRoles(Main plugin) {
        this.plugin = plugin;
    }

    public void createTable() {
        PreparedStatement ps;
        try {
            ps = plugin.SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE_TOWN_ROLES + " "
                    + "(townRoleName VARCHAR(100),townRoleId VARCHAR(100),Expired VARCHAR(100),PRIMARY KEY (townRoleName))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createEntry(String townRoleName, String townRoleId) {
        try {
            if (!entryExists(townRoleName, "townRoleName")) {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT IGNORE INTO " + TABLE_TOWN_ROLES + " "
                        + "(townRoleName,townRoleId,Expired) VALUES (?,?,?)");
                ps.setString(1, townRoleName);
                ps.setString(2, townRoleId);
                ps.setString(3, "false");
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteEntry(String searchValue, String searchColName) {
        deleteEntry(searchValue, searchColName, TABLE_TOWN_ROLES);
    }

    public boolean entryExists(String searchValue, String searchColName) {
        return entryExists(searchValue, searchColName, TABLE_TOWN_ROLES);
    }

    public void updateEntry(String setValue, String setCol, String searchValue, String searchColName) {
        updateEntry(setValue, setCol, searchValue, searchColName, TABLE_TOWN_ROLES);
    }

    public @Nullable String getEntry(String selectCol, String searchValue, String searchColName) {
        return getEntry(selectCol, searchValue, searchColName, TABLE_TOWN_ROLES);
    }

    public @Nullable List<String> getAllColumnEntries(String selectCol) {
        return getAllColumnEntries(selectCol, TABLE_TOWN_ROLES);
    }
}