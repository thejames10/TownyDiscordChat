package com.TownyDiscordChat.TownyDiscordChat.Tasks.Categories.Town.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.List;

public class Name extends SQL {

    public Name () {

    }

    public List<String> GetAllCategoryNames() {
        return getAllEntries("townCategoryName", TABLE_TOWN_CATEGORIES);
    }
}
