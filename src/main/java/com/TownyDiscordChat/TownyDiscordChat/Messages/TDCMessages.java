package com.TownyDiscordChat.TownyDiscordChat.Messages;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * In-game command feedback only. The old Discord log/DM APIs were removed:
 * they bypassed the durable queue and could hit Discord rate limits.
 */
public final class TDCMessages {
    private TDCMessages() { }

    public static void sendMessageToPlayerGame(Player player, String message) {
        if (player!=null && message!=null)
            player.sendMessage(getPluginPrefix()+" "+message);
    }

    public static String getPluginPrefix() {
        return ChatColor.translateAlternateColorCodes('&',
                Main.plugin.config.getString("messages.Prefix","&2[TownyDiscordChat]"));
    }

    public static String getConfigMsgCommandsPleasewait() {
        return ChatColor.translateAlternateColorCodes('&',
                Main.plugin.config.getString("messages.Commands.PleaseWait",
                        "&7Reconciliation requested."));
    }

    public static String getConfigMsgCommandsNopermission() {
        return ChatColor.translateAlternateColorCodes('&',
                Main.plugin.config.getString("messages.Commands.NoPermission",
                        "&cYou do not have permission."));
    }
}
