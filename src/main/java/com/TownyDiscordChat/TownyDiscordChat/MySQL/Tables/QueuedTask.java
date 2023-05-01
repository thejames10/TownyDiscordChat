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
            ps = plugin.SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE_QUEUED_TASKS + " "
                    + "(Time VARCHAR(100),Type VARCHAR(100),UUID VARCHAR(100),discordUserId VARCHAR(100),isMayor VARCHAR(100),isKing VARCHAR(100),townCategoryName VARCHAR(100),townCategoryId VARCHAR(100),nationCategoryName VARCHAR(100),nationCategoryId VARCHAR(100),nationRoleName VARCHAR(100),nationRoleId VARCHAR(100),nationTextChannelName VARCHAR(100),nationTextChannelId VARCHAR(100),nationTextChannelParentCategoryName VARCHAR(100),nationTextChannelParentCategoryId VARCHAR(100),nationVoiceChannelName VARCHAR(100),nationVoiceChannelId VARCHAR(100),nationVoiceChannelParentCategoryName VARCHAR(100),nationVoiceChannelParentCategoryId VARCHAR(100),townRoleName VARCHAR(100),townRoleNameId VARCHAR(100),townTextChannelName VARCHAR(100),townTextChannelId VARCHAR(100),townTextChannelParentCategoryName VARCHAR(100),townTextChannelParentCategoryId VARCHAR(100),townVoiceChannelName VARCHAR(100),townVoiceChannelId VARCHAR(100),townVoiceChannelParentCategoryName VARCHAR(100),townVoiceChannelParentCategoryId VARCHAR(100),Expired VARCHAR(100),PRIMARY KEY (Time))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createTownRole(String townRoleName) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("Time", String.valueOf(DateTimeToLong()));
        keyPair.put("townRoleName",townRoleName);
        createEntry(townRoleName, "townRoleName", TABLE_QUEUED_TASKS, keyPair, true);
    }

    public void createNationRole (String nationRoleName) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("Time", String.valueOf(DateTimeToLong()));
        keyPair.put("nationRoleName",nationRoleName);
        createEntry(nationRoleName, "nationRoleName", TABLE_QUEUED_TASKS, keyPair, true);
    }

    public void createTownCategory (String townCategoryName) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("Time", String.valueOf(DateTimeToLong()));
        keyPair.put("townCategoryName",townCategoryName);
        createEntry(townCategoryName, "townCategoryName", TABLE_QUEUED_TASKS, keyPair, true);
    }

    public void createNationCategory (String nationCategoryName) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("Time", String.valueOf(DateTimeToLong()));
        keyPair.put("nationCategoryName",nationCategoryName);
        createEntry(nationCategoryName, "nationCategoryName", TABLE_QUEUED_TASKS, keyPair, true);
    }

    public void createTownTextChannel (String townTextChannelName) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("Time", String.valueOf(DateTimeToLong()));
        keyPair.put("townTextChannelName",townTextChannelName);
        createEntry(townTextChannelName, "townTextChannelName", TABLE_QUEUED_TASKS, keyPair, true);
    }

    public void createNationTextChannel (String nationTextChannelName) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("Time", String.valueOf(DateTimeToLong()));
        keyPair.put("nationTextChannelName",nationTextChannelName);
        createEntry(nationTextChannelName, "nationTextChannelName", TABLE_QUEUED_TASKS, keyPair, true);
    }

    public void createTownVoiceChannel (String townVoiceChannelName) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("Time", String.valueOf(DateTimeToLong()));
        keyPair.put("townVoiceChannelName",townVoiceChannelName);
        createEntry(townVoiceChannelName, "townVoiceChannelName", TABLE_QUEUED_TASKS, keyPair, true);
    }

    public void createNationVoiceChannel (String nationVoiceChannelName) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("Time", String.valueOf(DateTimeToLong()));
        keyPair.put("nationVoiceChannelName",nationVoiceChannelName);
        createEntry(nationVoiceChannelName, "nationVoiceChannelName", TABLE_QUEUED_TASKS, keyPair, true);
    }

    public void deleteEntry(String searchValue, String searchColName) {
        deleteEntry(searchValue, searchColName, TABLE_QUEUED_TASKS);
    }

    public boolean entryExists(String searchValue, String searchColName) {
        return entryExists(searchValue, searchColName, TABLE_QUEUED_TASKS);
    }

    public void updateEntry(String setValue, String setCol, String searchValue, String searchColName) {
        updateEntry(setValue, setCol, searchValue, searchColName, TABLE_QUEUED_TASKS);
    }

    public @Nullable String getEntry(String selectCol, String searchValue, String searchColName) {
        return getEntry(selectCol, searchValue, searchColName, TABLE_QUEUED_TASKS);
    }

    public @Nullable List<String> getAllColumnEntries(String selectCol) {
        return getAllColumnEntries(selectCol, TABLE_QUEUED_TASKS);
    }
}
