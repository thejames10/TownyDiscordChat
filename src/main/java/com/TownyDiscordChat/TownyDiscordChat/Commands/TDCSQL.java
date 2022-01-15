package com.TownyDiscordChat.TownyDiscordChat.Commands;

import com.TownyDiscordChat.TownyDiscordChat.Main;
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

        if (args.length == 0) {

            boolean isConnected = false;
            try {
                Main.plugin.SQL.checkSQLConnection();
                isConnected = Main.plugin.SQL.isConnected();
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            player.sendMessage("isConnected? " + isConnected);

            Main.plugin.playersDB.deleteEntry("Testing", "discordUserId");

            Main.plugin.playersDB.createEntry("Testing","Testing",
                    "Testing", "Testing","Testing",
                    "Testing", "Testing","Testing","false","false");

        }

        if (args.length == 1) {

            String oldValue = Main.plugin.playersDB.getEntry("UUID", "Testing", "discordUserId");
            Main.plugin.playersDB.updateEntry(args[0], "UUID", "Testing", "discordUserId");

            player.sendMessage("Changed " + oldValue + " to " + args[0] + " !");
        }

        return true;
    }
}
