package com.TownyDiscordChat.TownyDiscordChat.Tasks.Nations.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.HashMap;
import java.util.Map;

public class Id extends SQL {

    public Id () {

    }

    public void SetId(String nationRoleId) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("nationRoleId",nationRoleId);
        createEntry(nationRoleId, "nationRoleId", SQL.TABLE_NATIONS, keyPair, false);
    }
}
