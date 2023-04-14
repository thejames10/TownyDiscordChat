package com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QueuedTask extends SQL {

    private final Main plugin;

    /***DatabaseTable***/
    public static final String TABLE = "tdc_queued_tasks";
    /***QueueType***/
    public static final String _PLAYER = "Player";
    public static final String _NATION = "Nation";
    public static final String _NATIONTEXTCHANNEL = "NationTextChannel";
    public static final String _NATIONVOICECHANNEL = "NationVoiceChannel";
    public static final String _TOWN = "Town";
    public static final String _TOWNTEXTCHANNEL = "TownTextChannel";
    public static final String _TOWNVOICECHANNEL = "TownVoiceChannel";
    /***Null***/
    public static final String _NULL = "null";

    public QueuedTask(Main plugin) {
        this.plugin = plugin;
    }

    public void createTable() {
        PreparedStatement ps;
        try {
            ps = plugin.SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE + " "
                    + "(Time VARCHAR(100),Type VARCHAR(100),UUID VARCHAR(100),discordUserId VARCHAR(100),isMayor VARCHAR(100),isKing VARCHAR(100),nationRoleName VARCHAR(100),nationRoleId VARCHAR(100),nationTextChannelName VARCHAR(100),nationTextChannelId VARCHAR(100),nationTextChannelParentCategoryName VARCHAR(100),nationTextChannelParentCategoryId VARCHAR(100),nationVoiceChannelName VARCHAR(100),nationVoiceChannelId VARCHAR(100),nationVoiceChannelParentCategoryName VARCHAR(100),nationVoiceChannelParentCategoryId VARCHAR(100),townRoleName VARCHAR(100),townRoleNameId VARCHAR(100),townTextChannelName VARCHAR(100),townTextChannelId VARCHAR(100),townTextChannelParentCategoryName VARCHAR(100),townTextChannelParentCategoryId VARCHAR(100),townVoiceChannelName VARCHAR(100),townVoiceChannelId VARCHAR(100),townVoiceChannelParentCategoryName VARCHAR(100),townVoiceChannelParentCategoryId VARCHAR(100),Expired VARCHAR(100),PRIMARY KEY (Time))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void queuePlayerTask (UUID UUID, String discordUserId,
                                 String townRoleName, String nationRoleName,
                                 String isMayor, String isKing, String doCleanUp) {
        createEntry(_PLAYER, UUID, discordUserId, isMayor, isKing, nationRoleName, _NULL, _NULL,
                _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, townRoleName,
                _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, doCleanUp);
    }

    public void queueNationTask (String nationRoleName, String nationRoleId, String doCleanUp) {
        createEntry(_NATION, _NULL, _NULL, _NULL, _NULL, nationRoleName, nationRoleId, _NULL,
                _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL,
                _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, doCleanUp);
    }

    public void queueNationTextChannelTask (String nationRoleName, String nationTextChannelName,
                                            String nationTextChannelId, String nationTextChannelParentCategoryName,
                                            String nationTextChannelParentCategoryId, String doCleanUp) {
        createEntry(_NATIONTEXTCHANNEL, _NULL, _NULL, _NULL, _NULL, nationRoleName, _NULL, nationTextChannelName,
                nationTextChannelId, nationTextChannelParentCategoryName, nationTextChannelParentCategoryId, _NULL,
                _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, doCleanUp);
    }

    public void queueNationVoiceChannelTask (String nationRoleName, String nationVoiceChannelName,
                                             String nationVoiceChannelId, String nationVoiceChannelParentCategoryName,
                                             String nationVoiceChannelParentCategoryId, String doCleanUp) {
        createEntry(_NATIONVOICECHANNEL, _NULL, _NULL, _NULL, _NULL, nationRoleName, _NULL, _NULL, _NULL,
                _NULL, _NULL, nationVoiceChannelName, nationVoiceChannelId, nationVoiceChannelParentCategoryName,
                nationVoiceChannelParentCategoryId, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, doCleanUp);
    }

    public void queueTownTask (String townRoleName, String townRoleId, String doCleanUp) {
        createEntry(_TOWN, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL,
                _NULL, _NULL, _NULL, _NULL, _NULL, townRoleName, townRoleId, _NULL,
                _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, doCleanUp);
    }

    public void queueTownTextChannelTask (String townRoleName, String townTextChannelName, String townTextChannelId,
                                          String townTextChannelParentCategoryName,
                                          String townTextChannelParentCategoryId, String doCleanUp) {
        createEntry(_TOWNTEXTCHANNEL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL,
                _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, townRoleName, _NULL, townTextChannelName,
                townTextChannelId, townTextChannelParentCategoryName, townTextChannelParentCategoryId,
                _NULL, _NULL, _NULL, _NULL, doCleanUp);
    }

    public void queueTownVoiceChannelTask (String townRoleName, String townVoiceChannelName, String townVoiceChannelId,
                                           String townVoiceChannelParentCategoryName,
                                           String townVoiceChannelParentCategoryId, String doCleanUp) {
        createEntry(_TOWNVOICECHANNEL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL, _NULL,
                _NULL, _NULL, _NULL, _NULL, townRoleName,_NULL, _NULL,_NULL, _NULL, _NULL, townVoiceChannelName,
                townVoiceChannelId, townVoiceChannelParentCategoryName, townVoiceChannelParentCategoryId, doCleanUp);
    }

    private void createEntry (String type, UUID UUID, String discordUserId, String isMayor, String isKing,
                             String nationRoleName, String nationRoleId, String nationTextChannelName, String nationTextChannelId,
                             String nationTextChannelParentCategoryName, String nationTextChannelParentCategoryId,
                             String nationVoiceChannelName, String nationVoiceChannelId, String nationVoiceChannelParentCategoryName,
                             String nationVoiceChannelParentCategoryId, String townRoleName, String townRoleId, String townTextChannelName,
                             String townTextChannelId, String townTextChannelParentCategoryName, String townTextChannelParentCategoryId,
                             String townVoiceChannelName, String townVoiceChannelId,
                             String townVoiceChannelParentCategoryName, String townVoiceChannelParentCategoryId, String doCleanUp) {
        createEntry(type, UUID.toString(), discordUserId, isMayor, isKing, nationRoleName, nationRoleId, nationTextChannelName,
                nationTextChannelId, nationTextChannelParentCategoryName, nationTextChannelParentCategoryId, nationVoiceChannelName,
                nationVoiceChannelId, nationVoiceChannelParentCategoryName, nationVoiceChannelParentCategoryId, townRoleName,
                townRoleId, townTextChannelName, townTextChannelId, townTextChannelParentCategoryName, townTextChannelParentCategoryId,
                townVoiceChannelName, townVoiceChannelId, townVoiceChannelParentCategoryName, townVoiceChannelParentCategoryId, doCleanUp);
    }

    public void createNationRole (String nationRoleName) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("Time", String.valueOf(DateTimeToLong()));
        keyPair.put("nationRoleName",nationRoleName);
        createEntry(nationRoleName, "nationRoleName", SQL.TABLE_QUEUED_TASKS, keyPair, true);
    }

    public void createTownRole(String townRoleName) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("Time", String.valueOf(DateTimeToLong()));
        keyPair.put("townRoleName",townRoleName);
        createEntry(townRoleName, "townRoleName", SQL.TABLE_QUEUED_TASKS, keyPair, true);
    }

    private void createEntry (String type, String UUID, String discordUserId, String isMayor, String isKing,
                             String nationRoleName, String nationRoleId, String nationTextChannelName, String nationTextChannelId,
                             String nationTextChannelParentCategoryName, String nationTextChannelParentCategoryId,
                             String nationVoiceChannelName, String nationVoiceChannelId, String nationVoiceChannelParentCategoryName,
                             String nationVoiceChannelParentCategoryId, String townRoleName, String townRoleId, String townTextChannelName,
                             String townTextChannelId, String townTextChannelParentCategoryName, String townTextChannelParentCategoryId,
                             String townVoiceChannelName, String townVoiceChannelId,
                             String townVoiceChannelParentCategoryName, String townVoiceChannelParentCategoryId, String doCleanUp) {
        try {
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT IGNORE INTO " + TABLE + " "
                        + "(Time,Type,UUID,discordUserId,isMayor,isKing,nationRoleName,nationRoleId,nationTextChannelName,nationTextChannelId,nationTextChannelParentCategoryName,nationTextChannelParentCategoryId,nationVoiceChannelName,nationVoiceChannelId,nationVoiceChannelParentCategoryName,nationVoiceChannelParentCategoryId,townRoleName,townRoleNameId,townTextChannelName,townTextChannelId,townTextChannelParentCategoryName,townTextChannelParentCategoryId,townVoiceChannelName,townVoiceChannelId,townVoiceChannelParentCategoryName,townVoiceChannelParentCategoryId,Expired) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                ps.setString(1, String.valueOf(DateTimeToLong()));
                ps.setString(2, type);
                ps.setString(3, UUID);
                ps.setString(4, discordUserId);
                ps.setString(5, isMayor);
                ps.setString(6, isKing);
                ps.setString(7, nationRoleName);
                ps.setString(8, nationRoleId);
                ps.setString(9, nationTextChannelName);
                ps.setString(10, nationTextChannelId);
                ps.setString(11, nationTextChannelParentCategoryName);
                ps.setString(12, nationTextChannelParentCategoryId);
                ps.setString(13, nationVoiceChannelName);
                ps.setString(14, nationVoiceChannelId);
                ps.setString(15, nationVoiceChannelParentCategoryName);
                ps.setString(16, nationVoiceChannelParentCategoryId);
                ps.setString(17, townRoleName);
                ps.setString(18, townRoleId);
                ps.setString(19, townTextChannelName);
                ps.setString(20, townTextChannelId);
                ps.setString(21, townTextChannelParentCategoryName);
                ps.setString(22, townTextChannelParentCategoryId);
                ps.setString(23, townVoiceChannelName);
                ps.setString(24, townVoiceChannelId);
                ps.setString(25, townVoiceChannelParentCategoryName);
                ps.setString(26, townVoiceChannelParentCategoryId);
                ps.setString(27, doCleanUp);
                ps.executeUpdate();
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
