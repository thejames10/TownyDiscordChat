package com.TownyDiscordChat.TownyDiscordChat.Tasks.Roles.Towns.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Name extends SQL {

    public static List<String> GetAllTownRoleNames() {
        return getAllEntries("townRoleName", SQL.TABLE_TOWN_ROLES);
    }
}
