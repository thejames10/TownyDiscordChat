package com.TownyDiscordChat.TownyDiscordChat.Tasks.Nations.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.HashMap;
import java.util.Map;

public class Expired extends SQL {

    public Expired() {

    }

    public void SetExpired(String nationRoleName) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("nationRoleNme",nationRoleName);
        createEntry(nationRoleName, "nationRoleName", SQL.TABLE_NATIONS, keyPair, false);
    }
}