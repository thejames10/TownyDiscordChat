package com.TownyDiscordChat.TownyDiscordChat.Tasks.Towns.Towny;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Name {

    public Name () {

    }

    public List<String> GetAllTowns () {
        List<String> Towns = new ArrayList<>();
        for (Town town : TownyUniverse.getInstance().getTowns()) { Towns.add(town.getName()); }
        return Towns;
    }

    public void SetName (String oldTown, String newTown) {
        Objects.requireNonNull(TownyUniverse.getInstance().getTown(oldTown)).setName(newTown);
    }
}
