package com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Text.Towns.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.List;

public class Id extends SQL {

    public Id () {

    }

    public List<String> GetAllTextChannelParentCategoryIds(String Id) {
        return getAllEntries("townTextChannelName", TABLE_TOWN_TEXT_CHANNELS);
    }
}
