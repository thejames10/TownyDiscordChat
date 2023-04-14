package com.TownyDiscordChat.TownyDiscordChat.Tasks.Towns.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Name extends SQL {

    public Name () {

    }

    public List<String> GetAllTowns() {
        return getAllEntries("townRoleName", SQL.TABLE_TOWNS);
    }

    public void SetName (String townRoleName) {
        AddName(townRoleName);
    }

    public void AddName(String townRoleName) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("townRoleName",townRoleName);
        createEntry(townRoleName, "townRoleName", SQL.TABLE_TOWNS, keyPair, false);
    }
}
