package com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class NationRoles extends SQL {

    private final Main plugin;

    public NationRoles(Main plugin) {
        this.plugin = plugin;
    }

    public void createTable() {
        PreparedStatement ps;
        try {
            ps = plugin.SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE_NATION_ROLES + " "
                    + "(nationRoleName VARCHAR(100),nationRoleId VARCHAR(100), Expired VARCHAR(100),PRIMARY KEY (nationRoleName))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createEntry(String nationRoleName, String nationRoleId) {
        try {
            if (!entryExists(nationRoleName, "nationRoleName")) {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT IGNORE INTO " + TABLE_NATION_ROLES + " "
                        + "(nationRoleName,nationRoleId,Expired) VALUES (?,?,?)");
                ps.setString(1, nationRoleName);
                ps.setString(2, nationRoleId);
                ps.setString(3, "false");
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteEntry(String searchValue, String searchColName) {
        deleteEntry(searchValue, searchColName, TABLE_NATION_ROLES);
    }

    public boolean entryExists(String searchValue, String searchColName) {
        return entryExists(searchValue, searchColName, TABLE_NATION_ROLES);
    }

    public void updateEntry(String setValue, String setCol, String searchValue, String searchColName) {
        updateEntry(setValue, setCol, searchValue, searchColName, TABLE_NATION_ROLES);
    }

    public @Nullable String getEntry(String selectCol, String searchValue, String searchColName) {
        return getEntry(selectCol, searchValue, searchColName, TABLE_NATION_ROLES);
    }

    public @Nullable List<String> getAllColumnEntries(String selectCol) {
        return getAllColumnEntries(selectCol, TABLE_NATION_ROLES);
    }
}