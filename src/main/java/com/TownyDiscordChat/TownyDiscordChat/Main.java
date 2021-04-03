package com.TownyDiscordChat.TownyDiscordChat;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
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
    }

    public void onDisable() {
        getLogger().info("TownyDiscordChat has been Disabled!");
    }
}