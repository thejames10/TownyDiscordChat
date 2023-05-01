package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Queue;

import com.TownyDiscordChat.TownyDiscordChat.Core.TDCManager;
import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class Queue {

    private final Main plugin;

    private final BukkitTask scheduler;

    public Queue (long startDelaySeconds, long intervalDelaySeconds, Main plugin) {

        this.plugin = plugin;
        // With BukkitRunnable
        scheduler = new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("Queue Scheduler Running!");

                Map<String,String> queue = SQL.getQueueFirst();
                if (queue != null) {

                    String timeCol = "Time";
                    String timeValue = queue.get(timeCol);
                    queue.remove(timeCol);

                    LocalDateTime dateTime = SQL.LongToDateTime(Long.parseLong(timeValue));

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
                        // Town Role Creation
                        case "townRoleName" -> {
                            Bukkit.broadcastMessage("Creating Town Role in Discord");

                            if (TimeDifference(dateTime) < Main.plugin.config.getInt("Discord.Api.RateLimit.Roles") * 2L) { break; }

                            Town town = TownyUniverse.getInstance().getTown(value);
                            assert town != null;
                            // Call the createDiscordRole method with a callback function that will print the role ID when it is available
                            String finalValue = value, finalColumn = column;
                            TDCManager.createDiscordRole(town, roleId -> {
                                if (roleId != null) {
                                    System.out.println("Role created with ID: " + roleId);

                                    // Creating TownRole Entry with townRoleName and Role ID to SQL TABLE tdc_towns
                                    Map<String,String> keyPair = new HashMap<>();
                                    keyPair.put(finalColumn, finalValue);
                                    keyPair.put("townRoleId", roleId);
                                    new SQL().createEntry(finalValue, finalColumn, SQL.TABLE_TOWN_ROLES, keyPair, false);

                                    // Remove From Queue Once DiscordSRV Action Completed
                                    new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS);
                                } else {
                                    System.out.println("Failed to create role.");
                                }
                            });
                        }
                        // Nation Role Creation
                        case "nationRoleName" -> {
                            Bukkit.broadcastMessage("Creating Nation Role in Discord");

                            if (TimeDifference(dateTime) < Main.plugin.config.getInt("Discord.Api.RateLimit.Roles") * 2L) { break; }

                            Nation nation = TownyUniverse.getInstance().getNation(value);
                            assert nation != null;
                            // Call the createDiscordRole method with a callback function that will print the role ID when it is available
                            String finalValue = value, finalColumn = column;
                            TDCManager.createDiscordRole(nation, roleId -> {
                                if (roleId != null) {
                                    System.out.println("Role created with ID: " + roleId);

                                    // Creating NationRole Entry with nationRoleName and Role ID to SQL TABLE tdc_nations
                                    Map<String,String> keyPair = new HashMap<>();
                                    keyPair.put(finalColumn, finalValue);
                                    keyPair.put("nationRoleId", roleId);
                                    new SQL().createEntry(finalValue, finalColumn, SQL.TABLE_NATION_ROLES, keyPair, false);

                                    // Remove From Queue Once DiscordSRV Action Completed
                                    new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS);
                                } else {
                                    System.out.println("Failed to create role.");
                                }
                            });
                        }
                        // Town Category Creation
                        case "townCategoryName" -> {
                            Bukkit.broadcastMessage("Creating Town Category in Discord");

                            if (TimeDifference(dateTime) < Main.plugin.config.getInt("Discord.Api.RateLimit.Categories") * 2L) { break; }

                            // Call the createCategory method with a callback function that will print the category ID when it is available
                            String finalValue = value, finalColumn = column;
                            TDCManager.createChatCategory(value, categoryId -> {
                                if (categoryId != null) {
                                    System.out.println("Category created with ID: " + categoryId);

                                    // Creating TownCategory Entry with townCategoryName and Category ID to SQL TABLE tdc_town_categories
                                    Map<String,String> keyPair = new HashMap<>();
                                    keyPair.put(finalColumn, finalValue);
                                    keyPair.put("townCategoryId", categoryId);
                                    new SQL().createEntry("0", finalColumn, SQL.TABLE_TOWN_CATEGORIES, keyPair, false);

                                    // Remove From Queue Once DiscordSRV Action Completed
                                    new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS);
                                } else {
                                    System.out.println("Failed to create category.");
                                }
                            });
                        }
                        // Nation Category Creation
                        case "nationCategoryName" -> {
                            Bukkit.broadcastMessage("Creating Nation Category in Discord");

                            if (TimeDifference(dateTime) < Main.plugin.config.getInt("Discord.Api.RateLimit.Categories") * 2L) { break; }

                            // Call the createCategory method with a callback function that will print the category ID when it is available
                            String finalValue = value, finalColumn = column;
                            TDCManager.createChatCategory(value, categoryId -> {
                                if (categoryId != null) {
                                    System.out.println("Category created with ID: " + categoryId);

                                    // Creating NationCategory Entry with nationCategoryName and Category ID to SQL TABLE tdc_nation_categories
                                    Map<String,String> keyPair = new HashMap<>();
                                    keyPair.put(finalColumn, finalValue);
                                    keyPair.put("nationCategoryId", categoryId);
                                    new SQL().createEntry("0", finalColumn, SQL.TABLE_NATION_CATEGORIES, keyPair, false);

                                    // Remove From Queue Once DiscordSRV Action Completed
                                    new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS);
                                } else {
                                    System.out.println("Failed to create category.");
                                }
                            });
                        }
                        // Town TextChannel Creation
                        case "townTextChannelName" -> {
                            if (TimeDifference(dateTime) < Main.plugin.config.getInt("Discord.Api.RateLimit.Channels") * 4L) { break; }

                            String townRoleId = new SQL().getEntry("townRoleId", value, "townRoleName", SQL.TABLE_TOWN_ROLES);
                            if (townRoleId == null) { new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS); break; }

                            List<String> townCategoryIds = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Categories.Town.SQL.Id().GetAllCategoryIds();
                            if (townCategoryIds.isEmpty()) { new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS); break; }

                            int numTownTextChannels = 0;
                            for (String townCategoryId : townCategoryIds) {
                                numTownTextChannels += new com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Text.Towns.SQL.Name().GetAllTownTextChannelNames(townCategoryId).size();
                            }
                            if (numTownTextChannels >= Main.plugin.config.getInt("category.SubChannelsMax") * townCategoryIds.size() / 2) { new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS); break; }

                            String townCategory = null;
                            for (String townCategoryId : townCategoryIds) {
                                if (new com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Text.Towns.SQL.Name().GetAllTownTextChannelNames(townCategoryId).size() < Main.plugin.config.getInt("category.SubChannelsMax") / 2) {
                                    townCategory = townCategoryId; break;
                                }
                            }

                            Bukkit.broadcastMessage("Creating Town TextChannel in Discord");

                            // Call the createTownTextChannel method with a callback function that will print the townTextChannel ID when it is available
                            String finalValue = value, finalColumn = column, finalTownCategory = townCategory;
                            TDCManager.createTownTextChannel(value, townRoleId, townCategory, townTextChannelId -> {
                                if (townTextChannelId != null) {
                                    System.out.println("TextChannel created with ID: " + townTextChannelId);

                                    // Creating townTextChannel Entry with townTextChannelName and townTextChannel ID to SQL TABLE tdc_town_text_channels
                                    Map<String,String> keyPair = new HashMap<>();
                                    keyPair.put("townRoleName", finalValue);
                                    keyPair.put(finalColumn, finalValue);
                                    keyPair.put("townTextChannelId", townTextChannelId);
                                    keyPair.put("townTextChannelParentCategoryName", Main.plugin.config.getString("category.Town.Name"));
                                    keyPair.put("townTextChannelParentCategoryId", finalTownCategory);
                                    new SQL().createEntry(finalValue, finalColumn, SQL.TABLE_TOWN_TEXT_CHANNELS, keyPair, false);

                                    // Remove From Queue Once DiscordSRV Action Completed
                                    new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS);
                                } else {
                                    System.out.println("Failed to create Town TextChannel.");
                                }
                            });
                        }
                        // Nation TextChannel Creation
                        case "nationTextChannelName" -> {
                            if (TimeDifference(dateTime) < Main.plugin.config.getInt("Discord.Api.RateLimit.Channels") * 4L) { break; }

                            String nationRoleId = new SQL().getEntry("nationRoleId", value, "nationRoleName", SQL.TABLE_NATION_ROLES);
                            if (nationRoleId == null) { new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS); break; }

                            List<String> nationCategoryIds = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Categories.Nation.SQL.Id().GetAllCategoryIds();
                            if (nationCategoryIds.isEmpty()) { new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS); break; }

                            int numNationTextChannels = 0;
                            for (String nationCategoryId : nationCategoryIds) {
                                numNationTextChannels += new com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Text.Nations.SQL.Name().GetAllNationTextChannelNames(nationCategoryId).size();
                            }
                            if (numNationTextChannels >= Main.plugin.config.getInt("category.SubChannelsMax") * nationCategoryIds.size() / 2) { new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS); break; }

                            String nationCategory = null;
                            for (String nationCategoryId : nationCategoryIds) {
                                if (new com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Text.Nations.SQL.Name().GetAllNationTextChannelNames(nationCategoryId).size() < Main.plugin.config.getInt("category.SubChannelsMax") / 2) {
                                    nationCategory = nationCategoryId; break;
                                }
                            }

                            Bukkit.broadcastMessage("Creating Nation TextChannel in Discord");

                            // Call the createNationTextChannel method with a callback function that will print the nationTextChannel ID when it is available
                            String finalValue = value, finalColumn = column, finalNationCategory = nationCategory;
                            TDCManager.createNationTextChannel(value, nationRoleId, nationCategory, nationTextChannelId -> {
                                if (nationTextChannelId != null) {
                                    System.out.println("TextChannel created with ID: " + nationTextChannelId);

                                    // Creating nationTextChannel Entry with nationTextChannelName and nationTextChannel ID to SQL TABLE tdc_nation_text_channels
                                    Map<String,String> keyPair = new HashMap<>();
                                    keyPair.put("nationRoleName", finalValue);
                                    keyPair.put(finalColumn, finalValue);
                                    keyPair.put("nationTextChannelId", nationTextChannelId);
                                    keyPair.put("nationTextChannelParentCategoryName", Main.plugin.config.getString("category.Nation.Name"));
                                    keyPair.put("nationTextChannelParentCategoryId", finalNationCategory);
                                    new SQL().createEntry(finalValue, finalColumn, SQL.TABLE_NATION_TEXT_CHANNELS, keyPair, false);

                                    // Remove From Queue Once DiscordSRV Action Completed
                                    new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS);
                                } else {
                                    System.out.println("Failed to create Nation TextChannel.");
                                }
                            });
                        }
                        // Town VoiceChannel Creation
                        case "townVoiceChannelName" -> {
                            if (TimeDifference(dateTime) < Main.plugin.config.getInt("Discord.Api.RateLimit.Channels") * 4L) { break; }

                            String townRoleId = new SQL().getEntry("townRoleId", value, "townRoleName", SQL.TABLE_TOWN_ROLES);
                            if (townRoleId == null) { new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS); break; }

                            List<String> townCategoryIds = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Categories.Town.SQL.Id().GetAllCategoryIds();
                            if (townCategoryIds.isEmpty()) { new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS); break; }

                            int numTownVoiceChannels = 0;
                            for (String townCategoryId : townCategoryIds) {
                                numTownVoiceChannels += new com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Voice.Towns.SQL.Name().GetAllTownVoiceChannelNames(townCategoryId).size();
                            }
                            if (numTownVoiceChannels >= Main.plugin.config.getInt("category.SubChannelsMax") * townCategoryIds.size() / 2) { new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS); break; }

                            String townCategory = null;
                            for (String townCategoryId : townCategoryIds) {
                                if (new com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Voice.Towns.SQL.Name().GetAllTownVoiceChannelNames(townCategoryId).size() < Main.plugin.config.getInt("category.SubChannelsMax") / 2) {
                                    townCategory = townCategoryId; break;
                                }
                            }

                            Bukkit.broadcastMessage("Creating Town VoiceChannel in Discord");

                            // Call the createTownVoiceChannel method with a callback function that will print the townVoiceChannel ID when it is available
                            String finalValue = value, finalColumn = column, finalTownCategory = townCategory;
                            TDCManager.createTownVoiceChannel(value, townRoleId, townCategory, townVoiceChannelId -> {
                                if (townVoiceChannelId != null) {
                                    System.out.println("VoiceChannel created with ID: " + townVoiceChannelId);

                                    // Creating townVoiceChannel Entry with townVoiceChannelName and townVoiceChannel ID to SQL TABLE tdc_town_voice_channels
                                    Map<String,String> keyPair = new HashMap<>();
                                    keyPair.put("townRoleName", finalValue);
                                    keyPair.put(finalColumn, finalValue);
                                    keyPair.put("townVoiceChannelId", townVoiceChannelId);
                                    keyPair.put("townVoiceChannelParentCategoryName", Main.plugin.config.getString("category.Town.Name"));
                                    keyPair.put("townVoiceChannelParentCategoryId", finalTownCategory);
                                    new SQL().createEntry(finalValue, finalColumn, SQL.TABLE_TOWN_VOICE_CHANNELS, keyPair, false);

                                    // Remove From Queue Once DiscordSRV Action Completed
                                    new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS);
                                } else {
                                    System.out.println("Failed to create Town VoiceChannel.");
                                }
                            });
                        }
                        // Nation VoiceChannel Creation
                        case "nationVoiceChannelName" -> {
                            if (TimeDifference(dateTime) < Main.plugin.config.getInt("Discord.Api.RateLimit.Channels") * 4L) { break; }

                            String nationRoleId = new SQL().getEntry("nationRoleId", value, "nationRoleName", SQL.TABLE_NATION_ROLES);
                            if (nationRoleId == null) { new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS); break; }

                            List<String> nationCategoryIds = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Categories.Nation.SQL.Id().GetAllCategoryIds();
                            if (nationCategoryIds.isEmpty()) { new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS); break; }

                            int numNationVoiceChannels = 0;
                            for (String nationCategoryId : nationCategoryIds) {
                                numNationVoiceChannels += new com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Voice.Nations.SQL.Name().GetAllNationVoiceChannelNames(nationCategoryId).size();
                            }
                            if (numNationVoiceChannels >= Main.plugin.config.getInt("category.SubChannelsMax") * nationCategoryIds.size() / 2) { new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS); break; }

                            String nationCategory = null;
                            for (String nationCategoryId : nationCategoryIds) {
                                if (new com.TownyDiscordChat.TownyDiscordChat.Tasks.Channels.Voice.Nations.SQL.Name().GetAllNationVoiceChannelNames(nationCategoryId).size() < Main.plugin.config.getInt("category.SubChannelsMax") / 2) {
                                    nationCategory = nationCategoryId; break;
                                }
                            }

                            Bukkit.broadcastMessage("Creating Nation VoiceChannel in Discord");

                            // Call the createNationVoiceChannel method with a callback function that will print the nationVoiceChannel ID when it is available
                            String finalValue = value, finalColumn = column, finalNationCategory = nationCategory;
                            TDCManager.createNationVoiceChannel(value, nationRoleId, nationCategory, nationVoiceChannelId -> {
                                if (nationVoiceChannelId != null) {
                                    System.out.println("VoiceChannel created with ID: " + nationVoiceChannelId);

                                    // Creating nationVoiceChannel Entry with nationVoiceChannelName and nationVoiceChannel ID to SQL TABLE tdc_nation_voice_channels
                                    Map<String,String> keyPair = new HashMap<>();
                                    keyPair.put("nationRoleName", finalValue);
                                    keyPair.put(finalColumn, finalValue);
                                    keyPair.put("nationVoiceChannelId", nationVoiceChannelId);
                                    keyPair.put("nationVoiceChannelParentCategoryName", Main.plugin.config.getString("category.Nation.Name"));
                                    keyPair.put("nationVoiceChannelParentCategoryId", finalNationCategory);
                                    new SQL().createEntry(finalValue, finalColumn, SQL.TABLE_NATION_VOICE_CHANNELS, keyPair, false);

                                    // Remove From Queue Once DiscordSRV Action Completed
                                    new SQL().deleteEntry(timeValue, timeCol, SQL.TABLE_QUEUED_TASKS);
                                } else {
                                    System.out.println("Failed to create Nation VoiceChannel.");
                                }
                            });
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * startDelaySeconds /*<-- the initial delay */, 20L * intervalDelaySeconds /*<-- the interval */);
        // Ticks > Seconds > Minutes > Hours > Days
    }

    private long TimeDifference(LocalDateTime pastDateTime) {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(pastDateTime, now);
        long seconds = duration.getSeconds();
        System.out.println("Time difference in seconds: " + seconds);
        return seconds;
    }

    public void cancel() {
        this.scheduler.cancel();
    }
}
