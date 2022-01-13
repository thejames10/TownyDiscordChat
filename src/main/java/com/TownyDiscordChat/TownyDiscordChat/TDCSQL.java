package com.TownyDiscordChat.TownyDiscordChat;

import com.TownyDiscordChat.TownyDiscordChat.MySQL.SQLGetter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class TDCSQL implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull final CommandSender commandSender, @NotNull final Command command, @NotNull final String label,
                             @NotNull final String[] args) {

        Player player = (Player) commandSender;

        boolean isConnected = false;
        try {
            Main.plugin.SQL.checkSQLConnection();
            isConnected = Main.plugin.SQL.isConnected();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        player.sendMessage("isConnected? " + isConnected);

        //Main.plugin.data.createEntry("Testing","Testing","Testing");

        Main.plugin.data.updateRoleId("Testing", "Replaced lol");

        return true;
    }
}
