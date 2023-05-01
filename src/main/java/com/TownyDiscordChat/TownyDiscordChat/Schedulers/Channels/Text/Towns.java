package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Channels.Text;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.QueuedTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Towns {

    private final Main plugin;

    private final BukkitTask scheduler;

    public Towns (long startDelaySeconds, long intervalDelaySeconds, Main plugin) {

        this.plugin = plugin;
        // With BukkitRunnable
        scheduler = new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("Town TextChannel Scheduler Running!");

                List<String> townTextChannelNames =new com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Text.Towns.SQL.Name().GetAllTownTextChannelNames();

                List<String> townRoleNames = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Roles.Towns.SQL.Name().GetAllTownRoleNames();

                Set<String> townTextChannelSet = new HashSet<>(townTextChannelNames);
                List<String> townTextChannelMissing = new ArrayList<>();
                for (String town : townRoleNames) { if (!townTextChannelSet.contains(town)) { townTextChannelMissing.add(town); } }
                for (String town : townTextChannelMissing) { new QueuedTask(plugin).createTownTextChannel(town); }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * startDelaySeconds /*<-- the initial delay */, 20L * intervalDelaySeconds /*<-- the interval */);
        // Ticks > Seconds > Minutes > Hours > Days
    }

    public void cancel() {
        this.scheduler.cancel();
    }
}