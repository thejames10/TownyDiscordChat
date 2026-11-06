package com.TownyDiscordChat.TownyDiscordChat.Commands;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.Queue.DiscordJobStore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import java.sql.SQLException;
import java.util.Map;

public class TDCSQL implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull final CommandSender sender,
                             @NotNull final Command command, @NotNull final String label,
                             @NotNull final String[] args) {
        if (!sender.hasPermission("TownyDiscordChat.Admin")) {
            sender.sendMessage("No permission (TownyDiscordChat.Admin).");
            return true;
        }
        if (args.length > 0 && !args[0].equalsIgnoreCase("status")
                && !args[0].equalsIgnoreCase("retry-dead")) {
            sender.sendMessage("Usage: /tdcqueue [status|retry-dead]");
            return true;
        }
        final boolean retry = args.length > 0 && args[0].equalsIgnoreCase("retry-dead");
        final Main plugin = Main.plugin;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override public void run() {
                final String message;
                try {
                    DiscordJobStore jobs = new DiscordJobStore(plugin);
                    if (retry) {
                        message = "Requeued " + jobs.retryDead() + " dead Discord jobs.";
                    } else {
                        Map<String, Integer> counts = jobs.statusCounts();
                        message = "Discord jobs: pending=" + counts.getOrDefault("PENDING", 0)
                                + ", running=" + counts.getOrDefault("RUNNING", 0)
                                + ", dead=" + counts.getOrDefault("DEAD", 0);
                    }
                } catch (SQLException e) {
                    plugin.getLogger().warning("Queue admin request failed: " + e.getMessage());
                    sendOnMain(plugin, sender, "Could not query MySQL queue. See server log.");
                    return;
                }
                sendOnMain(plugin, sender, message);
            }
        });
        return true;
    }

    private static void sendOnMain(Main plugin, CommandSender sender, String message) {
        if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(message));
    }
}
