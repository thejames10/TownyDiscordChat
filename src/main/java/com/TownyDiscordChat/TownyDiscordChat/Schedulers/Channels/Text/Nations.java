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

public class Nations {

    private final Main plugin;

    private final BukkitTask scheduler;

    public Nations (long startDelaySeconds, long intervalDelaySeconds, Main plugin) {

        this.plugin = plugin;
        // With BukkitRunnable
        scheduler = new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("Nation TextChannel Scheduler Running!");

                List<String> nationTextChannelNames =new com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Text.Nations.SQL.Name().GetAllNationTextChannelNames();

                List<String> nationRoleNames = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Roles.Nations.SQL.Name().GetAllNationRoleNames();

                Set<String> nationTextChannelSet = new HashSet<>(nationTextChannelNames);
                List<String> nationTextChannelMissing = new ArrayList<>();
                for (String nation : nationRoleNames) { if (!nationTextChannelSet.contains(nation)) { nationTextChannelMissing.add(nation); } }
                for (String nation : nationTextChannelMissing) { new QueuedTask(plugin).createNationTextChannel(nation); }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * startDelaySeconds /*<-- the initial delay */, 20L * intervalDelaySeconds /*<-- the interval */);
        // Ticks > Seconds > Minutes > Hours > Days
    }

    public void cancel() {
        this.scheduler.cancel();
    }
}
