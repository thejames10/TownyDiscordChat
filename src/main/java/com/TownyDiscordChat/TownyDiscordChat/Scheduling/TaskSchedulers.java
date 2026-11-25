package com.TownyDiscordChat.TownyDiscordChat.Scheduling;

import org.bukkit.plugin.Plugin;

/** Picks the scheduler implementation for the running server. */
public final class TaskSchedulers {
    private static final String FOLIA_MARKER_CLASS = "io.papermc.paper.threadedregions.RegionizedServer";

    private TaskSchedulers() {
    }

    public static boolean isFolia() {
        try {
            Class.forName(FOLIA_MARKER_CLASS);
            return true;
        } catch (ClassNotFoundException notFolia) {
            return false;
        }
    }

    public static TaskScheduler create(Plugin plugin) {
        if (isFolia()) {
            return FoliaTaskScheduler.create(plugin);
        }
        return new BukkitTaskScheduler(plugin);
    }
}
