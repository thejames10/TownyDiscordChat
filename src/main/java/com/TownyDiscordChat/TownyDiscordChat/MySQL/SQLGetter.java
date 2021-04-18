package com.TownyDiscordChat.TownyDiscordChat.MySQL;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLGetter {

    private Main plugin;

    private final String TABLE = "banlist";

    public SQLGetter(Main plugin) {
        this.plugin = plugin;
    }
    // discordId
    // TextChannelId
    // RoleId

    public void createTable() {
        PreparedStatement ps;
        try {
            ps = plugin.SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE + " "
            + "(DiscordId VARCHAR(100),RoleId VARCHAR(100),TextChannelId VARCHAR(100),PRIMARY KEY (discordId))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createEntry(String discordId, String roleId, String textChannelId) {
        try {
            if (!entryExists(discordId)) {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT IGNORE INTO " + TABLE + " "
                + "(DiscordId,RoleId,TextChannelId) VALUES (?,?,?)");
                ps.setString(1, discordId);
                ps.setString(2, roleId);
                ps.setString(3, textChannelId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteEntry(String discordId) {
        if (entryExists(discordId)) {
            try {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("DELETE FROM " + TABLE + " "
                        + "WHERE DiscordId=?");
                ps.setString(1, discordId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean entryExists(String discordId) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("SELECT * FROM " + TABLE + " WHERE DiscordId=?");
            ps.setString(1, discordId);

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

    public void updateRoleId (String discordId, String roleId) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("UPDATE " + TABLE + " SET RoleId=? WHERE DiscordId=?");
            ps.setString(1, roleId);
            ps.setString(2, discordId);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateTextChannelId (String discordId, String textChannelId) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("UPDATE " + TABLE + " SET TextChannelId=? WHERE DiscordId=?");
            ps.setString(1, textChannelId);
            ps.setString(2, discordId);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public @Nullable String getRoleId (String discordId) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("SELECT RoleId FROM " + TABLE + " WHERE DiscordId=?");
            ps.setString(1, discordId);

            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("RoleId");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public @Nullable String getTextChannelId (String discordId) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("SELECT TextChannelId FROM " + TABLE + " WHERE DiscordId=?");
            ps.setString(1, discordId);

            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("TextChannelId");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
