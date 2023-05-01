package com.TownyDiscordChat.TownyDiscordChat.Tasks.Roles.Nations.Towny;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;

import java.util.ArrayList;
import java.util.List;

public class Name {

    public static List<String> GetAllNationRoleNames () {
        List<String> Nations = new ArrayList<>();
        for (Nation nation : TownyUniverse.getInstance().getNations()) { Nations.add(nation.getName()); }
        return Nations;
    }
}