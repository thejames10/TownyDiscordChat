package com.TownyDiscordChat.TownyDiscordChat.Tasks.Roles.Towns.Towny;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;

import java.util.ArrayList;
import java.util.List;

public class Name {

    public static List<String> GetAllTownRoleNames () {
        List<String> Towns = new ArrayList<>();
        for (Town town : TownyUniverse.getInstance().getTowns()) { Towns.add(town.getName()); }
        return Towns;
    }
}
