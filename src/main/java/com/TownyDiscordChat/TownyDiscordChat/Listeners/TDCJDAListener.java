package com.TownyDiscordChat.TownyDiscordChat.Listeners;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.TDCManager;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import github.scarsz.discordsrv.dependencies.jda.api.events.guild.member.GuildMemberJoinEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.guild.member.GuildMemberRemoveEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.ChannelAction;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.MessageAction;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.RoleAction;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class TDCJDAListener extends ListenerAdapter {

    private Guild guild = DiscordSRV.getPlugin().getMainGuild();
    private final String newRolePrefix = "NewMember";
    private final String delimiter = "-";
    private final String categoryName = "NewMembers";
    private final String categoryId = "833169677466730514";
    private final String textChannelName = "Getting-Started";

    @Subscribe
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        updateGuild();
        User user = event.getUser();
        Member member = event.getMember();
        String discordId = member.getId();

        if (Main.plugin.SQL.isConnected()) {
            String textChannelId = Main.plugin.data.getTextChannelId(discordId);
            if (textChannelId != null) {
                guild.getTextChannelById(textChannelId).delete().queue(success -> {
                    if (Main.plugin.SQL.isConnected()) {
                        Main.plugin.data.deleteEntry(discordId);
                    }
                });
            }
        }
    }

    @Subscribe
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        updateGuild();
        User user = event.getUser();
        Member member = event.getMember();
        String discordId = member.getId();

        // If already linked before do nothing
        UUID UUID = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(discordId);
        if (UUID != null) {
            TDCManager.discordUserRoleCheck(discordId, UUID);
            return;
        }

        // If `"NewMember-<discordId>` doesn't exist
        //if (!serverRoleExists(discordId)) {
        //createServerRole(discordId).queue(role -> {

        createPrivateTextChannel(discordId).queue(textChannel -> {

            if (Main.plugin.SQL.isConnected()) {
                Main.plugin.data.createEntry(discordId, "EMPTY", textChannel.getId());
            }

            String message = String.join("\n"
                    , "."
                    , "=============================================" , "\n"
                    , "Hello and Welcome to **mc.daf4ever.com**", "\n"
                    , "=============================================", "\n", "\n"
                    , "**---> TO GAIN ACCESS TO DISCORD <---**"
                    , "1. Join our Minecraft Server on **mc.daf4ever.com **"
                    , "2. Write: /towny"
                    , "3. Write: /discord link", "\n", "\n", "\n"
                    , "*1. How to add the server to your Minecraft and connect to it.*"
                    , "----------------------------------------------------------------------------------------"
                    , "1.1. Open Minecraft"
                    , "1.2 Click the green \"PLAY\" button"
                    , "1.3 Click Multiplayer"
                    , "1.4 Click Add Server"
                    , "1.5 Click Done"
                    , "1.6 Type in both fields: **mc.daf4ever.com**"
                    , "1.7 The server now got added to your Minecraft. Congrats! You can click Join.", "\n", "\n"
                    , "*2. Changing your Minecraft Version.*"
                    , "----------------------------------------------------------------------------------------"
                    , "2.1 If it says anything about \"Version\" missmatch when you try to connect, then you need to adjust your Minecraft Version"
                    , "2.2 In the Mulitplayer list search for the newly added **mc.daf4ever.com**"
                    , "2.3 The first number is the server version. In this Example the server version is 1.16.5"
                    , "      --> 1.16.5 Suvival Towny Events Fun"
                    , "2.4 Close Minecraft and reopen it"
                    , "2.5 On the left side of the big green PLAY button, select the proper Minecraft Version."
                    , "2.6 If there it doenst exist, then you need to install it. On the top of minecraft you see:"
                    , "          Play    Installation   Skins    Patch notes"
                    , "2.7 Click on Installations"
                    , "2.8 Click New..."
                    , "2.9 Add a Name and chose the correction Version"
                    , "2.10 Bottom right click the green Create button." , "\n", "\n"
                    , "----------------------------------------------------------------------------------------"
                    , "Attention: Only Legal Copies of Minecraft are allowed to connect to the server due to hackers!"
            );

            updateGuild();

            //guild.addRoleToMember(discordId, role).queue(success -> {

            DiscordUtil.queueMessage(textChannel, message);
            //}, failure -> {

            //});

            //}, failure -> {

        });

        //}, failure -> {

        //});
        //}
    }

    private ChannelAction<TextChannel> createPrivateTextChannel(String discordId) {
        final long noPermission = 0;
        final long viewPermission = Permission.VIEW_CHANNEL.getRawValue();
        final long viewMessageHistory = Permission.MESSAGE_HISTORY.getRawValue();

        final long everyoneRoleId = guild.getPublicRole().getIdLong();
        //final long roleId = getServerRole(discordId).getIdLong();
        final long botId = guild.getMember(DiscordSRV.getPlugin().getJda().getSelfUser()).getIdLong();

        Long discordIdLong = Long.valueOf(discordId);

        return guild.createTextChannel(textChannelName)
                .addRolePermissionOverride(everyoneRoleId, noPermission, viewPermission)
                .addMemberPermissionOverride(discordIdLong, viewPermission + viewMessageHistory, noPermission)
                .addMemberPermissionOverride(botId, viewPermission, noPermission)
                .setParent(guild.getCategoryById(categoryId));
    }

    private RoleAction createServerRole(String discordId) {
        String serverRoleName = newRolePrefix + delimiter + discordId;
        return guild.createRole().setName(serverRoleName);
    }

    private @Nullable Role getServerRole(String discordId) {
        String serverRoleName = newRolePrefix + delimiter + discordId;
        List<Role> roles = guild.getRolesByName(serverRoleName, true);
        if (roles.isEmpty()) {
            return null;
        } else {
            return roles.get(0);
        }
    }

    private Boolean serverRoleExists(String discordId) {
        return getServerRole(discordId) != null;
    }

    private void updateGuild() {
        guild = DiscordSRV.getPlugin().getMainGuild();
    }
}
