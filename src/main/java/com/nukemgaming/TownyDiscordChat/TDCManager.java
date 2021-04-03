package com.nukemgaming.TownyDiscordChat;

import java.awt.Color;
import java.util.List;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.VoiceChannel;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.ChannelAction;
import github.scarsz.discordsrv.util.DiscordUtil;

public class TDCManager {
    private TDCManager() {
    }

    public static final void renameNation(String oldName, String newName) {
        rename(oldName, newName, "nation-", getNationTextCategoryId(), getNationVoiceCategoryId());
    }

    public static final void renameTown(String oldName, String newName) {
        rename(oldName, newName, "town-", getTownTextCategoryId(), getTownVoiceCategoryId());
    }

    public static final void rename(String oldName, String newName, String roleprefix, String townTextCategoryId,
            String townVoiceCategoryId) {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();

        getRole(roleprefix + oldName).getManager().setName(roleprefix + newName).queue();

        List<TextChannel> discordTextChannels = guild.getTextChannelsByName(oldName, true);
        for (TextChannel discordTextChannel : discordTextChannels) {
            if (townTextCategoryId == null || discordTextChannel.getParent().getId().equals(townTextCategoryId)) {
                discordTextChannel.getManager().setName(newName).queue();
            }
        }

        List<VoiceChannel> discordVoiceChannels = guild.getVoiceChannelsByName(oldName, true);
        for (VoiceChannel discordVoiceChannel : discordVoiceChannels) {
            if (townVoiceCategoryId == null || discordVoiceChannel.getParent().getId().equals(townVoiceCategoryId)) {
                discordVoiceChannel.getManager().setName(newName).queue();
            }
        }
    }

    /**
     * Deletes Discord a role and discord channels from the Town
     * 
     * @param town
     */
    public static final void deleteRoleAndChannels(final Town town) {
        deleteRoleAndChannelsFromTown(town.getName());
    }

    /**
     * Deletes Discord a role and discord channels from the Town
     * 
     * @param townName The name of the town
     */
    public static final void deleteRoleAndChannelsFromTown(final String townName) {
        deleteRoleAndChannels(townName, getRole("town-" + townName), getTownTextCategoryId(), getTownVoiceCategoryId());
    }

    /**
     * Deletes Discord a role and discord channels from the Nation
     * 
     * @param nation
     */
    public static final void deleteRoleAndChannels(final Nation nation) {
        deleteRoleAndChannelsFromNation(nation.getName());
    }

    /**
     * Deletes Discord a role and discord channels from the Nation
     * 
     * @param nationName The name of the Nation
     */
    public static final void deleteRoleAndChannelsFromNation(final String nationName) {
        deleteRoleAndChannels(nationName, getRole("nation-" + nationName), getNationTextCategoryId(),
                getNationVoiceCategoryId());
    }

    /**
     * Deletes Discord a role and discord channels with the name, optionally one can
     * define the parents
     * 
     * @param name                 Name of the role and channels to delete
     * @param textChannelParentId  id of the text channels parent or null
     * @param voiceChannelParentId id of the voice channels parent or null
     */
    public static final void deleteRoleAndChannels(final String name, @Nullable final Role role,
            final String textChannelParentId, final String voiceChannelParentId) {
        final Guild guild = DiscordSRV.getPlugin().getMainGuild();

        if (role != null)
            role.delete().queue();

        final List<TextChannel> discordTextChannels = guild.getTextChannelsByName(name, true);
        for (final TextChannel discordTextChannel : discordTextChannels) {
            if (textChannelParentId == null || discordTextChannel.getParent().getId().equals(textChannelParentId)) {
                discordTextChannel.delete().queue();
            }
        }

        final List<VoiceChannel> discordVoiceChannels = guild.getVoiceChannelsByName(name, true);
        for (final VoiceChannel discordVoiceChannel : discordVoiceChannels) {
            if (voiceChannelParentId == null || discordVoiceChannel.getParent().getId().equals(voiceChannelParentId)) {
                discordVoiceChannel.delete().queue();
            }
        }
    }

    /**
     * Removes the Role of the Town the Player is in from the Player
     * 
     * @param player the Player to remove the role from
     */
    public static final void removePlayerTownRole(@NotNull final Player player) {

        // get the Players Town
        final Town town = getTown(player);
        // check if the player is in a town
        if (town == null) {
            player.sendMessage("You're not in a town!");
            return;
        }

        removePlayerRole(player, town);
    }

