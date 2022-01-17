package com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.TownyDiscordChat.TownyDiscordChat.Main.plugin;

public class SQLGetter {

    public void deleteEntry(String searchValue, String searchColName, String TABLE) {
        if (entryExists(searchValue, searchColName, TABLE)) {
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

    public boolean entryExists(String searchValue, String searchColName, String TABLE) {
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

    public void updateEntry(String setValue, String setCol, String searchValue, String searchColName, String TABLE) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("UPDATE " + TABLE + " SET " + setCol + "=? WHERE " + searchColName + "=?");
            ps.setString(1, setValue);
            ps.setString(2, searchValue);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public @Nullable String getEntry(String selectCol, String searchValue, String searchColName, String TABLE) {
        if (entryExists(searchValue, searchColName, TABLE)) {
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
