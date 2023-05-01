package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Categories;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.QueuedTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class Towns {

    private final Main plugin;

    private final BukkitTask scheduler;

    public Towns (long startDelaySeconds, long intervalDelaySeconds, Main plugin) {

        this.plugin = plugin;
        // With BukkitRunnable
        scheduler = new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("Town Category Scheduler Running!");

                String townCategoryName = Main.plugin.config.getString("category.Town.Name");
                assert townCategoryName != null;

                List<String> SQLRoles = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Roles.Towns.SQL.Name().GetAllTownRoleNames();
                List<String> SQLCategories = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Categories.Town.SQL.Name().GetAllCategoryNames();
                List<String> townCategoryQueue = SQL.getAllEntriesWhereEqual("townCategoryName", "townCategoryName", townCategoryName, SQL.TABLE_QUEUED_TASKS);

                int roles = SQLRoles.size();
                int categories = SQLCategories.size();
                int categoriesQueue = townCategoryQueue.size();

                if (SQLCategories.isEmpty()) {
                    new QueuedTask(plugin).createTownCategory(townCategoryName);
                }
                // if total text channels is more than or equal to half sub channels
                else if (roles > Main.plugin.config.getInt("category.SubChannelsMax") * (categories + categoriesQueue) / 2) {
                    new QueuedTask(plugin).createTownCategory(townCategoryName);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * startDelaySeconds /*<-- the initial delay */, 20L * intervalDelaySeconds /*<-- the interval */);
        // Ticks > Seconds > Minutes > Hours > Days
    }

    public void cancel() {
        this.scheduler.cancel();
    }
}