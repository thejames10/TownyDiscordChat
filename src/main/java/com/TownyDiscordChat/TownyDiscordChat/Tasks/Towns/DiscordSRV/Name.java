package com.TownyDiscordChat.TownyDiscordChat.Tasks.Towns.DiscordSRV;

import com.TownyDiscordChat.TownyDiscordChat.Messages.TDCMessages;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Name extends SQL {

    public Name () {

    }

    public List<String> GetAllTowns() {
        List<String> Towns = new ArrayList<>();
        for (Role town : DiscordSRV.getPlugin().getMainGuild().getRoles()) {
            if (town.getName().contains("town-")) { Towns.add(town.getName().substring("town-".length())); }
        }
        return Towns;
    }

    public void NameUpdate(String oldTown, String newTown) {
        DiscordSRV.getPlugin().getMainGuild().getRolesByName(oldTown, true).get(0).getManager().setName(newTown).queue(success -> {
            // success
        }, failure -> {
            // failure
        });;
    }
}
