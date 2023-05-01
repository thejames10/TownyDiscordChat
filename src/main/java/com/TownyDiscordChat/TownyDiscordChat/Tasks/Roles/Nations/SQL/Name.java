package com.TownyDiscordChat.TownyDiscordChat.Tasks.Roles.Nations.SQL;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Name extends SQL {

    public static List<String> GetAllNationRoleNames() { return getAllEntries("nationRoleName", SQL.TABLE_NATION_ROLES); }
}