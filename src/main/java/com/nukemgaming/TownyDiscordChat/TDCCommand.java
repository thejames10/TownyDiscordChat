package com.nukemgaming.TownyDiscordChat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;

public class TDCCommand implements CommandExecutor {

    AccountLinkManager manager = DiscordSRV.getPlugin().getAccountLinkManager();

    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label,
            @NotNull final String[] args) {
        if (!(sender instanceof Player))
            return true;

        // creating instance of player
        final Player player = (Player) sender;

        TDCManager.givePlayerTownRole(player);
        TDCManager.givePlayerNationRole(player);
        
        return true;
    }
}