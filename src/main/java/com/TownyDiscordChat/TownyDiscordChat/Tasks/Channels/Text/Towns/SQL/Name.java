package com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Text.Towns.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.List;

public class Name extends SQL{

    public Name() {

    }

    public List<String> GetAllTownTextChannelNames() {
        return getAllEntries("townTextChannelName", TABLE_TOWN_TEXT_CHANNELS);
    }

    public List<String> GetAllTownTextChannelNames(String parentCategoryId) {
        return getAllEntriesWhereEqual("townTextChannelName", "townTextChannelParentCategoryId", parentCategoryId, TABLE_TOWN_TEXT_CHANNELS);
    }

}
