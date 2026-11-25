package com.TownyDiscordChat.TownyDiscordChat.Naming;

import org.bukkit.configuration.ConfigurationSection;

/** The role name templates for towns and nations from config.yml. */
public final class RoleNames {
    public static final String TOWN_KEY = "names.townRole";
    public static final String NATION_KEY = "names.nationRole";
    static final String TOWN_DEFAULT = "town-{name}";
    static final String NATION_DEFAULT = "nation-{name}";
    private static final String TOWN_TYPE = "TOWN";

    private final RoleNameTemplate town;
    private final RoleNameTemplate nation;

    private RoleNames(RoleNameTemplate town, RoleNameTemplate nation) {
        this.town = town;
        this.nation = nation;
    }

    public static RoleNames defaults() {
        return new RoleNames(RoleNameTemplate.parse(TOWN_DEFAULT), RoleNameTemplate.parse(NATION_DEFAULT));
    }

    /** Falls back to the defaults when the templates are invalid; validation reports why. */
    public static RoleNames fromConfig(ConfigurationSection config) {
        if (config == null || problem(config) != null) {
            return defaults();
        }
        return new RoleNames(RoleNameTemplate.parse(config.getString(TOWN_KEY, TOWN_DEFAULT)),
                RoleNameTemplate.parse(config.getString(NATION_KEY, NATION_DEFAULT)));
    }

    /** Why the configured templates can't be used, or null when they can. */
    public static String problem(ConfigurationSection config) {
        RoleNameTemplate town;
        RoleNameTemplate nation;
        try {
            town = RoleNameTemplate.parse(config.getString(TOWN_KEY, TOWN_DEFAULT));
        } catch (IllegalArgumentException invalid) {
            return TOWN_KEY + " " + invalid.getMessage() + ".";
        }
        try {
            nation = RoleNameTemplate.parse(config.getString(NATION_KEY, NATION_DEFAULT));
        } catch (IllegalArgumentException invalid) {
            return NATION_KEY + " " + invalid.getMessage() + ".";
        }
        if (town.overlaps(nation)) {
            return TOWN_KEY + " and " + NATION_KEY + " could produce the same role name;"
                    + " give them different text before or after {name}.";
        }
        return null;
    }

    public RoleNameTemplate town() {
        return town;
    }

    public RoleNameTemplate nation() {
        return nation;
    }

    /** @param type "TOWN" or "NATION", as used by the queue */
    public RoleNameTemplate forType(String type) {
        return TOWN_TYPE.equals(type) ? town : nation;
    }

    public boolean isManagedName(String roleName) {
        return town.matches(roleName) || nation.matches(roleName);
    }
}
