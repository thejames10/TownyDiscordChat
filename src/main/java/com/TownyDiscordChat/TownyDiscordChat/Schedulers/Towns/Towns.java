package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Towns;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.QueuedTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Towns {

    // Goes through all Towns and makes sure SQL is up-to-date
    // -> Each individiual change will be queued

    // Goes through all DiscordSRV Roles
    // -> Makes sure Roles are created
    //

    private final Main plugin;

    public Towns (Main plugin) {

        this.plugin = plugin;
        // With BukkitRunnable
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("Towns Scheduler Running!");

                List<String> TownyTowns = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Towns.Towny.Name().GetAllTowns();

                List<String> SQLTowns = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Towns.SQL.Name().GetAllTowns();
                List<String> DiscordSRVTowns = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Towns.DiscordSRV.Name().GetAllTowns();;

                Set<String> SQLSet = new HashSet<>(SQLTowns);
                List<String> SQLTownsMissing = new ArrayList<>();
                for (String town : TownyTowns) { if (!SQLSet.contains(town)) { SQLTownsMissing.add(town); } }
                for (String town : SQLTownsMissing) { new com.TownyDiscordChat.TownyDiscordChat.Tasks.Towns.SQL.Name().AddName(town); }

                Set<String> DiscordSRVSet = new HashSet<>(DiscordSRVTowns);
                List<String> DiscordSRVMissing = new ArrayList<>();
                for (String town : TownyTowns) { if (!DiscordSRVSet.contains(town)) { DiscordSRVMissing.add(town); } }
                for (String town : DiscordSRVMissing) { new QueuedTask(plugin).createTownRole(town); }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 15L /*<-- the initial delay */, 20L * 15L /*<-- the interval */);
        // Ticks > Seconds > Minutes > Hours > Days
    }
}
