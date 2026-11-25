package com.TownyDiscordChat.TownyDiscordChat.Scheduling;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

/** Paper, Purpur, Spigot and CraftBukkit: one main thread runs all global work. */
final class BukkitTaskScheduler implements TaskScheduler {
    private final Plugin plugin;

    BukkitTaskScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    private BukkitScheduler scheduler() {
        return plugin.getServer().getScheduler();
    }

    @Override
    public void runGlobal(Runnable task) {
        scheduler().runTask(plugin, task);
    }

    @Override
    public void runGlobalLater(Runnable task, long delayTicks) {
        scheduler().runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public RepeatingTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask handle = scheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        return handle::cancel;
    }

    @Override
    public void runAsync(Runnable task) {
        scheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public RepeatingTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask handle = scheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        return handle::cancel;
    }

    @Override
    public void runForPlayer(Player player, Runnable task) {
        runGlobal(task);
    }

    @Override
    public void cancelAll() {
        scheduler().cancelTasks(plugin);
    }
}
