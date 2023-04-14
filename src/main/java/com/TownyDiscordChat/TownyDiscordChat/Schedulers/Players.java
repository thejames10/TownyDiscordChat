package com.TownyDiscordChat.TownyDiscordChat.Schedulers;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.QueuedTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Players {

    private final Main plugin;

    public Players(Main plugin) {

        this.plugin = plugin;
        // With BukkitRunnable
        new BukkitRunnable() {
            @Override
            public void run() {
                // Run Player Tasks From Queue //

                // Get Queue
                try {
                    // Getting the oldest Player result in the Queue Table
                    PreparedStatement queuePS = plugin.SQL.getConnection().prepareStatement("SELECT * FROM " + QueuedTask.TABLE + " WHERE Expired != 'true' AND Type = '" + QueuedTask._PLAYER + "' ORDER BY CAST(Time AS SIGNED) LIMIT 1");

                    ResultSet queueResultSet = queuePS.executeQuery();
                    if (queueResultSet.next()) {

                        String queueUUID = queueResultSet.getString("UUID");
                        String queuediscordUserId = queueResultSet.getString("discordUserId");
                        String queuetownRoleName = queueResultSet.getString("townRoleName");
                        String queuenationRoleName = queueResultSet.getString("nationRoleName");
                        String queueisMayor = queueResultSet.getString("isMayor");
                        String queueisKing = queueResultSet.getString("isKing");
                        //String queuedoCleanUp = queueResultSet.getString("Expired");

                        // Checking if Player is already in database
                        if (Main.plugin.playersDB.entryExists(queueUUID, "UUID")) {
                            // Pulling Player info from database
                            PreparedStatement playersPS = plugin.SQL.getConnection().prepareStatement("SELECT * FROM " + com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.Players.TABLE + " WHERE UUID = '" + queueUUID + "'");
                            ResultSet playersResult = playersPS.executeQuery();

                            // If the query returns a result
                            if (playersResult.next()) {
                                String currentDiscordUserId = playersResult.getString("discordUserId");
                                String currentTownRoleName = playersResult.getString("townRoleName");
                                String currentNationRoleName = playersResult.getString("nationRoleName");
                                String currentIsMayor = playersResult.getString("isMayor");
                                String currentIsKing = playersResult.getString("isKing");

                                // Checking to see if the Player's discordId has changed
                                if (!currentDiscordUserId.equalsIgnoreCase(queuediscordUserId)) {
                                    // Discord User ID has changed. Let DiscordSRV know and Update database
                                    // Reset DiscordSRV link and start new player link
                                    // Remove Nation Role from old discord user
                                    // Remove Town Role from old discord user

                                    // Look at this later. Edge Case.
                                }

                                // Checking to see if Player has changed Town or Nations
                                if (queuenationRoleName.equalsIgnoreCase(currentNationRoleName)) {
                                    if (queuetownRoleName.equalsIgnoreCase(currentTownRoleName)) {
                                        // case 1: same nation, same town
                                        System.out.println("Case 1: Same nation, same town");
                                        // Check isKing
                                        // Check isMayor
                                        // Update Player Database
                                        // Remove From Queue

                                        if (queueisKing.equalsIgnoreCase(currentIsKing)) {
                                            if (queueisMayor.equalsIgnoreCase(currentIsMayor)) {
                                                // Should Never Happen
                                                // Remove From Queue
                                            } else {
                                                // Remove Old Mayor Role From Player
                                                // Add New Mayor Role To Player
                                                // Update Player Database
                                                // Remove From Queue
                                            }
                                        } else {
                                            // Remove Old King Role From Player
                                            // Add New King Role To Player
                                            // Update Player Database
                                            // Remove From Queue
                                        }


                                    } else {
                                        // case 2: same nation, different town
                                        System.out.println("Case 2: Same nation, different town");
                                        // Player Changed Towns
                                        // Check isKing
                                        // Check isMayor
                                        // Remove Old Town Role From Player
                                        // Add New Town Role To Player
                                        // Update Player Database
                                        // Remove From Queue
                                    }
                                } else {
                                    if (queuetownRoleName.equalsIgnoreCase(currentTownRoleName)) {
                                        // case 3: different nation, same town
                                        System.out.println("Case 3: Different nation, same town");
                                        // Player Changed Nations
                                        // Check isKing
                                        // Check isMayor
                                        // Remove Old Nation Role From Player
                                        // Add New Nation Role To Player
                                        // Update Player Database
                                        // Remove From Queue
                                    } else {
                                        // case 4: different nation, different town
                                        System.out.println("Case 4: Different nation, different town");
                                        // Player Changed Towns and Nations
                                        // Check isKing
                                        // Check isMayor
                                        // Remove Old Town Role From Player
                                        // Remove Old Nation Role From Player
                                        // Add New Town Role To Player
                                        // Add New Nation Role To Player
                                        // Update Player Database
                                        // Remove From Queue
                                    }
                                }
                            }
                        } else {
                            Main.plugin.playersDB.createEntry(queueUUID, queuediscordUserId, queuetownRoleName, queuenationRoleName, queueisMayor, queueisKing);
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                // Determine Type

                // Fire Task

                // Update Correct TABLE

                // Remove From Queue
                Bukkit.broadcastMessage("Players Scheduler Running!");
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60L * 5L /*<-- the initial delay */, 20L * 60L * 5L /*<-- the interval */);
        // Ticks > Seconds > Minutes > Hours > Days
    }
}
