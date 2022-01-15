package com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQLGetter;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Nations implements SQLGetter {

    private final Main plugin;

    private final String TABLE = "nations";

    public Nations(Main plugin) {
        this.plugin = plugin;
    }

    public void createTable() {
        PreparedStatement ps;
        try {
            ps = plugin.SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE + " "
                    + "(nationRoleName VARCHAR(100),nationRoleId VARCHAR(100), doCleanUp VARCHAR(100),PRIMARY KEY (nationRoleName))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createEntry(String nationRoleName, String nationRoleId) {
        try {
            if (!entryExists(nationRoleName, "nationRoleName")) {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT IGNORE INTO " + TABLE + " "
                        + "(nationRoleName,nationRoleId,doCleanUp) VALUES (?,?,?)");
                ps.setString(1, nationRoleName);
                ps.setString(2, nationRoleId);
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