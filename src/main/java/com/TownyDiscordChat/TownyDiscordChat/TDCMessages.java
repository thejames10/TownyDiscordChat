package com.TownyDiscordChat.TownyDiscordChat;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.google.common.base.Preconditions;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class TDCMessages {

    /**
     * Send message to game discord and log channel
     *
     * @param UUID    the associated minecraft player
     * @param message the message
     */
    public static void sendMessageToAll(UUID UUID, String message) {
        Preconditions.checkNotNull(UUID);
        Preconditions.checkNotNull(message);

        sendMessageToPlayerGame(Bukkit.getOfflinePlayer(UUID), message);
        sendMessageToPlayerDiscord(UUID, message);
        sendMessageToDiscordLogChannel(UUID, message);
    }

    /**
     * Send message to the minecraft player in-game
     *
     * @param player  the associated minecraft player
     * @param message the message
     */
    public static void sendMessageToPlayerGame(Player player, String message) {
        Preconditions.checkNotNull(player);
        Preconditions.checkNotNull(message);

        player.sendMessage(getPluginPrefix() + " " + message);
    }

    /**
     * Attempts to send message to the minecraft player in-game
     *
     * @param offlinePlayer the associated minecraft player
     * @param message       the message
     */
    public static void sendMessageToPlayerGame(OfflinePlayer offlinePlayer, String message) {
        Preconditions.checkNotNull(offlinePlayer);
        Preconditions.checkNotNull(message);

        if (offlinePlayer.getPlayer() != null) {
            offlinePlayer.getPlayer().sendMessage(getPluginPrefix() + " " + message);
        }
    }

    /**
     * Send message to minecraft player's discord
     *
     * @param UUID    the UUID of the associated minecraft player
     * @param message the message
     */
    public static void sendMessageToPlayerDiscord(UUID UUID, String message) {
        Preconditions.checkNotNull(UUID);
        Preconditions.checkNotNull(message);

        String linkedId = Preconditions.checkNotNull(TDCManager.getLinkedId(Bukkit.getOfflinePlayer(UUID)));
        User user = Preconditions.checkNotNull(DiscordUtil.getUserById(linkedId));

        DiscordUtil.privateMessage(user, ChatColor.stripColor(getPluginPrefix() + " " + message));
    }

    /**
     * Send message to ChannelID found in config.yml
     *
     * @param message the message
     */
    public static void sendMessageToDiscordLogChannel(String message) {
        Preconditions.checkNotNull(message);

        String discordLogTextChannelId = Preconditions.checkNotNull(Main.plugin.config.getString("messages.DiscordLogTextChannelId"));

        if (!discordLogTextChannelId.equals("0")) {
            Guild guild = Preconditions.checkNotNull(DiscordSRV.getPlugin().getMainGuild());
            TextChannel textChannel = Preconditions.checkNotNull(guild.getTextChannelById(discordLogTextChannelId));

            String timeZone = getConfigTimeZone();
            String dateTimeFormat = getConfigDateTimeFormat();

            Instant instant = Instant.now();
            ZoneId zoneId = ZoneId.of(timeZone);
            ZonedDateTime zonedDateTime = instant.atZone(zoneId);
            String logTime = DateTimeFormatter.ofPattern(dateTimeFormat).format(zonedDateTime);

            String logMsg = String.join("\n"
                    , logTime
                    , "--------------------------------------------------"
                    , "Message: " + getPluginPrefix() + " " + message
                    , "--------------------------------------------------");

            DiscordUtil.sendMessage(textChannel, ChatColor.stripColor(logMsg));
            Main.plugin.getLogger().info(ChatColor.stripColor(logMsg));
        }
    }

    /**
     * Send message to ChannelID found in config.yml
     *
     * @param UUID    the UUID of the associated minecraft player
     * @param message the message
     */
    public static void sendMessageToDiscordLogChannel(UUID UUID, String message) {
        Preconditions.checkNotNull(message);

        String discordLogTextChannelId = Preconditions.checkNotNull(Main.plugin.config.getString("messages.DiscordLogTextChannelId"));

        if (!discordLogTextChannelId.equals("0")) {
            OfflinePlayer offlinePlayer = Preconditions.checkNotNull(Bukkit.getOfflinePlayer(UUID));
            Guild guild = Preconditions.checkNotNull(DiscordSRV.getPlugin().getMainGuild());
            TextChannel textChannel = Preconditions.checkNotNull(guild.getTextChannelById(discordLogTextChannelId));
            String discordId = Preconditions.checkNotNull(TDCManager.getLinkedId(offlinePlayer));
            Member member = Preconditions.checkNotNull(DiscordUtil.getMemberById(discordId));
            List<Role> roles = Preconditions.checkNotNull(member).getRoles();

            String timeZone = getConfigTimeZone();
            String dateTimeFormat = getConfigDateTimeFormat();

            Instant instant = Instant.now();
            ZoneId zoneId = ZoneId.of(timeZone);
            ZonedDateTime zonedDateTime = instant.atZone(zoneId);
            String logTime = DateTimeFormatter.ofPattern(dateTimeFormat).format(zonedDateTime);

            String logMsg = String.join("\n"
                    , logTime
                    , "--------------------------------------------------"
                    , "Minecraft Name: " + offlinePlayer.getName()
                    , "Minecraft UUID: " + UUID
                    , "Discord Name: " + member.getUser().getAsMention()
                    , "Discord ID: " + TDCManager.getLinkedId(offlinePlayer)
                    , "Discord Roles: " + roles
                    , "Message: " + getPluginPrefix() + " " + message
                    , "--------------------------------------------------");

            DiscordUtil.sendMessage(textChannel, ChatColor.stripColor(logMsg));
            Main.plugin.getLogger().info(ChatColor.stripColor(logMsg));
        }
    }

    /**
     * Retrieves the plugin prefix for messages from config.yml
     *
     * @return String prefix
     */
    public static String getPluginPrefix() {
        String prefix = Preconditions.checkNotNull(Main.plugin.config.getString("messages.Prefix"));
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    /**
     * Retrieves Please wait message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgCommandsPleasewait() {
        return getConfigMsg("messages.Commands.PleaseWait");
    }

    /**
     * Retrieves No permission message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgCommandsNopermission() {
        return getConfigMsg("messages.Commands.NoPermission");
    }

    /**
     * Retrieves Role change not required message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgRoleDoNothingSuccess() {
        return getConfigMsg("messages.Role.DoNothing.Success");
    }

    /**
     * Retrieves Failed to do nothing message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgRoleDoNothingFailure() {
        return getConfigMsg("messages.Role.DoNothing.Failure");
    }

    /**
     * Retrieves Removed role message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgRoleRemoveSuccess() {
        return getConfigMsg("messages.Role.Remove.Success");
    }

    /**
     * Retrieves Failed to remove role message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgRoleRemoveFailure() {
        return getConfigMsg("messages.Role.Remove.Failure");
    }

    /**
     * Retrieves Added role message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgRoleAddSuccess() {
        return getConfigMsg("messages.Role.Add.Success");
    }

    /**
     * Retrieves Failed to add role message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgRoleAddFailure() {
        return getConfigMsg("messages.Role.Add.Failure");
    }

    /**
     * Retrieves Created role message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgRoleCreateSuccess() {
        return getConfigMsg("messages.Role.Create.Success");
    }

    /**
     * Retrieves Failed to create role message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgRoleCreateFailure() {
        return getConfigMsg("messages.Role.Create.Failure");
    }

    /**
     * Retrieves Deleted role message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgRoleDeleteSuccess() {
        return getConfigMsg("messages.Role.Delete.Success");
    }

    /**
     * Retrieves Failed to delete role message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgRoleDeleteFailure() {
        return getConfigMsg("messages.Role.Delete.Failure");
    }

    /**
     * Retrieves Renamed role message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgRoleRenameSuccess() {
        return getConfigMsg("messages.Role.Rename.Success");
    }

    /**
     * Retrieves Failed to rename role message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgRoleRenameFailure() {
        return getConfigMsg("messages.Role.Rename.Failure");
    }

    /**
     * Retrieves TextChannel change not required message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgTextChannelDoNothingSuccess() {
        return getConfigMsg("messages.TextChannel.DoNothing.Success");
    }

    /**
     * Retrieves Failed to do nothing message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgTextChannelDoNothingFailure() {
        return getConfigMsg("messages.TextChannel.DoNothing.Failure");
    }

    /**
     * Retrieves Removed TextChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgTextChannelRemoveSuccess() {
        return getConfigMsg("messages.TextChannel.Remove.Success");
    }

    /**
     * Retrieves Failed to remove TextChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgTextChannelRemoveFailure() {
        return getConfigMsg("messages.TextChannel.Remove.Failure");
    }

    /**
     * Retrieves Added TextChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgTextChannelAddSuccess() {
        return getConfigMsg("messages.TextChannel.Add.Success");
    }

    /**
     * Retrieves Failed to add TextChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgTextChannelAddFailure() {
        return getConfigMsg("messages.TextChannel.Add.Failure");
    }

    /**
     * Retrieves Created TextChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgTextChannelCreateSuccess() {
        return getConfigMsg("messages.TextChannel.Create.Success");
    }

    /**
     * Retrieves Failed to create TextChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgTextChannelCreateFailure() {
        return getConfigMsg("messages.TextChannel.Create.Failure");
    }

    /**
     * Retrieves Deleted TextChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgTextChannelDeleteSuccess() {
        return getConfigMsg("messages.TextChannel.Delete.Success");
    }

    /**
     * Retrieves Failed to delete TextChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgTextChannelDeleteFailure() {
        return getConfigMsg("messages.TextChannel.Delete.Failure");
    }

    /**
     * Retrieves Renamed TextChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgTextChannelRenameSuccess() {
        return getConfigMsg("messages.TextChannel.Rename.Success");
    }

    /**
     * Retrieves Failed to rename TextChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgTextChannelRenameFailure() {
        return getConfigMsg("messages.TextChannel.Rename.Failure");
    }

    /**
     * Retrieves VoiceChannel change not required message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgVoiceChannelDoNothingSuccess() {
        return getConfigMsg("messages.VoiceChannel.DoNothing.Success");
    }

    /**
     * Retrieves Failed to do nothing message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgVoiceChannelDoNothingFailure() {
        return getConfigMsg("messages.VoiceChannel.DoNothing.Failure");
    }

    /**
     * Retrieves Removed VoiceChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgVoiceChannelRemoveSuccess() {
        return getConfigMsg("messages.VoiceChannel.Remove.Success");
    }

    /**
     * Retrieves Failed to remove VoiceChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgVoiceChannelRemoveFailure() {
        return getConfigMsg("messages.VoiceChannel.Remove.Failure");
    }

    /**
     * Retrieves Added VoiceChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgVoiceChannelAddSuccess() {
        return getConfigMsg("messages.VoiceChannel.Add.Success");
    }

    /**
     * Retrieves Added VoiceChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgVoiceChannelAddFailure() {
        return getConfigMsg("messages.VoiceChannel.Add.Failure");
    }

    /**
     * Retrieves Created VoiceChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgVoiceChannelCreateSuccess() {
        return getConfigMsg("messages.VoiceChannel.Create.Success");
    }

    /**
     * Retrieves Failed to create VoiceChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgVoiceChannelCreateFailure() {
        return getConfigMsg("messages.VoiceChannel.Create.Failure");
    }

    /**
     * Retrieves Deleted VoiceChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgVoiceChannelDeleteSuccess() {
        return getConfigMsg("messages.VoiceChannel.Delete.Success");
    }

    /**
     * Retrieves Failed to delete VoiceChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgVoiceChannelDeleteFailure() {
        return getConfigMsg("messages.VoiceChannel.Delete.Failure");
    }

    /**
     * Retrieves Renamed VoiceChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgVoiceChannelRenameSuccess() {
        return getConfigMsg("messages.VoiceChannel.Rename.Success");
    }

    /**
     * Retrieves Failed to rename VoiceChannel message from config.yml
     *
     * @return String message
     */
    public static String getConfigMsgVoiceChannelRenameFailure() {
        return getConfigMsg("messages.VoiceChannel.Rename.Failure");
    }

    /**
     * Retrieves message from config.yml
     *
     * @param ymlPath YML reference location
     * @return String message
     */
    private static String getConfigMsg(String ymlPath) {
        String plainText = Preconditions.checkNotNull(Main.plugin.config.getString(ymlPath));

        return ChatColor.translateAlternateColorCodes('&', plainText);
    }

    /**
     * Retrieves TimeZone from config.yml
     *
     * @return String timeZone
     */
    private static String getConfigTimeZone() {
        String timeZone = Main.plugin.config.getString("messages.TimeZone");
        return Preconditions.checkNotNull(timeZone);
    }

    /**
     * Retrieves DateTimeFormat from config.yml
     *
     * @return String dateTimeFormat
     */
    private static String getConfigDateTimeFormat() {
        String dateTimeFormat = Main.plugin.config.getString("messages.DateFormat");
        return Preconditions.checkNotNull(dateTimeFormat);
    }
}