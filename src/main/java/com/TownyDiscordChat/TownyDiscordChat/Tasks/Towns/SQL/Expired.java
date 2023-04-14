package com.TownyDiscordChat.TownyDiscordChat.Tasks.Towns.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.HashMap;
import java.util.Map;

public class Expired extends SQL {

    public Expired() {

    }

    public void SetExpired(String townRoleName) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("townRoleNme",townRoleName);
        createEntry(townRoleName, "townRoleName", SQL.TABLE_TOWNS, keyPair, false);
    }
}
