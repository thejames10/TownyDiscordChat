package com.TownyDiscordChat.TownyDiscordChat.Tasks.Categories.Nation.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.List;

public class Id extends SQL {

    public Id () {

    }

    public List<String> GetAllCategoryIds() {
        return getAllEntries("nationCategoryId", TABLE_NATION_CATEGORIES);
    }
}