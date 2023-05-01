package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Log;

import com.TownyDiscordChat.TownyDiscordChat.Core.TDCManager;
import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Log {

    private final Main plugin;

    private final BukkitTask scheduler;

    public Log (Main plugin) {

        this.plugin = plugin;
        // With BukkitRunnable
        scheduler = new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("Log Category Scheduler Running!");

                String logCategoryName = Main.plugin.config.getString("category.Admin.Name");

                List<String> LogCategory = SQL.getAllEntriesWhereEqual("logCategoryId", "logCategoryName", logCategoryName, SQL.TABLE_LOG_CATEGORY);

                if (LogCategory.isEmpty()) {
                    TDCManager.createChatCategory(logCategoryName, logCategoryId -> {
                        if (logCategoryId != null) {
                            System.out.println("logCategory created with ID: " + logCategoryId);

                            // Creating logCategory Entry with logCategoryName and logCategoryId to SQL TABLE tdc_log_category
                            Map<String,String> keyPair = new HashMap<>();
                            keyPair.put("logCategoryName", logCategoryName);
                            keyPair.put("logCategoryId", logCategoryId);
                            new SQL().createEntry(logCategoryName, "logCategoryName", SQL.TABLE_LOG_CATEGORY, keyPair, false);

                            String logTextChannelName = Main.plugin.config.getString("log.Channel.Name");

                            List<String> LogTextChannel = SQL.getAllEntriesWhereEqual("logTextChannelId", "logTextChannelName", logTextChannelName, SQL.TABLE_LOG_TEXT_CHANNEL);

                            if (LogTextChannel.isEmpty()) {
                                TDCManager.createLogTextChannel(logTextChannelName, logCategoryId, logTextChannelId -> {
                                    if (logTextChannelId != null) {
                                        System.out.println("logTextChannel created with ID: " + logTextChannelId);

                                        // Creating logTextChannel Entry with logTextChannelName and logTextChannelId to SQL TABLE tdc_log_text_channel
                                        Map<String,String> keyPairr = new HashMap<>();
                                        keyPairr.put("logTextChannelName", logTextChannelName);
                                        keyPairr.put("logTextChannelId", logTextChannelId);
                                        new SQL().createEntry(logTextChannelName, "logTextChannelName", SQL.TABLE_LOG_TEXT_CHANNEL, keyPairr, false);
                                    } else {
                                        System.out.println("Failed to create Log TextChannel.");
                                    }
                                });
                            }
                        } else {
                            System.out.println("Failed to create Log Category.");
                        }
                    });
                }
            }
        }.runTaskLater(plugin, 20L * 5L);
    }

    public void cancel() {
        this.scheduler.cancel();
    }
}