package com.TownyDiscordChat.TownyDiscordChat;

import com.TownyDiscordChat.TownyDiscordChat.Commands.TDCCommand;
import com.TownyDiscordChat.TownyDiscordChat.Commands.TDCSQL;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.MySQL;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.*;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Players;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Queue;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.SQLException;
import java.util.*;

public class Main extends JavaPlugin {

    public MySQL SQL;
    public com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.Players playersDB;
    public Towns townsDB;
    public Nations nationsDB;
    public TownTextChannels townTextChannelsDB;
    public TownVoiceChannels townVoiceChannelsDB;
    public NationTextChannels nationTextChannelsDB;
    public NationVoiceChannels nationVoiceChannelsDB;
    public QueuedTask queuedTaskDB;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Nations.Nations nationsScheduler;
    public Players playersScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Towns.Towns townsScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Queue queueScheduler;

    public static Main plugin;

    public FileConfiguration config = getConfig();

    public void onEnable() {
        int pluginId = 10980;
        new Metrics(this, pluginId);

        this.SQL = new MySQL();
        this.playersDB = new com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.Players(this);
        this.townsDB = new Towns(this);
        this.nationsDB = new Nations(this);
        this.townTextChannelsDB = new TownTextChannels(this);
        this.townVoiceChannelsDB = new TownVoiceChannels(this);
        this.nationTextChannelsDB = new NationTextChannels(this);
        this.nationVoiceChannelsDB = new NationVoiceChannels(this);
        this.queuedTaskDB = new QueuedTask(this);
        this.nationsScheduler = new com.TownyDiscordChat.TownyDiscordChat.Schedulers.Nations.Nations(this);
        //this.playersScheduler = new Players(this);
        this.townsScheduler = new com.TownyDiscordChat.TownyDiscordChat.Schedulers.Towns.Towns(this);
        this.queueScheduler = new Queue(this);

        try {
            SQL.connect();
        } catch (ClassNotFoundException | SQLException e) {
            getLogger().info("Database not connected");
        }

        try {
            if (SQL.isConnected()) {
                getLogger().info("Database is connected!");
                playersDB.createTable();
                townsDB.createTable();
                nationsDB.createTable();
                townTextChannelsDB.createTable();
                townVoiceChannelsDB.createTable();
                nationTextChannelsDB.createTable();
                nationVoiceChannelsDB.createTable();
                queuedTaskDB.createTable();

                // Sync Existing Towns with TownsDB
                //  // Compare lists first then perform api calls to reduce overall api calls
                // Towny List
                //List<String> townyList = new ArrayList<>();
                //for (Town town : TownyUniverse.getInstance().getTowns()) {
                //    townyList.add(town.getName());
                //}
                // MySQL List
                //List<String> townsDBList = townsDB.getAllColumnEntries("townRoleName");
                // Differential List
                //List<String> differences = new ArrayList<>(townyList);
                //assert townsDBList != null;
                //differences.removeAll(townsDBList);
                //for (String townName : differences) {
                //    UUID UUID = Objects.requireNonNull(TownyUniverse.getInstance().getTown(townName)).getUUID();
                //    townsDB.createEntry(townName, UUID.toString());
                //}


                // Sync Existing Nations with NationsDB
                //  // Compare lists first then perform api calls to reduce overall api calls
                // Sync Existing Players with PlayersDB
                //  // Compare lists first then perform api calls to reduce overall api calls
                // Sync Existing TextChannels with TextChannelsDB
                //  // Compare lists first then perform api calls to reduce overall api calls
                // Sync Existing VoiceChannels with VoiceChannelsDB
                //  // Compare lists first then perform api calls to reduce overall api calls
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        saveDefaultConfig();
        reloadConfig();
        saveConfig();
        config = getConfig();

        Objects.requireNonNull(getCommand("TownyDiscordChat")).setExecutor(new TDCCommand());
        Objects.requireNonNull(getCommand("SQL")).setExecutor(new TDCSQL());
        getLogger().info("TownyDiscordChat has been Enabled!");
        plugin = this;

        //new TDCTownyListener(plugin); // Disabling Towny Listener for now
        //DiscordSRV.api.subscribe(new TDCDiscordSRVListener()); // Disabling DiscordSRV Listener for now
    }

    public void onDisable() {

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