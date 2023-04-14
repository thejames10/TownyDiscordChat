package com.TownyDiscordChat.TownyDiscordChat.Schedulers;

import com.TownyDiscordChat.TownyDiscordChat.Core.TDCManager;
import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Queue {

    private final Main plugin;

    public Queue (Main plugin) {

        this.plugin = plugin;
        // With BukkitRunnable
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("Queue Scheduler Running!");

                Map<String,String> queue = SQL.getQueueFirst();
                if (queue != null) {

                    String timeCol = "Time";
                    String timeValue = queue.get(timeCol);
                    queue.remove(timeCol);

                    String column = "", value = "";

                    Iterator<Map.Entry<String, String>> iterator = queue.entrySet().iterator();
                    if (iterator.hasNext()) {
                        Map.Entry<String, String> entry = iterator.next();
                        column = entry.getKey();
                        value = entry.getValue();
                    }

                    Bukkit.broadcastMessage("Column: " + timeCol);
                    Bukkit.broadcastMessage("Value: " + timeValue);

                    Bukkit.broadcastMessage("Column: " + column);
                    Bukkit.broadcastMessage("Value: " + value);

                    // Perform Queue Action For DiscordSRV
                    switch (column) {
                        case "townRoleName" -> {
                            Bukkit.broadcastMessage("Creating Town Role in Discord");
                            Town town = TownyUniverse.getInstance().getTown(value);
                            assert town != null;
                            // Call the createDiscordRole method with a callback function that will print the role ID when it is available
                            String finalValue = value, finalColumn = column;
                            TDCManager.createDiscordRole(town, roleId -> {
                                if (roleId != null) {
                                    System.out.println("Role created with ID: " + roleId);

                                    // Add Role ID to SQL
                                    Map<String,String> keyPair = new HashMap<>();
                                    keyPair.put("townRoleId", roleId);
                                    new SQL().createEntry(finalValue, finalColumn, SQL.TABLE_TOWNS, keyPair, false);

                                    // Remove From Queue Once DiscordSRV Action Completed
                                    new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS);
                                } else {
                                    System.out.println("Failed to create role.");
                                }
                            });
                        }
                        case "nationRoleName" -> {
                            Bukkit.broadcastMessage("Creating Nation Role in Discord");
                            Nation nation = TownyUniverse.getInstance().getNation(value);
                            assert nation != null;
                            // Call the createDiscordRole method with a callback function that will print the role ID when it is available
                            String finalValue = value, finalColumn = column;
                            TDCManager.createDiscordRole(nation, roleId -> {
                                if (roleId != null) {
                                    System.out.println("Role created with ID: " + roleId);

                                    // Add Role ID to SQL
                                    Map<String,String> keyPair = new HashMap<>();
                                    keyPair.put("nationRoleId", roleId);
                                    new SQL().createEntry(finalValue, finalColumn, SQL.TABLE_NATIONS, keyPair, false);

                                    // Remove From Queue Once DiscordSRV Action Completed
                                    new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS);
                                } else {
                                    System.out.println("Failed to create role.");
                                }
                            });
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 15L /*<-- the initial delay */, 20L * 15L /*<-- the interval */);
        // Ticks > Seconds > Minutes > Hours > Days
    }
}
