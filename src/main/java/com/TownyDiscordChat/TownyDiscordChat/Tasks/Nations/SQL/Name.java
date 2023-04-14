package com.TownyDiscordChat.TownyDiscordChat.Tasks.Nations.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Name extends SQL {

    public Name () {

    }

    public List<String> GetAllNations() {
        return getAllEntries("nationRoleName", SQL.TABLE_NATIONS);
    }

    public void SetName (String nationRoleName) {
        AddName(nationRoleName);
    }

    public void AddName(String nationRoleName) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("nationRoleName",nationRoleName);
        createEntry(nationRoleName, "nationRoleName", SQL.TABLE_NATIONS, keyPair, false);
    }
}
