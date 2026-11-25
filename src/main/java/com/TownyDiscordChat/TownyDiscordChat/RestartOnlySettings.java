package com.TownyDiscordChat.TownyDiscordChat;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Settings read once at startup. /tdc reload reports a change to any of them
 * instead of applying it, because storage and timers are already running.
 */
public final class RestartOnlySettings {
    private static final List<String> SECTIONS = Collections.unmodifiableList(
            Arrays.asList("storage", "SQL"));
    private static final List<String> KEYS = Collections.unmodifiableList(Arrays.asList(
            "queue.startDelaySeconds", "queue.pollSeconds", "queue.reconcileSeconds"));

    private RestartOnlySettings() {
    }

    public static List<String> changed(ConfigurationSection before, ConfigurationSection after) {
        List<String> changed = new ArrayList<>();
        for (String key : watchedKeys(before, after)) {
            if (!Objects.equals(before.get(key), after.get(key))) {
                changed.add(key);
            }
        }
        return changed;
    }

    private static Set<String> watchedKeys(ConfigurationSection before, ConfigurationSection after) {
        Set<String> keys = new TreeSet<>(KEYS);
        for (ConfigurationSection config : Arrays.asList(before, after)) {
            for (String section : SECTIONS) {
                ConfigurationSection values = config.getConfigurationSection(section);
                if (values == null) {
                    continue;
                }
                for (String key : values.getKeys(true)) {
                    if (!values.isConfigurationSection(key)) {
                        keys.add(section + "." + key);
                    }
                }
            }
        }
        return keys;
    }
}
