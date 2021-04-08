package com.TownyDiscordChat.TownyDiscordChat;

import com.TownyDiscordChat.TownyDiscordChat.Listeners.TDCDiscordSRVListener;
import com.TownyDiscordChat.TownyDiscordChat.Listeners.TDCTownyListener;
import github.scarsz.discordsrv.DiscordSRV;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

public class Main extends JavaPlugin {

    public static Main plugin;

    public FileConfiguration config = getConfig();

    public void onEnable() {
        int pluginId = 10980;
        Metrics metrics = new Metrics(this, pluginId);

        saveDefaultConfig();
        reloadConfig();
        saveConfig();
        config = getConfig();

        Objects.requireNonNull(getCommand("TownyDiscordChat")).setExecutor(new TDCCommand());
        getLogger().info("TownyDiscordChat has been Enabled!");
        plugin = this;

        new TDCTownyListener(plugin);
        DiscordSRV.api.subscribe(new TDCDiscordSRVListener());
    }

    public void onDisable() {
        getLogger().info("TownyDiscordChat has been Disabled!");
    }
}