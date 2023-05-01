package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Categories;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Interfaces.SQL;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.QueuedTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class Nations {

    private final Main plugin;

    private final BukkitTask scheduler;

    public Nations (long startDelaySeconds, long intervalDelaySeconds, Main plugin) {

        this.plugin = plugin;
        // With BukkitRunnable
        scheduler = new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("Nation Category Scheduler Running!");

                String nationCategoryName = Main.plugin.config.getString("category.Nation.Name");
                assert nationCategoryName != null;

                List<String> SQLRoles = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Roles.Nations.SQL.Name().GetAllNationRoleNames();
                List<String> SQLCategories = new com.TownyDiscordChat.TownyDiscordChat.Tasks.Categories.Nation.SQL.Name().GetAllCategoryNames();
                List<String> nationCategoryQueue = SQL.getAllEntriesWhereEqual("nationCategoryName", "nationCategoryName", nationCategoryName, SQL.TABLE_QUEUED_TASKS);

                int roles = SQLRoles.size();
                int categories = SQLCategories.size();
                int categoriesQueue = nationCategoryQueue.size();

                if (SQLCategories.isEmpty()) {
                    new QueuedTask(plugin).createNationCategory(nationCategoryName);
                }
                // if total text channels is more than or equal to half sub channels
                else if (roles > Main.plugin.config.getInt("category.SubChannelsMax") * (categories + categoriesQueue) / 2) {
                    new QueuedTask(plugin).createNationCategory(nationCategoryName);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * startDelaySeconds /*<-- the initial delay */, 20L * intervalDelaySeconds /*<-- the interval */);
        // Ticks > Seconds > Minutes > Hours > Days
    }

    public void cancel() {
        this.scheduler.cancel();
    }
}