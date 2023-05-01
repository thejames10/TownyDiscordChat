package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Players;

import com.TownyDiscordChat.TownyDiscordChat.Core.TDCManager;
import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.QueuedTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Players {

    private final Main plugin;

    private final BukkitTask scheduler;

    public Players (Main plugin) {

        this.plugin = plugin;
        // With BukkitRunnable
        scheduler = new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("Players Scheduler Running!");

                if (!SQL.getAllEntries("townRoleName", SQL.TABLE_TOWN_ROLES).isEmpty()) {
                    TDCManager.discordUserRoleCheckAllLinked();
                }
            }
        }.runTaskAsynchronously(plugin);
        // Ticks > Seconds > Minutes > Hours > Days
    }

    public void cancel() {
        this.scheduler.cancel();
    }
}