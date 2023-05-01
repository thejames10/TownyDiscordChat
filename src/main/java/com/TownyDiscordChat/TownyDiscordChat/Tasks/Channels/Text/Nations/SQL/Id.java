package com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Text.Nations.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.List;

public class Id extends SQL {

    public Id () {

    }

    public List<String> GetAllTextChannelParentCategoryIds(String Id) {
        return getAllEntries("nationTextChannelName", TABLE_NATION_TEXT_CHANNELS);
    }
}
