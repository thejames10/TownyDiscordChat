package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Roles;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.QueuedTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class Nations {

    private final BukkitTask scheduler;

    public Nations (long startDelaySeconds, long intervalDelaySeconds, Main plugin) {
        // Creating a BukkitRunnable to run the task asynchronously on a schedule
        scheduler = new BukkitRunnable() {
            @Override
            public void run() {
                // Broadcast a message to all players on the server
                Bukkit.broadcastMessage("Nation Role Scheduler Running!");
                // Get all nation role names from Towny and SQL databases
                List<String> TownyNationRoleNames = com.TownyDiscordChat.TownyDiscordChat.Tasks.Roles.Nations.Towny.Name.GetAllNationRoleNames();
                List<String> SQLNationRoleNames = com.TownyDiscordChat.TownyDiscordChat.Tasks.Roles.Nations.SQL.Name.GetAllNationRoleNames();
                // Find the missing nation role names
                List<String> missing = new ArrayList<>(TownyNationRoleNames);
                missing.removeAll(SQLNationRoleNames);
                // Create a QueuedTask for each missing nation role
                missing.forEach(nationRoleName -> new QueuedTask(plugin).createNationRole(nationRoleName));
            }
        }.runTaskTimerAsynchronously(plugin, 20L * startDelaySeconds /*<-- the initial delay */, 20L * intervalDelaySeconds /*<-- the interval */);
        // The initial delay and interval are given in seconds and converted to ticks (1 tick = 1/20 second)
    }

    public void cancel() {
        // Cancel the task
        this.scheduler.cancel();
    }
}