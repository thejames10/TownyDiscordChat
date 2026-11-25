package com.TownyDiscordChat.TownyDiscordChat.Ranks;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Which Towny ranks moderate their own town and nation Discord channels. */
public final class RankSettings {
    public static final String ENABLED_KEY = "ranks.enabled";
    public static final String TOWN_RANKS_KEY = "ranks.town";
    public static final String NATION_RANKS_KEY = "ranks.nation";

    private final boolean enabled;
    private final List<String> townRanks;
    private final List<String> nationRanks;

    RankSettings(boolean enabled, List<String> townRanks, List<String> nationRanks) {
        this.enabled = enabled;
        this.townRanks = Collections.unmodifiableList(new ArrayList<String>(townRanks));
        this.nationRanks = Collections.unmodifiableList(new ArrayList<String>(nationRanks));
    }

    public static RankSettings fromConfig(ConfigurationSection config) {
        return new RankSettings(config.getBoolean(ENABLED_KEY, false),
                rankNames(config, TOWN_RANKS_KEY), rankNames(config, NATION_RANKS_KEY));
    }

    private static List<String> rankNames(ConfigurationSection config, String key) {
        List<String> names = new ArrayList<String>();
        for (String name : config.getStringList(key)) {
            if (name != null && !name.trim().isEmpty()) {
                names.add(name.trim());
            }
        }
        return names;
    }

    public boolean enabled() {
        return enabled;
    }

    /** Extra Towny town ranks; the mayor always qualifies. */
    public List<String> townRanks() {
        return townRanks;
    }

    /** Extra Towny nation ranks; the king always qualifies. */
    public List<String> nationRanks() {
        return nationRanks;
    }
}
