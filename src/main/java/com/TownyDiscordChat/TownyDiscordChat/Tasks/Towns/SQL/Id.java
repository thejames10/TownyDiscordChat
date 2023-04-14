package com.TownyDiscordChat.TownyDiscordChat.Tasks.Towns.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.HashMap;
import java.util.Map;

public class Id extends SQL {

    public Id () {

    }

    public void SetId(String townRoleId) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("townRoleId",townRoleId);
        createEntry(townRoleId, "townRoleId", SQL.TABLE_TOWNS, keyPair, false);
    }
}