    /**
     * Removes the Towns Discord Role from the Player
     * 
     * @param player The Player from which the role should be removed
     * @param town   The Town which role should be removed from the Player
     */
    public static final void removePlayerRole(@NotNull final Player player, @NotNull final Town town) {

        // get the LinkedId
        final String linkedId = getLinkedId(player);
        // check if the player has linked their account
        if (linkedId == null) {
            player.sendMessage("You haven't linked your Discord, do /discord link to get started!");
            return;
        }

        // get the discord member from the id
        final Member member = getMember(linkedId);
        // check if the member is in the discord
        if (member == null) {
            player.sendMessage("You are not in the Discord server!");
            return;
        }

        // get the towns discord role
        final Role townRole = getRole(town);

        // check if the Town Role exists and if the player has the town role
        if (townRole != null && member.getRoles().contains(townRole)) {

            // remove the town role from the player
            DiscordUtil.removeRolesFromMember(member, townRole);

            // notify the player on discord
            DiscordUtil.privateMessage(member.getUser(),
                    "You have been removed from the discord " + town + " channels!");

            // notify the player ingame
            player.sendMessage("You have been removed from the discord " + town + " channels!");
        }
    }

    /**
     * Gives the Player it towns Discord Role
     * 
     * @param player Player to give the role to
     */
    public static final void givePlayerNationRole(@NotNull final Player player) {

        // Get the Players Town
        final Nation nation = getNation(player);
        // check if the player is in a town
        if (nation == null) {
            player.sendMessage("You're not in a nation!");
            return;
        }
        givePlayerRole(player, nation);
    }

    /**
     * Gives the Player the Nations Discord Role
     * 
     * @param player Player to give the Role to
     * @param nation The nation which role the Player should get
     */
    public static final void givePlayerRole(@NotNull final Player player, @NotNull final Nation nation) {

        // get the LinkedId
        final String linkedId = getLinkedId(player);
        // checking to see if the player's minecraft account is linked with their
        // discord account
        if (linkedId == null) {
            player.sendMessage("You haven't linked your Discord, do /discord link to get started!");
            return;
        }

        // getting the Discord Member instance
        final Member member = getMember(linkedId);
        // check if the member is in the discord
        if (member == null) {
            player.sendMessage("You are not in the Discord server!");
            return;
        }

        // get the towns Role
        final Role townRole = getRole(nation);
        // check if the role exists
        if (townRole != null) {
            // give the player the town role if it exists
            giveRoleToMember(player, member, townRole);
        } else {
            // create the town role if it does not exist
            createRole(player, member, nation);
        }
    }

    /**
     * Gives the Player it towns Discord Role
     * 
     * @param player Player to give the role to
     */
    public static final void givePlayerTownRole(@NotNull final Player player) {

        // Get the Players Town
        final Town town = getTown(player);
        // check if the player is in a town
        if (town == null) {
            player.sendMessage("You're not in a town!");
            return;
        }
        givePlayerRole(player, town);
    }

    /**
     * Gives the Player the Towns Discord Role
     * 
     * @param player Player to give the Role to
     * @param town   The town which role the Player should get
     */
    public static final void givePlayerRole(@NotNull final Player player, @NotNull final Town town) {

        // get the LinkedId
        final String linkedId = getLinkedId(player);
        // checking to see if the player's minecraft account is linked with their
        // discord account
        if (linkedId == null) {
            player.sendMessage("You haven't linked your Discord, do /discord link to get started!");
            return;
        }

        // getting the Discord Member instance
        final Member member = getMember(linkedId);
        // check if the member is in the discord
        if (member == null) {
            player.sendMessage("You are not in the Discord server!");
            return;
        }

        // get the towns Role
        final Role townRole = getRole(town);
        // check if the role exists
        if (townRole != null) {
            // give the player the town role if it exists
            giveRoleToMember(player, member, townRole);
        } else {
            // create the town role if it does not exist
            createRole(player, member, town);
        }
    }

