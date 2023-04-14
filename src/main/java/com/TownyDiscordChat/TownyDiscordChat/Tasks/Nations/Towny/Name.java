package com.TownyDiscordChat.TownyDiscordChat.Tasks.Nations.Towny;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Name {

    public Name () {

    }

    public List<String> GetAllNations () {
        List<String> Nations = new ArrayList<>();
        for (Nation nation : TownyUniverse.getInstance().getNations()) { Nations.add(nation.getName()); }
        return Nations;
    }

    public void SetName (String oldNation, String newNation) {
        Objects.requireNonNull(TownyUniverse.getInstance().getNation(oldNation)).setName(newNation);
    }
}