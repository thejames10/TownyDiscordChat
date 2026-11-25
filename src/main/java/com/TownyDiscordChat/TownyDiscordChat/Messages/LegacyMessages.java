package com.TownyDiscordChat.TownyDiscordChat.Messages;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Carries messages customised in config.yml by 1.1.x into a new messages.yml.
 * Values still equal to the old defaults are left out, so the new defaults apply.
 */
public final class LegacyMessages {
    private static final String PREFIX_PLACEHOLDER = "{prefix} ";
    private static final Map<String, Legacy> SOURCES = createSources();

    private LegacyMessages() {
    }

    private static final class Legacy {
        private final String configKey;
        private final String oldDefault;
        // 1.1.x prepended the prefix to command replies but not to reminders.
        private final boolean prefixed;

        private Legacy(String configKey, String oldDefault, boolean prefixed) {
            this.configKey = configKey;
            this.oldDefault = oldDefault;
            this.prefixed = prefixed;
        }
    }

    private static Map<String, Legacy> createSources() {
        Map<String, Legacy> sources = new LinkedHashMap<>();
        sources.put("prefix", new Legacy("messages.Prefix", "&8[&2TDC Bot&8]", false));
        sources.put("no-permission", new Legacy("messages.Commands.NoPermission",
                "&7You don't have permission to use this command!", true));
        sources.put("check.requested", new Legacy("messages.Commands.PleaseWait",
                "&7Please wait...", true));
        sources.put("onboarding.reminder", new Legacy("onboarding.message",
                "&eYour town has private Discord channels. Link using &a/tdc link&e (DiscordSRV: /discord link).",
                false));
        return sources;
    }

    /** Maps messages.yml keys to the customised text found in config.yml. */
    public static Map<String, String> overrides(ConfigurationSection config) {
        Map<String, String> overrides = new LinkedHashMap<>();
        if (config == null) {
            return overrides;
        }
        for (Map.Entry<String, Legacy> source : SOURCES.entrySet()) {
            Legacy legacy = source.getValue();
            String value = config.getString(legacy.configKey);
            if (value == null || value.trim().isEmpty() || value.equals(legacy.oldDefault)) {
                continue;
            }
            overrides.put(source.getKey(), legacy.prefixed ? PREFIX_PLACEHOLDER + value : value);
        }
        return overrides;
    }

    /** config.yml keys whose text now lives in messages.yml. */
    public static boolean isMovedSetting(String configKey) {
        for (Legacy legacy : SOURCES.values()) {
            if (legacy.configKey.equals(configKey)) {
                return true;
            }
        }
        return false;
    }
}
