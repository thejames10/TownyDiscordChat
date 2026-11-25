package com.TownyDiscordChat.TownyDiscordChat.Scheduling;

import org.bukkit.entity.Player;

/**
 * Schedules plugin work on Bukkit-style servers and on Folia. "Global" tasks
 * run on the main server thread, or on Folia's global region thread, which is
 * where Towny and DiscordSRV state may be read.
 */
public interface TaskScheduler {
    void runGlobal(Runnable task);

    void runGlobalLater(Runnable task, long delayTicks);

    RepeatingTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks);

    void runAsync(Runnable task);

    RepeatingTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks);

    /** Runs where the player is ticked; nothing happens if the player has left. */
    void runForPlayer(Player player, Runnable task);

    void cancelAll();
}
