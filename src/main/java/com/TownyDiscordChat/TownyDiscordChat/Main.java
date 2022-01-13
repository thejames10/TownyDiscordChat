package com.TownyDiscordChat.TownyDiscordChat;

import com.TownyDiscordChat.TownyDiscordChat.Listeners.TDCDiscordSRVListener;
import com.TownyDiscordChat.TownyDiscordChat.Listeners.TDCTownyListener;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.MySQL;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.SQLGetter;
import github.scarsz.discordsrv.DiscordSRV;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.SQLException;
import java.util.Objects;

public class Main extends JavaPlugin {

    public MySQL SQL;
    public SQLGetter data;

    public static Main plugin;

    public FileConfiguration config = getConfig();

    public void onEnable() {
        int pluginId = 10980;
        new Metrics(this, pluginId);

        this.SQL = new MySQL();
        this.data = new SQLGetter(this);

        try {
            SQL.connect();
        } catch (ClassNotFoundException | SQLException e) {
            getLogger().info("Database not connected");
        }

        try {
            if (SQL.isConnected()) {
                getLogger().info("Database is connected!");
                data.createTable();
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

        new TDCTownyListener(plugin);
        DiscordSRV.api.subscribe(new TDCDiscordSRVListener());
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