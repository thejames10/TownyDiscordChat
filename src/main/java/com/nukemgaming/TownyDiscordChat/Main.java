package com.nukemgaming.TownyDiscordChat;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

public class Main extends JavaPlugin {

    public static Main plugin;

    public FileConfiguration config = getConfig();

    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        saveConfig();
        config = getConfig();
        
        Objects.requireNonNull(getCommand("TownyDiscordChat")).setExecutor(new TDCCommand());
        getLogger().info("TownyDiscordChat has been Enabled!");
        plugin = this;
        new TDCListener(plugin);

        // This is a test
    }

    public void onDisable() {
        getLogger().info("TownyDiscordChat has been Disabled!");
    }
}