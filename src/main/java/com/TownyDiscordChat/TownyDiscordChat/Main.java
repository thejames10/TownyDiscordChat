package com.TownyDiscordChat.TownyDiscordChat;

import com.TownyDiscordChat.TownyDiscordChat.Commands.TDCCommand;
import com.TownyDiscordChat.TownyDiscordChat.Commands.TDCSQL;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.MySQL;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.*;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Log.Log;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Players.Players;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Queue.Queue;
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
        Objects.requireNonNull(getCommand("SQL")).setExecutor(new TDCSQL());
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
            getLogger().info("Database not connected");
        }

        try {
            if (SQL.isConnected()) {
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

                Guild guild = null;
                do {
                    try {
                        guild = DiscordSRV.getPlugin().getMainGuild();
                    } catch (Exception e) { }
                } while (guild == null);

                this.logScheduler = new Log(this);
                this.playersScheduler = new Players(this);

                this.townRolesScheduler = new com.TownyDiscordChat.TownyDiscordChat.Schedulers.Roles.Towns(15, 60, this);
                this.nationRolesScheduler = new com.TownyDiscordChat.TownyDiscordChat.Schedulers.Roles.Nations(15, 60, this);
                this.townCategoriesScheduler = new com.TownyDiscordChat.TownyDiscordChat.Schedulers.Categories.Towns(30, 75, this);
                this.nationCategoriesScheduler = new com.TownyDiscordChat.TownyDiscordChat.Schedulers.Categories.Nations(30, 75, this);
                this.townTextChannelsScheduler = new com.TownyDiscordChat.TownyDiscordChat.Schedulers.Channels.Text.Towns(45, 90, this);
                this.nationTextChannelsScheduler = new com.TownyDiscordChat.TownyDiscordChat.Schedulers.Channels.Text.Nations(45, 90, this);
                this.townVoiceChannelsScheduler = new com.TownyDiscordChat.TownyDiscordChat.Schedulers.Channels.Voice.Towns(60, 105, this);
                this.nationVoiceChannelsScheduler = new com.TownyDiscordChat.TownyDiscordChat.Schedulers.Channels.Voice.Nations(60, 105, this);

                this.queueScheduler = new Queue(75, 120, this);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //new TDCTownyListener(plugin); // Disabling Towny Listener for now
        //DiscordSRV.api.subscribe(new TDCDiscordSRVListener()); // Disabling DiscordSRV Listener for now
        getLogger().info("TownyDiscordChat has been Enabled!");
    }

    public void onDisable() {

        this.queueScheduler.cancel();
        this.nationVoiceChannelsScheduler.cancel();
        this.townVoiceChannelsScheduler.cancel();
        this.nationTextChannelsScheduler.cancel();
        this.townTextChannelsScheduler.cancel();
        this.nationCategoriesScheduler.cancel();
        this.townCategoriesScheduler.cancel();
        this.nationRolesScheduler.cancel();
        this.townRolesScheduler.cancel();

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