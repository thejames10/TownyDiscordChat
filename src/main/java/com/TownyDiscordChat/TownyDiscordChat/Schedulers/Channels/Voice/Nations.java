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

public class Nations {

    private final Main plugin;

    private final BukkitTask scheduler;

    public Nations (long startDelaySeconds, long intervalDelaySeconds, Main plugin) {

        this.plugin = plugin;
        // With BukkitRunnable
        scheduler = new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("Nation VoiceChannel Scheduler Running!");

                List<String> nationVoiceChannelNames =new com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Voice.Nations.SQL.Name().GetAllNationVoiceChannelNames();

                List<String> nationRoleNames = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Roles.Nations.SQL.Name().GetAllNationRoleNames();

                Set<String> nationVoiceChannelSet = new HashSet<>(nationVoiceChannelNames);
                List<String> nationVoiceChannelMissing = new ArrayList<>();
                for (String nation : nationRoleNames) { if (!nationVoiceChannelSet.contains(nation)) { nationVoiceChannelMissing.add(nation); } }
                for (String nation : nationVoiceChannelMissing) { new QueuedTask(plugin).createNationVoiceChannel(nation); }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * startDelaySeconds /*<-- the initial delay */, 20L * intervalDelaySeconds /*<-- the interval */);
        // Ticks > Seconds > Minutes > Hours > Days
    }

    public void cancel() {
        this.scheduler.cancel();
    }
}