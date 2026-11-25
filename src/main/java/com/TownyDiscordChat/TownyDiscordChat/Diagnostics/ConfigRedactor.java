package com.TownyDiscordChat.TownyDiscordChat.Diagnostics;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** config.yml as text for a support report, with credentials hidden. */
public final class ConfigRedactor {
    private static final Set<String> CREDENTIAL_KEYS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("SQL.host", "SQL.username", "SQL.password")));
    private static final Pattern SECRET_NAME = Pattern.compile("(?i)password|token|secret|api.?key");
    private static final String HIDDEN = "(set, hidden)";
    private static final String NOT_SET = "(not set)";
    private static final String NOT_LOADED = "config.yml was not loaded.";

    private ConfigRedactor() {
    }

    public static List<String> lines(ConfigurationSection config) {
        List<String> lines = new ArrayList<>();
        if (config == null) {
            lines.add(NOT_LOADED);
            return lines;
        }
        for (String key : config.getKeys(true)) {
            if (!config.isConfigurationSection(key)) {
                lines.add(key + ": " + shown(key, config.get(key)));
            }
        }
        return lines;
    }

    private static String shown(String key, Object value) {
        if (!isSecret(key)) {
            return String.valueOf(value);
        }
        return value == null || String.valueOf(value).trim().isEmpty() ? NOT_SET : HIDDEN;
    }

    static boolean isSecret(String key) {
        return CREDENTIAL_KEYS.contains(key) || SECRET_NAME.matcher(key).find();
    }
}
