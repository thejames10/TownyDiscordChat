package com.TownyDiscordChat.TownyDiscordChat;

import com.google.common.base.Preconditions;
import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TDCCommand implements CommandExecutor {

    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label,
                             @NotNull final String[] args) {
        if (!(sender instanceof Player))
            return true;

        if (args.length == 0) {
            // No arguments were provided, just "/TownyDiscordChat | /tdc"
            return true;
        }

        Player player = ((Player) sender).getPlayer();

        Preconditions.checkNotNull(player);


        if (args.length >= 1) {
            // Some arguments were provided
            if (args[0].equalsIgnoreCase("Check")) {
                // The first argument is "Check", therefore "/<base> Check"
                if (args.length >= 2 && args[1].equalsIgnoreCase("Role")) {
                    // The first argument is "Check" and second is "Role", therefore "/<base> Check Role"
                    if(args.length >= 3 && args[2].equalsIgnoreCase("AllLinked")) {
                        // The first argument is "Check" and second is "Role" and third is "AllLinked", therefore "/<base> Check Role AllLinked
                        // /TDC Check Role AllLinked command
                        if (sender.hasPermission("TownyDiscordChat.Admin") || sender.hasPermission("TownyDiscordChat.Check.Role.AllLinked")) {
                            TDCManager.discordUserRoleCheckAllLinked();
                            player.sendMessage("Check Discord for updated roles!");
                        } else {
                            player.sendMessage("You don't have permission to use this command!");
                        }
                        return true;
                    } else if (args.length >= 3 && args[2].equalsIgnoreCase("CreateAllTownsAndNations")) {
                        // The first argument is "Check" and second is "Role" and third is "CreateAllTownsAndNations", therefore "/<base> Check Role CreateAllTownsAndNations
                        // /TDC Check Role CreateAllTownsAndNations command
                        if (sender.hasPermission("TownyDiscordChat.Admin") || sender.hasPermission("TownyDiscordChat.Check.Role.CreateAllTownsAndNations")) {
                            TDCManager.discordRoleCheckAllTownsAllNations();
                            player.sendMessage("Check Discord for updated roles!");
                        } else {
                            player.sendMessage("You don't have permission to use this command!");
                        }
                        return true;
                    }
                    // /TDC Check Role command
                    if (sender.hasPermission("TownyDiscordChat.Admin") || sender.hasPermission("TownyDiscordChat.Player") || sender.hasPermission("TownyDiscordChat.Check.Role")) {
                        UUID UUID = player.getUniqueId();
                        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(UUID);

                        Preconditions.checkNotNull(UUID, "UUID null in onCommand()!");
                        Preconditions.checkNotNull(UUID, "discordId null in onCommand()!");

                        TDCManager.discordUserRoleCheck(discordId, UUID);
                        player.sendMessage("Check Discord for updated role!");
                    }
                    else {
                        player.sendMessage("You don't have permission to use this command!");
                    }
                    return true;
                } else if (args.length >= 2 && args[1].equalsIgnoreCase("TextChannel")) {
                    // The first argument is "Check" and second is "TextChannel", therefore "/<base> Check TextChannel"
                    if (args.length >= 3 && args[2].equalsIgnoreCase("AllTownsAndNations")) {
                        // The first argument is "Check" and second is "TextChannel" and third is "AllTownsAndNations", therefore "/<base> Check TextChannel AllTownsAndNations
                        // /TDC Check TextChannel AllTownsAndNations command
                        if (sender.hasPermission("TownyDiscordChat.Admin") || sender.hasPermission("TownyDiscordChat.Check.TextChannel.AllTownsAndNations")) {
                            TDCManager.discordTextChannelCheckAllTownsAllNations();
                            player.sendMessage("Check Discord for updated roles!");
                        }
                        else {
                            player.sendMessage("You don't have permission to use this command!");
                        }
                        return true;
                    }
                    return true;
                } else if (args.length >= 2 && args[1].equalsIgnoreCase("VoiceChannel")) {
                    // The first argument is "Check" and second is "VoiceChannel", therefore "/<base> Check VoiceChannel"
                    if (args.length >= 3 && args[2].equalsIgnoreCase("AllTownsAndNations")) {
                        // The first argument is "Check" and second is "VoiceChannel" and third is "AllTownsAndNations", therefore "/<base> Check VoiceChannel AllTownsAndNations
                        // /TDC Check VoiceChannel AllTownsAndNations command
                        if (sender.hasPermission("TownyDiscordChat.Admin") || sender.hasPermission("TownyDiscordChat.Check.VoiceChannel.AllTownsAndNations")) {
                            TDCManager.discordVoiceChannelCheckAllTownsAllNations();
                            player.sendMessage("Check Discord for updated roles!");
                        } else {
                            player.sendMessage("You don't have permission to use this command!");
                        }
                        return true;
                    }
                    return true;
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("baz")) {
                // The first argument is "baz", therefore "/foo baz"
                return true;
            }
        }

        return true;
    }
}