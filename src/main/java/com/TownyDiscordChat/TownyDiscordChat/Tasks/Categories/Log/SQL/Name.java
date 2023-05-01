package com.TownyDiscordChat.TownyDiscordChat.Tasks.Categories.Log.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Name extends SQL {

    public Name () {

    }

    public List<String> GetAllNations() {
        return getAllEntries("nationCategoryName", TABLE_NATION_CATEGORIES);
    }

    public void SetName (String nationCategoryName) {
        AddName(nationCategoryName);
    }

    public void AddName(String nationCategoryName) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("nationCategoryName",nationCategoryName);
        createEntry(nationCategoryName, "nationCategoryName", TABLE_NATION_CATEGORIES, keyPair, false);
    }
}