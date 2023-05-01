package com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Voice.Nations.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.List;

public class Name extends SQL {

    public Name() {

    }

    public List<String> GetAllNationVoiceChannelNames() {
        return getAllEntries("nationVoiceChannelName", TABLE_NATION_VOICE_CHANNELS);
    }

    public List<String> GetAllNationVoiceChannelNames(String parentCategoryId) {
        return getAllEntriesWhereEqual("nationVoiceChannelName", "nationVoiceChannelParentCategoryId", parentCategoryId, TABLE_NATION_VOICE_CHANNELS);
    }

}