    /**
     * Gives the player the role and notifies the Player accordingly
     * 
     * @param player   Player to notify
     * @param member   Member to give the role to
     * @param townRole The role to give to the Member
     */
    private static void giveRoleToMember(@NotNull final Player player, @NotNull final Member member,
            @NotNull final Role townRole) {
        // idk debug output? was here when I got here
        Main.plugin.getLogger().info("--------------------------------------------------");
        Main.plugin.getLogger().info(member.getId());
        Main.plugin.getLogger().info("--------------------------------------------------");

        // ActiveNoise Username Discord Roles
        final List<Role> usernameDiscordRoles = member.getRoles();
        Main.plugin.getLogger().info("--------------------------------------------------");
        for (final Role role : usernameDiscordRoles) {
            Main.plugin.getLogger().info(role.getName() + " | " + role.getId() + " | " + role);
        }
        Main.plugin.getLogger().info("--------------------------------------------------");
        // Check to see if the player is already in a town
        if (member.getRoles().contains(townRole)) {
            player.sendMessage("You are already a part of the " + townRole.getName() + " role!");
        } else {
            // give the member the role
            DiscordUtil.addRolesToMember(member, townRole);
            // notify on discord
            DiscordUtil.privateMessage(member.getUser(), "Your account has been linked to "
                    + townRole.getName().substring(townRole.getName().indexOf('-') + 1) + "!");
            // notify ingame
            player.sendMessage("Your account has been linked to "
                    + townRole.getName().substring(townRole.getName().indexOf('-') + 1) + "!");
        }
    }

    /**
     * Creates Towns role, creates the corresponding channels and gives the Member
     * the role
     * 
     * @param player The Player who initiated the creation
     * @param member The member corresponding to the Player
     * @param town   The Town to create the role for
     */
    private static void createRole(@NotNull final Player player, @NotNull final Member member,
            @NotNull final Town town) {
        final Guild guild = member.getGuild();
        if (Main.plugin.config.getBoolean("town.CreateRoleIfNoneExists")) {
            player.sendMessage(town + " Doesn't have a Role, automatically creating one for you...!");
            guild.createRole().setName("town-" + town.getName())
                    .setColor(Color.decode(Main.plugin.config.getString("town.RoleCreateColorCode"))).queue(role -> {
                        DiscordUtil.addRolesToMember(member, role);
                        createChannels(guild, town, role);
                    });
        }
    }

    /**
     * Creates Nnations role, creates the corresponding channels and gives the
     * Member the role
     * 
     * @param player The Player who initiated the creation
     * @param member The member corresponding to the Player
     * @param nation The Nation to create the role for
     */
    private static void createRole(@NotNull final Player player, @NotNull final Member member,
            @NotNull final Nation nation) {
        final Guild guild = member.getGuild();
        if (Main.plugin.config.getBoolean("nation.CreateRoleIfNoneExists")) {
            player.sendMessage(nation + " Doesn't have a Role, automatically creating one for you...!");
            guild.createRole().setName("nation-" + nation.getName())
                    .setColor(Color.decode(Main.plugin.config.getString("nation.RoleCreateColorCode"))).queue(role -> {
                        giveRoleToMember(player, member, role);
                        createChannels(guild, nation, role);
                    });
        }
    }

    /**
     * Creates text and or voice channels in the guild that are only accesible by
     * the towns Role
     * 
     * @param guild The guild to create the chanels in
     * @param town  The town to create the channels for
     * @param role  The Role that can access the channels
     */
    private static void createChannels(final Guild guild, final Town town, final Role role) {
        createChannels(guild, town.getName(), role, Main.plugin.config.getBoolean("town.CreateVoiceChannelForRole"),
                Main.plugin.config.getBoolean("town.CreateTextChannelForRole"), getTownVoiceCategoryId(),
                getTownTextCategoryId());
    }

    /**
     * Creates text and or voice channels in the guild that are only accesible by
     * the nation role
     * 
     * @param guild  The guild to create the chanels in
     * @param nation The nation to create the channels for
     * @param role   The Role that can access the channels
     */
    private static void createChannels(final Guild guild, final Nation nation, final Role role) {
        createChannels(guild, nation.getName(), role, Main.plugin.config.getBoolean("nation.CreateVoiceChannelForRole"),
                Main.plugin.config.getBoolean("nation.CreateTextChannelForRole"), getNationVoiceCategoryId(),
                getNationTextCategoryId());
    }

