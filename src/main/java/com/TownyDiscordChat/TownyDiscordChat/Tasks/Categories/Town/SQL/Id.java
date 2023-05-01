package com.TownyDiscordChat.TownyDiscordChat.Tasks.Categories.Town.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.List;

public class Id extends SQL {

    public Id () {

    }

    public List<String> GetAllCategoryIds() {
        return getAllEntries("townCategoryId", TABLE_TOWN_CATEGORIES);
    }
}
