package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Nations;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.QueuedTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Nations {

    // Goes through all Towns and makes sure SQL is up-to-date
    // -> Each individiual change will be queued

    // Goes through all DiscordSRV Roles
    // -> Makes sure Roles are created
    //

    private final Main plugin;

    public Nations (Main plugin) {

        this.plugin = plugin;
        // With BukkitRunnable
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("Nations Scheduler Running!");

                List<String> TownyNations = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Nations.Towny.Name().GetAllNations();

                List<String> SQLNations = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Nations.SQL.Name().GetAllNations();
                List<String> DiscordSRVNations = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Nations.DiscordSRV.Name().GetAllNations();;

                Set<String> SQLSet = new HashSet<>(SQLNations);
                List<String> SQLNationsMissing = new ArrayList<>();
                for (String nation : TownyNations) { if (!SQLSet.contains(nation)) { SQLNationsMissing.add(nation); } }
                for (String nation : SQLNationsMissing) { new com.TownyDiscordChat.TownyDiscordChat.Tasks.Nations.SQL.Name().AddName(nation); }

                Set<String> DiscordSRVSet = new HashSet<>(DiscordSRVNations);
                List<String> DiscordSRVMissing = new ArrayList<>();
                for (String nation : TownyNations) { if (!DiscordSRVSet.contains(nation)) { DiscordSRVMissing.add(nation); } }
                for (String nation : DiscordSRVMissing) { new QueuedTask(plugin).createNationRole(nation); }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 15L /*<-- the initial delay */, 20L * 15L /*<-- the interval */);
        // Ticks > Seconds > Minutes > Hours > Days
    }
}