package com.TownyDiscordChat.TownyDiscordChat.MySQL;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class SQLGetter {

    private final Main plugin;

    private final String TABLE = "players";

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
                    + "(UUID VARCHAR(100),discordUserId VARCHAR(100),townRoleName VARCHAR(100),townRoleId VARCHAR(100),townTextChannelId VARCHAR(100),townVoiceChannelId VARCHAR(100),nationTextChannelId VARCHAR(100),nationVoiceChannelId VARCHAR(100),PRIMARY KEY (discordId))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createEntry (UUID UUID, String discordUserId, String townRoleName,
                             String townRoleId, String townTextChannelId, String townVoiceChannelId,
                             String nationTextChannelId, String nationVoiceChannelId) {
        createEntry(UUID.toString(), discordUserId, townRoleName, townRoleId, townTextChannelId,
                townVoiceChannelId, nationTextChannelId, nationVoiceChannelId);
    }

    public void createEntry(String UUID, String discordUserId, String townRoleName,
                            String townRoleId, String townTextChannelId, String townVoiceChannelId,
                            String nationTextChannelId, String nationVoiceChannelId) {
        try {
            if (!entryExists(discordUserId)) {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT IGNORE INTO " + TABLE + " "
                        + "(UUID,discordUserId,townRoleName,townRoleId,townTextChannelId,townVoiceChannelId,nationTextChannelId,nationVoiceChannelId) VALUES (?,?,?,?,?,?,?,?)");
                ps.setString(1, UUID);
                ps.setString(2, discordUserId);
                ps.setString(3, townRoleName);
                ps.setString(4, townRoleId);
                ps.setString(5, townTextChannelId);
                ps.setString(6, townVoiceChannelId);
                ps.setString(7, nationTextChannelId);
                ps.setString(8, nationVoiceChannelId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteEntry(String discordUserId) {
        if (entryExists(discordUserId)) {
            try {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("DELETE FROM " + TABLE + " "
                        + "WHERE discordUserId=?");
                ps.setString(1, discordUserId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean entryExists(String discordUserId) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("SELECT * FROM " + TABLE + " WHERE discordUserId=?");
            ps.setString(1, discordUserId);

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

    public void updateRoleId (String discordUserId, String roleId) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("UPDATE " + TABLE + " SET RoleId=? WHERE DiscordId=?");
            ps.setString(1, roleId);
            ps.setString(2, discordUserId);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateTextChannelId (String discordUserId, String textChannelId) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("UPDATE " + TABLE + " SET TextChannelId=? WHERE DiscordId=?");
            ps.setString(1, textChannelId);
            ps.setString(2, discordUserId);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public @Nullable String getRoleId (String discordUserId) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("SELECT RoleId FROM " + TABLE + " WHERE DiscordId=?");
            ps.setString(1, discordUserId);

            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("RoleId");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public @Nullable String getTextChannelId (String discordUserId) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("SELECT TextChannelId FROM " + TABLE + " WHERE DiscordId=?");
            ps.setString(1, discordUserId);

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
