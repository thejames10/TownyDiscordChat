package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Channels.Voice;

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
                Bukkit.broadcastMessage("Town VoiceChannel Scheduler Running!");

                List<String> townVoiceChannelNames = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Voice.Towns.SQL.Name().GetAllTownVoiceChannelNames();

                List<String> townTextChannelNames =new com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Text.Towns.SQL.Name().GetAllTownTextChannelNames();

                Set<String> townVoiceChannelSet = new HashSet<>(townVoiceChannelNames);
                List<String> townVoiceChannelMissing = new ArrayList<>();
                for (String town : townTextChannelNames) { if (!townVoiceChannelSet.contains(town)) { townVoiceChannelMissing.add(town); } }
                for (String town : townVoiceChannelMissing) { new QueuedTask(plugin).createTownVoiceChannel(town); }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * startDelaySeconds /*<-- the initial delay */, 20L * intervalDelaySeconds /*<-- the interval */);
        // Ticks > Seconds > Minutes > Hours > Days
    }

    public void cancel() {
        this.scheduler.cancel();
    }
}