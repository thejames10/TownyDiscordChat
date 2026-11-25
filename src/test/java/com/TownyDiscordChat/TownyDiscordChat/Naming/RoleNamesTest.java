package com.TownyDiscordChat.TownyDiscordChat.Naming;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RoleNamesTest {
    private static YamlConfiguration names(String town, String nation) {
        YamlConfiguration config = new YamlConfiguration();
        config.set(RoleNames.TOWN_KEY, town);
        config.set(RoleNames.NATION_KEY, nation);
        return config;
    }

    @Test
    public void defaultsMatchTheNamesUsedBefore() {
        RoleNames defaults = RoleNames.fromConfig(new YamlConfiguration());
        assertEquals("town-Oakvale", defaults.town().format("Oakvale"));
        assertEquals("nation-Northlands", defaults.nation().format("Northlands"));
    }

    @Test
    public void templatesWorkBothWays() {
        RoleNameTemplate template = RoleNameTemplate.parse("[{name}] Town");
        assertEquals("[Oakvale] Town", template.format("Oakvale"));
        assertEquals("Oakvale", template.townyName("[Oakvale] Town"));
        assertNull(template.townyName("[Oakvale] Nation"));
        assertNull(template.townyName("[] Town"));
    }

    @Test
    public void aTemplateNeedsExactlyOnePlaceholderAndSomeFixedText() {
        assertThrows(IllegalArgumentException.class, () -> RoleNameTemplate.parse("town"));
        assertThrows(IllegalArgumentException.class, () -> RoleNameTemplate.parse("{name}-{name}"));
        assertThrows(IllegalArgumentException.class, () -> RoleNameTemplate.parse(" {name} "));
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i <= RoleNameTemplate.MAXIMUM_FIXED_LENGTH; i++) {
            longText.append('x');
        }
        assertThrows(IllegalArgumentException.class, () -> RoleNameTemplate.parse(longText + "{name}"));
    }

    @Test
    public void templatesThatCouldShareARoleNameAreRejected() {
        assertNotNull(RoleNames.problem(names("{name}-role", "{name}-role")));
        assertNotNull(RoleNames.problem(names("t-{name}", "t-n-{name}")));
        assertNotNull(RoleNames.problem(names("town {name}", "town {name} nation")));
        assertNull(RoleNames.problem(names("town-{name}", "nation-{name}")));
        assertNull(RoleNames.problem(names("{name} (town)", "{name} (nation)")));
    }

    @Test
    public void invalidTemplatesFallBackToTheDefaults() {
        RoleNames names = RoleNames.fromConfig(names("{name}", "nation-{name}"));
        assertEquals("town-Oakvale", names.town().format("Oakvale"));
    }

    @Test
    public void managedNamesFollowTheTemplates() {
        RoleNames names = RoleNames.fromConfig(names("{name} Citizens", "{name} Nationals"));
        assertTrue(names.isManagedName("Oakvale Citizens"));
        assertFalse(names.isManagedName("town-Oakvale"));
        assertEquals("Oakvale", names.forType("TOWN").townyName("Oakvale Citizens"));
        assertEquals("Northlands", names.forType("NATION").townyName("Northlands Nationals"));
    }
}
