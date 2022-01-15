package com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces;
import org.jetbrains.annotations.Nullable;

public interface SQLGetter {

    public void createTable();

    //public void createEntry(String UUID, String discordUserId, String townRoleName, String townRoleId, String townTextChannelId, String townVoiceChannelId, String nationTextChannelId, String nationVoiceChannelId);

    public void deleteEntry(String searchValue, String searchColName);

    public boolean entryExists(String searchValue, String searchColName);

    public void updateEntry(String setValue, String setCol, String searchValue, String searchColName);

    public @Nullable String getEntry(String selectCol, String searchValue, String searchColName);

}
