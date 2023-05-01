package com.TownyDiscordChat.TownyDiscordChat.Tasks.Roles.Towns.DiscordSRV;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;

import java.util.ArrayList;
import java.util.List;

public class Name extends SQL {

    public static List<String> GetAllTownRoleNames() {
        List<String> Towns = new ArrayList<>();
        for (Role town : DiscordSRV.getPlugin().getMainGuild().getRoles()) {
            if (town.getName().contains("town-")) { Towns.add(town.getName().substring("town-".length())); }
        }
        return Towns;
    }
}
