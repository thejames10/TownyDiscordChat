package com.TownyDiscordChat.TownyDiscordChat.Tasks.Nations.DiscordSRV;

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

    public List<String> GetAllNations() {
        List<String> Nations = new ArrayList<>();
        for (Role nation : DiscordSRV.getPlugin().getMainGuild().getRoles()) {
            if (nation.getName().contains("nation-")) { Nations.add(nation.getName().substring("nation-".length())); }
        }
        return Nations;
    }

    public void SetName(String oldNation, String newNation) {
        DiscordSRV.getPlugin().getMainGuild().getRolesByName(oldNation, true).get(0).getManager().setName(newNation).queue(success -> {
            // success
        }, failure -> {
            // failure
        });;
    }

    public void Queue(String nationRoleName) {
        Map<String,String> keyPair = new HashMap<>();
        keyPair.put("nationRoleName",nationRoleName);
        createEntry(nationRoleName, "nationRoleName", SQL.TABLE_QUEUED_TASKS, keyPair, true);
    }
}
