package com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQLGetter;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Towns implements SQLGetter {

    private final Main plugin;

    private final String TABLE = "towns";

    public Towns(Main plugin) {
        this.plugin = plugin;
    }

    public void createTable() {
        PreparedStatement ps;
        try {
            ps = plugin.SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE + " "
                    + "(townRoleName VARCHAR(100),townRoleId VARCHAR(100),doCleanUp VARCHAR(100),PRIMARY KEY (townRoleName))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createEntry(String townRoleName, String townRoleId) {
        try {
            if (!entryExists(townRoleName, "townRoleName")) {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT IGNORE INTO " + TABLE + " "
                        + "(townRoleName,townRoleId,doCleanUp) VALUES (?,?,?)");
                ps.setString(1, townRoleName);
                ps.setString(2, townRoleId);
                ps.setString(3, "false");
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