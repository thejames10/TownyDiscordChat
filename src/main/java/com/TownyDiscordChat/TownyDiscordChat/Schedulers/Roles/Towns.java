package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Roles;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.QueuedTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class Towns {

    private final BukkitTask scheduler;

    public Towns (long startDelaySeconds, long intervalDelaySeconds, Main plugin) {
        // Creating a BukkitRunnable to run the task asynchronously on a schedule
        scheduler = new BukkitRunnable() {
            @Override
            public void run() {
                // Broadcast a message to all players on the server
                Bukkit.broadcastMessage("Town Role Scheduler Running!");
                // Get all town role names from Towny and SQL databases
                List<String> TownyTownRoleNames = com.TownyDiscordChat.TownyDiscordChat.Tasks.Roles.Towns.Towny.Name.GetAllTownRoleNames();
                List<String> SQLTownRoleNames = com.TownyDiscordChat.TownyDiscordChat.Tasks.Roles.Towns.SQL.Name.GetAllTownRoleNames();
                // Find the missing town role names
                List<String> missing = new ArrayList<>(TownyTownRoleNames);
                missing.removeAll(SQLTownRoleNames);
                // Create a QueuedTask for each missing town role
                missing.forEach(townRoleName -> new QueuedTask(plugin).createTownRole(townRoleName));
            }
        }.runTaskTimerAsynchronously(plugin, 20L * startDelaySeconds /*<-- the initial delay */, 20L * intervalDelaySeconds /*<-- the interval */);
        // The initial delay and interval are given in seconds and converted to ticks (1 tick = 1/20 second)
    }

    public void cancel() {
        // Cancel the task
        this.scheduler.cancel();
    }
}