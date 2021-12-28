package com.TownyDiscordChat.TownyDiscordChat;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.google.common.base.Preconditions;
import org.bukkit.ChatColor;
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

        Player player = ((Player) sender).getPlayer();

        Preconditions.checkNotNull(player);

        if (args.length == 0) {
            // No arguments were provided, just "/TownyDiscordChat | /tdc"

            String msg = String.join("\n"
                    , ChatColor.DARK_GREEN + "------------------------"
                    , ChatColor.DARK_GREEN + "Plugin: " + ChatColor.GRAY + "TownyDiscordChat"
                    , ChatColor.DARK_GREEN + "Version: " + ChatColor.GRAY + "1.0.7"
                    , ChatColor.DARK_GREEN + "Authors: " + ChatColor.GRAY + "thejames10,Hugo5000"
                    , ChatColor.DARK_GREEN + "Root Cmd: " + ChatColor.GRAY + "/TownyDiscordChat"
                    , ChatColor.DARK_GREEN + "Alias: " + ChatColor.GRAY + "/TDC"
                    , ChatColor.DARK_GREEN + "---------------------------------");

            TDCMessages.sendMessageToPlayerGame(player,msg);

            return true;
        }

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
                            TDCMessages.sendMessageToPlayerGame(player, TDCMessages.getConfigMsgCommandsPleasewait());
                            TDCManager.discordUserRoleCheckAllLinked();
                        } else {
                            TDCMessages.sendMessageToPlayerGame(player, TDCMessages.getConfigMsgCommandsNopermission());
                        }
                        return true;
                    } else if (args.length >= 3 && args[2].equalsIgnoreCase("CreateAllTownsAndNations")) {
                        // The first argument is "Check" and second is "Role" and third is "CreateAllTownsAndNations", therefore "/<base> Check Role CreateAllTownsAndNations
                        // /TDC Check Role CreateAllTownsAndNations command
                        if (sender.hasPermission("TownyDiscordChat.Admin") || sender.hasPermission("TownyDiscordChat.Check.Role.CreateAllTownsAndNations")) {
                            TDCMessages.sendMessageToPlayerGame(player, TDCMessages.getConfigMsgCommandsPleasewait());
                            TDCManager.discordRoleCheckAllTownsAllNations();
                        } else {
                            TDCMessages.sendMessageToPlayerGame(player, TDCMessages.getConfigMsgCommandsNopermission());
                        }
                        return true;
                    }
                    // /TDC Check Role command
                    if (sender.hasPermission("TownyDiscordChat.Admin") || sender.hasPermission("TownyDiscordChat.Player") || sender.hasPermission("TownyDiscordChat.Check.Role")) {
                        UUID UUID = player.getUniqueId();
                        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(UUID);

                        Preconditions.checkNotNull(UUID, "UUID null in onCommand()!");
                        Preconditions.checkNotNull(UUID, "discordId null in onCommand()!");

                        TDCMessages.sendMessageToPlayerGame(player, TDCMessages.getConfigMsgCommandsPleasewait());
                        TDCManager.discordUserRoleCheck(discordId, UUID);
                    }
                    else {
                        TDCMessages.sendMessageToPlayerGame(player, TDCMessages.getConfigMsgCommandsNopermission());
                    }
                    return true;
                } else if (args.length >= 2 && args[1].equalsIgnoreCase("TextChannel")) {
                    // The first argument is "Check" and second is "TextChannel", therefore "/<base> Check TextChannel"
                    if (args.length >= 3 && args[2].equalsIgnoreCase("AllTownsAndNations")) {
                        // The first argument is "Check" and second is "TextChannel" and third is "AllTownsAndNations", therefore "/<base> Check TextChannel AllTownsAndNations
                        // /TDC Check TextChannel AllTownsAndNations command
                        if (sender.hasPermission("TownyDiscordChat.Admin") || sender.hasPermission("TownyDiscordChat.Check.TextChannel.AllTownsAndNations")) {
                            TDCMessages.sendMessageToPlayerGame(player, TDCMessages.getConfigMsgCommandsPleasewait());
                            TDCManager.discordTextChannelCheckAllTownsAllNations();
                        }
                        else {
                            TDCMessages.sendMessageToPlayerGame(player, TDCMessages.getConfigMsgCommandsNopermission());
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
                            TDCMessages.sendMessageToPlayerGame(player, TDCMessages.getConfigMsgCommandsPleasewait());
                            TDCManager.discordVoiceChannelCheckAllTownsAllNations();
                        } else {
                            TDCMessages.sendMessageToPlayerGame(player, TDCMessages.getConfigMsgCommandsNopermission());
                        }
                        return true;
                    }
                    return true;
                }
                return true;
            }
            return  true;
        }
        return true;
    }
}