package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Queue;

import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Category;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.PermissionOverride;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Where a deleted town or nation's text channel goes when archive.enabled is
 * on. An archive category must hide channels from @everyone: archived town
 * history is as private as the town channel was.
 */
public final class ChannelArchive {
    public static final String ENABLED_KEY = "archive.enabled";
    public static final String CATEGORY_KEY = "archive.categoryName";
    static final String DEFAULT_CATEGORY = "MINECRAFT-ARCHIVE";
    private static final String CAPACITY_KEY = "category.SubChannelsMax";
    private static final int DISCORD_CATEGORY_LIMIT = 50;

    private ChannelArchive() {
    }

    public static boolean enabled(ConfigurationSection config) {
        return config.getBoolean(ENABLED_KEY, false);
    }

    public static String categoryName(ConfigurationSection config) {
        return config.getString(CATEGORY_KEY, DEFAULT_CATEGORY);
    }

    /** A private archive category with room for one more channel, or null. */
    static Category availableCategory(Guild guild, ConfigurationSection config) {
        int capacity = Math.max(1, Math.min(DISCORD_CATEGORY_LIMIT,
                config.getInt(CAPACITY_KEY, DISCORD_CATEGORY_LIMIT)));
        for (Category category : guild.getCategoriesByName(categoryName(config), true)) {
            if (isPrivate(guild, category) && category.getChannels().size() < capacity) {
                return category;
            }
        }
        return null;
    }

    /** True when a category with the archive's name lets @everyone see it. */
    public static boolean hasPublicCategory(Guild guild, ConfigurationSection config) {
        for (Category category : guild.getCategoriesByName(categoryName(config), true)) {
            if (!isPrivate(guild, category)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPrivate(Guild guild, Category category) {
        PermissionOverride everyone = category.getPermissionOverride(guild.getPublicRole());
        return everyone != null && everyone.getDenied().contains(Permission.VIEW_CHANNEL);
    }
}