    /**
     * Create either text and or voice channels with a specific name in specific
     * categories that are only accessible by a specific role
     * 
     * @param guild                  The guild to create the channels in
     * @param name                   The name of the channels
     * @param role                   The role that can access the channels
     * @param createVoiceChannel     If a voice channel should be created
     * @param createTextChannel      If a text channel should be created
     * @param voiceChannelCategoryId Optional voice channel category
     * @param textChannelCategoryId  Optional text channel category
     */
    private static void createChannels(@NotNull final Guild guild, @NotNull final String name, @NotNull final Role role,
            final boolean createVoiceChannel, final boolean createTextChannel,
            @Nullable final String voiceChannelCategoryId, @Nullable final String textChannelCategoryId) {

        final long noPermission = 0;
        final long viewPermission = Permission.VIEW_CHANNEL.getRawValue();

        final long everyoneRoleId = guild.getPublicRole().getIdLong();
        final long roleId = role.getIdLong();
        final long botId = guild.getMember(DiscordSRV.getPlugin().getJda().getSelfUser()).getIdLong();

        if (createVoiceChannel) {
            final ChannelAction<VoiceChannel> voiceChannelAction = guild.createVoiceChannel(name)
                    .addRolePermissionOverride(everyoneRoleId, noPermission, viewPermission)
                    .addRolePermissionOverride(roleId, viewPermission, noPermission)
                    .addMemberPermissionOverride(botId, viewPermission, noPermission);
            if (voiceChannelCategoryId != null) {
                voiceChannelAction.setParent(guild.getCategoryById(voiceChannelCategoryId));
            }
            voiceChannelAction.queue();
        }
        if (createTextChannel) {
            final ChannelAction<TextChannel> textChannelAction = guild.createTextChannel(name)
                    .addRolePermissionOverride(everyoneRoleId, noPermission, viewPermission)
                    .addRolePermissionOverride(roleId, viewPermission, noPermission)
                    .addMemberPermissionOverride(botId, viewPermission, noPermission);
            if (textChannelCategoryId != null) {
                textChannelAction.setParent(guild.getCategoryById(textChannelCategoryId));
            }
            textChannelAction.queue();
        }
    }

    /**
     * Gets the Discord ID of the Player
     * 
     * @param player Player to get the Discord ID from
     * @return The Discord ID or null when the player did not link their Discord
     */
    private static @Nullable String getLinkedId(@NotNull final Player player) {
        return DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
    }

    /**
     * Gets the Member instance of the Discord ID from the Main Guild
     * 
     * @param id The Discord ID to get the Member of
     * @return The Member or null if the ID is not in the discord
     */
    private static @Nullable Member getMember(@NotNull final String id) {
        return DiscordSRV.getPlugin().getMainGuild().getMemberById(id);
    }

    /**
     * Gets the Town of the Player
     * 
     * @param player The Player to get the Town from
     * @return The Players Town or null if the Player is in no Town
     */
    private static @Nullable Town getTown(@NotNull final Player player) {
        try {
            final Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
            return resident.getTown();
        } catch (final NotRegisteredException e) {
            return null;
        }
    }

    /**
     * Gets the Nation of the Player
     * 
     * @param player The Player to get the Nation from
     * @return The Players Nation or null if the Player is in no Nation
     */
    private static @Nullable Nation getNation(@NotNull final Player player) {
        Town town = getTown(player);
        if (town == null)
            return null;
        try {
            return town.getNation();
        } catch (NotRegisteredException e) {
            return null;
        }

    }

    /**
     * Gets the Towns Discord Role
     * 
     * @param town The Town to get the Role of
     * @return The Towns Discord Role or null if it does not exist
     */
    private static @Nullable Role getRole(@NotNull final Town town) {
        return getRole("town-" + town.getName());
    }

    /**
     * Gets the Nations Discord Role
     * 
     * @param nation The Nation to get the Role of
     * @return The Nations Discord Role or null if it does not exist
     */
    private static @Nullable Role getRole(@NotNull final Nation nation) {
        return getRole("nation-" + nation.getName());
    }

    /**
     * Gets the Discord Role by Name
     * 
     * @param name The name of the Role
     * @return The Discord Role or null if it does not exist
     */
    private static @Nullable Role getRole(@NotNull final String name) {
        return DiscordUtil.getRoleByName(DiscordSRV.getPlugin().getMainGuild(), name);
    }

    private static @Nullable String getTownVoiceCategoryId() {
        return Main.plugin.config.getBoolean("town.UseCategoryForText")
                ? Main.plugin.config.getString("town.TextCategoryId")
                : null;
    }

    private static @Nullable String getTownTextCategoryId() {
        return Main.plugin.config.getBoolean("town.UseCategoryForVoice")
                ? Main.plugin.config.getString("town.VoiceCategoryId")
                : null;
    }

    private static @Nullable String getNationVoiceCategoryId() {
        return Main.plugin.config.getBoolean("nation.UseCategoryForText")
                ? Main.plugin.config.getString("nation.TextCategoryId")
                : null;
    }

    private static @Nullable String getNationTextCategoryId() {
        return Main.plugin.config.getBoolean("nation.UseCategoryForVoice")
                ? Main.plugin.config.getString("nation.VoiceCategoryId")
                : null;
    }
}
