package com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Text.Nations.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.List;

public class Name extends SQL {

    public Name() {

    }

    public List<String> GetAllNationTextChannelNames() {
        return getAllEntries("nationTextChannelName", TABLE_NATION_TEXT_CHANNELS);
    }

    public List<String> GetAllNationTextChannelNames(String parentCategoryId) {
        return getAllEntriesWhereEqual("nationTextChannelName", "nationTextChannelParentCategoryId", parentCategoryId, TABLE_NATION_TEXT_CHANNELS);
    }

}