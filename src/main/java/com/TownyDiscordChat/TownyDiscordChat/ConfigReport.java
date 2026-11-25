package com.TownyDiscordChat.TownyDiscordChat;

import com.TownyDiscordChat.TownyDiscordChat.Messages.LegacyMessages;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Names settings in a server's config.yml that this version no longer reads,
 * and whole sections it is missing. Missing settings use their defaults, and
 * nothing in the file is changed, so admins keep their comments and layout.
 */
public final class ConfigReport {
    private static final List<String> OBSOLETE = Collections.unmodifiableList(Arrays.asList(
            "messages.Role", "messages.Category", "messages.TextChannel", "messages.VoiceChannel",
            "messages.DiscordLogTextChannelId", "messages.Prefix", "messages.Commands",
            "onboarding.message"));

    private ConfigReport() {
    }

    /** Obsolete settings present in the file, with where moved text now lives. */
    public static List<String> obsoleteSettings(ConfigurationSection file) {
        List<String> found = new ArrayList<>();
        for (String key : OBSOLETE) {
            if (file.isSet(key)) {
                found.add(LegacyMessages.isMovedSetting(key) || key.equals("messages.Commands")
                        ? key + " (now in messages.yml)" : key);
            }
        }
        return found;
    }

    /** Top-level sections in the bundled config.yml that the file does not have. */
    public static List<String> missingSections(ConfigurationSection file) {
        List<String> missing = new ArrayList<>();
        Configuration defaults = file.getRoot() == null ? null : file.getRoot().getDefaults();
        if (defaults == null) {
            return missing;
        }
        for (String section : defaults.getKeys(false)) {
            if (!file.isSet(section)) {
                missing.add(section);
            }
        }
        return missing;
    }
}
