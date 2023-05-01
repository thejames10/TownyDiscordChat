package com.TownyDiscordChat.TownyDiscordChat.Tasks.Roles.Nations.DiscordSRV;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Name extends SQL {

    public static List<String> GetAllNationRoleNames() {
        List<String> Nations = new ArrayList<>();
        for (Role nation : DiscordSRV.getPlugin().getMainGuild().getRoles()) {
            if (nation.getName().contains("nation-")) { Nations.add(nation.getName().substring("nation-".length())); }
        }
        return Nations;
    }
}