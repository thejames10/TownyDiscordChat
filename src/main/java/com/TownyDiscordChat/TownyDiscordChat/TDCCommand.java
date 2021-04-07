package com.TownyDiscordChat.TownyDiscordChat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Timer;

public class TDCCommand implements CommandExecutor {

    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label,
                             @NotNull final String[] args) {
        if (!(sender instanceof Player))
            return true;

        return true;
    }
}