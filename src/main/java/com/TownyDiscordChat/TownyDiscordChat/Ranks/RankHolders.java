package com.TownyDiscordChat.TownyDiscordChat.Ranks;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Towny residents who moderate a town or nation channel. Call on the global thread. */
public final class RankHolders {
    private RankHolders() {
    }

    public static Set<UUID> townLeaders(Town town, RankSettings settings) {
        Set<UUID> leaders = new HashSet<UUID>();
        for (Resident resident : town.getResidents()) {
            if (town.isMayor(resident) || hasTownRank(resident, settings.townRanks())) {
                leaders.add(resident.getUUID());
            }
        }
        return leaders;
    }

    public static Set<UUID> nationLeaders(Nation nation, RankSettings settings) {
        Set<UUID> leaders = new HashSet<UUID>();
        for (Resident resident : nation.getResidents()) {
            if (nation.isKing(resident) || hasNationRank(resident, settings.nationRanks())) {
                leaders.add(resident.getUUID());
            }
        }
        return leaders;
    }

    private static boolean hasTownRank(Resident resident, List<String> ranks) {
        for (String rank : ranks) {
            if (resident.hasTownRank(rank)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNationRank(Resident resident, List<String> ranks) {
        for (String rank : ranks) {
            if (resident.hasNationRank(rank)) {
                return true;
            }
        }
        return false;
    }
}
