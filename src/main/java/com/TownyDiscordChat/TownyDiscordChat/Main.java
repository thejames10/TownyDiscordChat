package com.TownyDiscordChat.TownyDiscordChat;

import com.TownyDiscordChat.TownyDiscordChat.Commands.TDCCommand;
import com.TownyDiscordChat.TownyDiscordChat.Commands.TDCSQL;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.MySQL;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.*;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Log.Log;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Players.Players;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Queue.Queue;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Reconciler;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.SQLException;
import java.util.*;

public class Main extends JavaPlugin {

    public MySQL SQL;
    public LogCategory logCategoryDB;
    public LogTextChannel logTextChannelDB;
    public com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.Players playersDB;
    public TownRoles townRolesDB;
    public NationRoles nationRolesDB;
    public TownCategories townCategoriesDB;
    public NationCategories nationCategoriesDB;
    public TownTextChannels townTextChannelsDB;
    public TownVoiceChannels townVoiceChannelsDB;
    public NationTextChannels nationTextChannelsDB;
    public NationVoiceChannels nationVoiceChannelsDB;
    public QueuedTask queuedTaskDB;
    public Log logScheduler;
    public Players playersScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Categories.Towns townCategoriesScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Categories.Nations nationCategoriesScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Roles.Nations nationRolesScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Roles.Towns townRolesScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Channels.Text.Towns townTextChannelsScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Channels.Text.Nations nationTextChannelsScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Channels.Voice.Towns townVoiceChannelsScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Channels.Voice.Nations nationVoiceChannelsScheduler;
    public Queue queueScheduler;
    public Reconciler reconciler;

    public static Main plugin;

    public FileConfiguration config = getConfig();

    public void onEnable() {
        int pluginId = 10980;
        new Metrics(this, pluginId);

        saveDefaultConfig();
        reloadConfig();
        saveConfig();
        config = getConfig();

        Objects.requireNonNull(getCommand("TownyDiscordChat")).setExecutor(new TDCCommand());
        Objects.requireNonNull(getCommand("tdcqueue")).setExecutor(new TDCSQL());
        plugin = this;

        this.SQL = new MySQL();
        this.logCategoryDB = new LogCategory(this);
        this.logTextChannelDB = new LogTextChannel(this);
        this.playersDB = new com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.Players(this);
        this.townRolesDB = new TownRoles(this);
        this.nationRolesDB = new NationRoles(this);
        this.townCategoriesDB = new TownCategories(this);
        this.nationCategoriesDB = new NationCategories(this);
        this.townTextChannelsDB = new TownTextChannels(this);
        this.townVoiceChannelsDB = new TownVoiceChannels(this);
        this.nationTextChannelsDB = new NationTextChannels(this);
        this.nationVoiceChannelsDB = new NationVoiceChannels(this);
        this.queuedTaskDB = new QueuedTask(this);

        try {
            SQL.connect();
        } catch (ClassNotFoundException | SQLException e) {
            getLogger().severe("Database not connected: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            if (SQL != null && SQL.isConnected()) {
                getLogger().info("Database is connected!");
                logCategoryDB.createTable();
                logTextChannelDB.createTable();
                playersDB.createTable();
                townRolesDB.createTable();
                nationRolesDB.createTable();
                townCategoriesDB.createTable();
                nationCategoriesDB.createTable();
                townTextChannelsDB.createTable();
                townVoiceChannelsDB.createTable();
                nationTextChannelsDB.createTable();
                nationVoiceChannelsDB.createTable();
                queuedTaskDB.createTable();

                // Do not wait synchronously for DiscordSRV to connect.
                // Reconciler will start once the guild is available.
                this.reconciler = new Reconciler(this);
                this.queueScheduler = new Queue(
                        config.getLong("queue.startDelaySeconds", 10L),
                        config.getLong("queue.pollSeconds", 5L), this);
            }
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize queue: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Legacy listeners issue direct Discord calls outside the queue; keep disabled
        // until their mutations have been converted into durable job types.
        //DiscordSRV.api.subscribe(new TDCDiscordSRVListener());
        getLogger().info("TownyDiscordChat has been Enabled!");
    }

    public void onDisable() {

        if (queueScheduler != null) queueScheduler.cancel();
        if (reconciler != null) reconciler.cancel();

        try {
            if (SQL.isConnected()) {
                SQL.disconnect();
                getLogger().info("Database is disconnected!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getLogger().info("TownyDiscordChat has been Disabled!");
    }
}