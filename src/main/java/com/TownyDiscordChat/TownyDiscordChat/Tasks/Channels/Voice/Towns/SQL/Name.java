package com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Voice.Towns.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.List;

public class Name extends SQL {

    public Name() {

    }

    public List<String> GetAllTownVoiceChannelNames() {
        return getAllEntries("townVoiceChannelName", TABLE_TOWN_VOICE_CHANNELS);
    }

    public List<String> GetAllTownVoiceChannelNames(String parentCategoryId) {
        return getAllEntriesWhereEqual("townVoiceChannelName", "townVoiceChannelParentCategoryId", parentCategoryId, TABLE_TOWN_VOICE_CHANNELS);
    }

}