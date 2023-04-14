package com.TownyDiscordChat.TownyDiscordChat.Core;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.Messages.TDCMessages;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import github.scarsz.discordsrv.dependencies.google.common.base.Preconditions;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.RoleAction;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.ChannelAction;

public class TDCManager {

    private TDCManager() {
    }

    /**
     * Add or remove Discord ROLE for ALL PLAYERS that are in a town or nation
     */
    public static final void discordUserRoleCheckAllLinked() {
        Map<String, UUID> linkedAccounts = DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts();
        linkedAccounts.forEach((discordId, UUID) -> {
            discordUserRoleCheck(discordId, UUID);
        });
    }

    /**
     * Add or remove Discord ROLE for PLAYER that is in a town or nation
     *
     * @param discordId Player's Discord ID
     * @param UUID      Player's minecraft unique ID
     */
    public static final void discordUserRoleCheck(String discordId, UUID UUID) {

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID);
        if (!offlinePlayer.hasPlayedBefore()) {
            return;
        }

        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        Resident resident = TownyUniverse.getInstance().getResident(UUID);
        if (resident == null) {
            return;
        }

        List<Role> memberRoles = DiscordUtil.getMemberById(discordId).getRoles();
        List<Role> memberTownRoles = new ArrayList<>();
        List<Role> memberNationRoles = new ArrayList<>();
        for (Role role : memberRoles) {
            if (role.getName().startsWith("town-")) {
                memberTownRoles.add(role);
            } else if (role.getName().startsWith("nation-")) {
                memberNationRoles.add(role);
            }
        }

        Town town = null;
        boolean hasTown = resident.hasTown();
        if (hasTown) {
            try {
                town = resident.getTown();
            } catch (NotRegisteredException e) {
                //e.printStackTrace();
            }
        }

        Nation nation = null;
        boolean hasNation = resident.hasNation();
        if (hasNation) {
            try {
                nation = resident.getTown().getNation();
            } catch (NotRegisteredException e) {
                //e.printStackTrace();
            }
        }

        boolean townRoleExists = false;
        if (town != null) {
            List<Role> roles = guild.getRolesByName("town-" + town.getName(), true);
            if (!roles.isEmpty()) {
                townRoleExists = guild.getRoles().contains(roles.get(0));
            }
        }

        boolean nationRoleExists = false;
        if (nation != null) {
            List<Role> roles = guild.getRolesByName("nation-" + nation.getName(), true);
            if (!roles.isEmpty()) {
                nationRoleExists = guild.getRoles().contains(roles.get(0));
            }
        }

        boolean hasTownDiscordRole = memberTownRoles.size() != 0 & townRoleExists;
        boolean hasNationDiscordRole = memberNationRoles.size() != 0 & nation != null & nationRoleExists;

        if (!hasTown & !hasNation & !hasTownDiscordRole & !hasNationDiscordRole) {
            // Do nothing
            TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleDoNothingSuccess() + "[1]");
        } else if (!hasTown & !hasNation & !hasTownDiscordRole & hasNationDiscordRole) {
            // remove nation role
            for (Role memberNationRole : memberNationRoles) {
                guild.removeRoleFromMember(discordId, memberNationRole).queueAfter(10, TimeUnit.SECONDS, success -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleRemoveSuccess() + " " + memberNationRole.getName() + " [2]");
                }, failure -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleRemoveFailure() + " " + memberNationRole.getName() + " [2]");
                });
            }
        } else if (!hasTown & !hasNation & hasTownDiscordRole & !hasNationDiscordRole) {
            // remove town role
            for (Role memberTownRole : memberTownRoles) {
                guild.removeRoleFromMember(discordId, memberTownRole).queueAfter(10, TimeUnit.SECONDS, success -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleRemoveSuccess() + " " + memberTownRole.getName() + " [3]");
                }, failure -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleRemoveFailure() + " " + memberTownRole.getName() + " [3]");
                });
            }
        } else if (!hasTown & !hasNation & hasTownDiscordRole & hasNationDiscordRole) {
            // remove town and nation
            for (Role memberTownRole : memberTownRoles) {
                guild.removeRoleFromMember(discordId, memberTownRole).queueAfter(10, TimeUnit.SECONDS, success -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleRemoveSuccess() + " " + memberTownRole.getName() + " [4]");
                }, failure -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleRemoveFailure() + " " + memberTownRole.getName() + " [4]");
                });
            }
            for (Role memberNationRole : memberNationRoles) {
                guild.removeRoleFromMember(discordId, memberNationRole).queueAfter(10, TimeUnit.SECONDS, success -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleRemoveSuccess() + " " + memberNationRole.getName() + " [4]");
                }, failure -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleRemoveFailure() + " " + memberNationRole.getName() + " [4]");
                });
            }
        } else if (!hasTown & hasNation & !hasTownDiscordRole & !hasNationDiscordRole) {
            // Do nothing - should never reach this case
            TDCMessages.sendMessageToDiscordLogChannel(UUID, TDCMessages.getConfigMsgRoleDoNothingSuccess() + " [5]");
        } else if (!hasTown & hasNation & !hasTownDiscordRole & hasNationDiscordRole) {
            // Do nothing - should never reach this case
            TDCMessages.sendMessageToDiscordLogChannel(UUID, TDCMessages.getConfigMsgRoleDoNothingSuccess() + " [6]");
        } else if (!hasTown & hasNation & hasTownDiscordRole & !hasNationDiscordRole) {
            // Do nothing - should never reach this case
            TDCMessages.sendMessageToDiscordLogChannel(UUID, TDCMessages.getConfigMsgRoleDoNothingSuccess() + " [7]");
        } else if (!hasTown & hasNation & hasTownDiscordRole & hasNationDiscordRole) {
            // remove town role
            for (Role memberTownRole : memberTownRoles) {
                guild.removeRoleFromMember(discordId, memberTownRole).queueAfter(10, TimeUnit.SECONDS, success -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleRemoveSuccess() + " " + memberTownRole.getName() + " [8]");
                }, failure -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleRemoveFailure() + " " + memberTownRole.getName() + " [8]");
                });
            }
        } else if (hasTown & !hasNation & !hasTownDiscordRole & !hasNationDiscordRole) {
            // add town role
            memberTownRoles.add(guild.getRolesByName("town-" + town.getName(), true).get(0));
            for (Role memberTownRole : memberTownRoles) {
                guild.addRoleToMember(discordId, memberTownRole).queueAfter(10, TimeUnit.SECONDS, success -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleAddSuccess() + " " + memberTownRole.getName() + " [9]");
                }, failure -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleAddFailure() + " " + memberTownRole.getName() + " [9]");
                });
            }
        } else if (hasTown & !hasNation & !hasTownDiscordRole & hasNationDiscordRole) {
            // add town role and remove nation role
            memberTownRoles.add(guild.getRolesByName("town-" + town.getName(), true).get(0));
            for (Role memberTownRole : memberTownRoles) {
                guild.addRoleToMember(discordId, memberTownRole).queueAfter(10, TimeUnit.SECONDS, success -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleAddSuccess() + " " + memberTownRole.getName() + " [10]");
                }, failure -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleAddFailure() + " " + memberTownRole.getName() + " [10]");
                });
            }
            for (Role memberNationRole : memberNationRoles) {
                guild.removeRoleFromMember(discordId, memberNationRole).queueAfter(10, TimeUnit.SECONDS, success -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleRemoveSuccess() + " " + memberNationRole.getName() + " [10]");
                }, failure -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleRemoveFailure() + " " + memberNationRole.getName() + " [10]");
                });
            }
        } else if (hasTown & !hasNation & hasTownDiscordRole & !hasNationDiscordRole) {
            // Do nothing - player already has required discord roles
            TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleDoNothingSuccess() + " [11]");
        } else if (hasTown & !hasNation & hasTownDiscordRole & hasNationDiscordRole) {
            // remove nation role
            for (Role memberNationRole : memberNationRoles) {
                guild.removeRoleFromMember(discordId, memberNationRole).queueAfter(10, TimeUnit.SECONDS, success -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleRemoveSuccess() + " " + memberNationRole.getName() + " [12]");
                }, failure -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleRemoveFailure() + " " + memberNationRole.getName() + " [12]");
                });
            }
        } else if (hasTown & hasNation & !hasTownDiscordRole & !hasNationDiscordRole) {
            // add town role and nation role
            memberTownRoles.add(guild.getRolesByName("town-" + town.getName(), true).get(0));
            for (Role memberTownRole : memberTownRoles) {
                guild.addRoleToMember(discordId, memberTownRole).queueAfter(10, TimeUnit.SECONDS, success -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleAddSuccess() + " " + memberTownRole.getName() + " [13]");
                }, failure -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleAddFailure() + " " + memberTownRole.getName() + " [13]");
                });
            }
            memberNationRoles.add(guild.getRolesByName("nation-" + nation.getName(), true).get(0));
            for (Role memberNationRole : memberNationRoles) {
                guild.addRoleToMember(discordId, memberNationRole).queueAfter(10, TimeUnit.SECONDS, success -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleAddSuccess() + " " + memberNationRole.getName() + " [13]");
                }, failure -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleAddFailure() + " " + memberNationRole.getName() + " [13]");
                });
            }
        } else if (hasTown & hasNation & !hasTownDiscordRole & hasNationDiscordRole) {
            // add town role
            memberTownRoles.add(guild.getRolesByName("town-" + town.getName(), true).get(0));
            for (Role memberTownRole : memberTownRoles) {
                guild.addRoleToMember(discordId, memberTownRole).queueAfter(10, TimeUnit.SECONDS, success -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleAddSuccess() + " " + memberTownRole.getName() + " [14]");
                }, failure -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleAddFailure() + " " + memberTownRole.getName() + " [14]");
                });
            }
        } else if (hasTown & hasNation & hasTownDiscordRole & !hasNationDiscordRole) {
            // add nation role
            memberNationRoles.add(guild.getRolesByName("nation-" + nation.getName(), true).get(0));
            for (Role memberNationRole : memberNationRoles) {
                guild.addRoleToMember(discordId, memberNationRole).queueAfter(10, TimeUnit.SECONDS, success -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleAddSuccess() + " " + memberNationRole.getName() + " [15]");
                }, failure -> {
                    TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleAddFailure() + " " + memberNationRole.getName() + " [15]");
                });
            }
        } else if (hasTown & hasNation & hasTownDiscordRole & hasNationDiscordRole) {
            // Do nothing - player already has required discord roles
            TDCMessages.sendMessageToPlayerGameAndLog(UUID, TDCMessages.getConfigMsgRoleDoNothingSuccess() + " [16]");
        }
    }

    /**
     * Creates Discord ROLE for TOWN
     */
    public static @Nullable String createDiscordRole(Town town) {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();

        AtomicReference<String> roleId = null;

        RoleAction role = guild.createRole().setName("town-" + town.getName()).setColor(Color.decode(Main.plugin.config.getString("town.RoleCreateColorCode")));
        role.queue(success -> {
            TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleCreateSuccess() + " town-" + town.getName() + " [1023]");
            roleId.set(success.getId());
        }, failure -> {
            TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleCreateFailure() + " town-" + town.getName() + " [1023]");
        });
        return roleId.get();
    }

    public static void createDiscordRole(Town town, Consumer<String> callback) {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();

        AtomicReference<String> roleId = new AtomicReference<>();

        RoleAction role = guild.createRole().setName("town-" + town.getName()).setColor(Color.decode(Main.plugin.config.getString("town.RoleCreateColorCode")));
        role.queue(success -> {
            TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleCreateSuccess() + " town-" + town.getName() + " [1023]");
            roleId.set(success.getId());
            callback.accept(roleId.get());
        }, failure -> {
            TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleCreateFailure() + " town-" + town.getName() + " [1023]");
            callback.accept(null);
        });
    }

    /**
     * Creates Discord ROLE for NATION
     */
    public static void createDiscordRole(Nation nation) {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();

        RoleAction role = guild.createRole().setName("nation-" + nation.getName()).setColor(Color.decode(Main.plugin.config.getString("nation.RoleCreateColorCode")));
        role.queue(success -> {
            TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleCreateSuccess() + " nation-" + nation.getName() + " [1024]");
        }, failure -> {
            TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleCreateFailure() + " town-" + nation.getName() + " [1024]");
        });
    }

    public static void createDiscordRole(Nation nation, Consumer<String> callback) {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();

        AtomicReference<String> roleId = new AtomicReference<>();

        RoleAction role = guild.createRole().setName("nation-" + nation.getName()).setColor(Color.decode(Main.plugin.config.getString("nation.RoleCreateColorCode")));
        role.queue(success -> {
            TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleCreateSuccess() + " nation-" + nation.getName() + " [1024]");
            roleId.set(success.getId());
            callback.accept(roleId.get());
        }, failure -> {
            TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleCreateFailure() + " nation-" + nation.getName() + " [1024]");
            callback.accept(null);
        });
    }

    /**
     * Creates Discord ROLE for TOWN or NATION that doesn't have a Discord ROLE created yet
     */
    public static final void discordRoleCheckAllTownsAllNations() {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();

        List<Role> allRoles = guild.getRoles();
        List<Town> allTowns = new ArrayList<>(TownyUniverse.getInstance().getTowns());
        List<Nation> allNations = new ArrayList<>(TownyUniverse.getInstance().getNations());
        List<Town> townsWithoutRole = new ArrayList<>(allTowns);
        List<Nation> nationsWithoutRole = new ArrayList<>(allNations);
        System.out.println(allTowns);
        System.out.println(allNations);

        forEachRole:
        for (Role role : allRoles) { // allRoles

            for (Town town : allTowns) { // allTowns
                if (("town-" + town.getName()).equalsIgnoreCase(role.getName())) {
                    townsWithoutRole.remove(town); // Removing matches
                    continue forEachRole;
                }
            }

            for (Nation nation : allNations) { // allNations
                if (("nation-" + nation.getName()).equalsIgnoreCase(role.getName())) {
                    nationsWithoutRole.remove(nation); // Removing matches
                    continue forEachRole;
                }
            }
        }

        if (!townsWithoutRole.isEmpty()) { // There are towns that don't have a discord role created yet
            Main.plugin.getLogger().info("Reached townsWithoutRole.isEmpty()");

            for (Town town : townsWithoutRole) {
                RoleAction role = guild.createRole().setName("town-" + town.getName()).setColor(Color.decode(Main.plugin.config.getString("town.RoleCreateColorCode")));
                role.queue(success -> {
                    TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleCreateSuccess() + " town-" + town.getName() + " [17]");
                }, failure -> {
                    TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleCreateFailure() + " town-" + town.getName() + " [17]");
                });
            }
        }

        if (!nationsWithoutRole.isEmpty()) { // There are nations that don't have a discord role created yet
            Main.plugin.getLogger().info("Reached nationsWithoutRole.isEmpty()");

            for (Nation nation : nationsWithoutRole) {
                RoleAction role = guild.createRole().setName("nation-" + nation.getName()).setColor(Color.decode(Main.plugin.config.getString("nation.RoleCreateColorCode")));
                role.queue(success -> {
                    TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleCreateSuccess() + " nation-" + nation.getName() + " [17]");
                }, failure -> {
                    TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleCreateFailure() + " town-" + nation.getName() + " [17]");
                });
            }
        }
    }

    /**
     * Creates Discord TEXT_CHANNEL for TOWN or NATION that doesn't have a Discord TEXT_CHANNEL created yet
     */
    public static final void discordTextChannelCheckAllTownsAllNations() {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();

        List<Town> allTowns = new ArrayList<>(TownyUniverse.getInstance().getTowns());
        List<Nation> allNations = new ArrayList<>(TownyUniverse.getInstance().getNations());
        List<Town> townsWithoutTextChannel = new ArrayList<>(allTowns);
        List<Nation> nationsWithoutTextChannel = new ArrayList<>(allNations);
        List<TextChannel> allTownTextChannels = guild.getCategoryById(getTownTextCategoryId()).getTextChannels();
        List<TextChannel> allNationTextChannels = guild.getCategoryById(getNationTextCategoryId()).getTextChannels();

        Preconditions.checkNotNull(allTowns);
        Preconditions.checkNotNull(allNations);
        Preconditions.checkNotNull(allTownTextChannels);
        Preconditions.checkNotNull(allNationTextChannels);

        forEachTown:
        for (Town town : allTowns) { // allTowns
            for (TextChannel textChannel : allTownTextChannels) { // allTownTextChannels
                if (textChannel.getName().equalsIgnoreCase(town.getName())) {
                    townsWithoutTextChannel.remove(town); // Removing matches
                    continue forEachTown;
                }
            }
        }

        forEachNation:
        for (Nation nation : allNations) { // allNations
            for (TextChannel textChannel : allNationTextChannels) { // allNationTextChannels
                if (textChannel.getName().equalsIgnoreCase(nation.getName())) {
                    nationsWithoutTextChannel.remove(nation); // Removing matches
                    continue forEachNation;
                }
            }
        }


        if (!townsWithoutTextChannel.isEmpty()) { // There are towns that don't have a Discord VoiceChannel created yet
            Main.plugin.getLogger().info("Reached townsWithoutTextChannel.isEmpty()");

            for (Town town : townsWithoutTextChannel) {
                createChannels(guild, town.getName(), guild.getRolesByName("town-" + town.getName(), true).get(0), false, true, null, getTownTextCategoryId());
            }
        }

        if (!nationsWithoutTextChannel.isEmpty()) { // There are nations that don't have a Discord VoiceChannel created yet
            Main.plugin.getLogger().info("Reached nationsWithoutVoiceChannel.isEmpty()");

            for (Nation nation : nationsWithoutTextChannel) {
                createChannels(guild, nation.getName(), guild.getRolesByName("nation-" + nation.getName(), true).get(0), false, true, null, getNationTextCategoryId());
            }
        }
    }

    /**
     * Creates Discord VOICE_CHANNEL for TOWN or NATION that doesn't have a Discord VOICE_CHANNEL created yet
     */
    public static final void discordVoiceChannelCheckAllTownsAllNations() {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();

        List<Town> allTowns = new ArrayList<>(TownyUniverse.getInstance().getTowns());
        List<Nation> allNations = new ArrayList<>(TownyUniverse.getInstance().getNations());
        List<Town> townsWithoutVoiceChannel = new ArrayList<>(allTowns);
        List<Nation> nationsWithoutVoiceChannel = new ArrayList<>(allNations);
        List<VoiceChannel> allTownVoiceChannels = guild.getCategoryById(getTownVoiceCategoryId()).getVoiceChannels();
        List<VoiceChannel> allNationVoiceChannels = guild.getCategoryById(getNationVoiceCategoryId()).getVoiceChannels();

        Preconditions.checkNotNull(allTowns);
        Preconditions.checkNotNull(allNations);
        Preconditions.checkNotNull(allTownVoiceChannels);
        Preconditions.checkNotNull(allNationVoiceChannels);

        forEachTown:
        for (Town town : allTowns) { // allTowns
            for (VoiceChannel voiceChannel : allTownVoiceChannels) { // allTownVoiceChannels
                if (voiceChannel.getName().equalsIgnoreCase(town.getName())) {
                    townsWithoutVoiceChannel.remove(town); // Removing matches
                    continue forEachTown;
                }
            }
        }

        forEachNation:
        for (Nation nation : allNations) { // allNations
            for (VoiceChannel voiceChannel : allNationVoiceChannels) { // allNationVoiceChannels
                if (voiceChannel.getName().equalsIgnoreCase(nation.getName())) {
                    nationsWithoutVoiceChannel.remove(nation); // Removing matches
                    continue forEachNation;
                }
            }
        }


        if (!townsWithoutVoiceChannel.isEmpty()) { // There are towns that don't have a Discord VoiceChannel created yet
            Main.plugin.getLogger().info("Reached townsWithoutVoiceChannel.isEmpty()");

            for (Town town : townsWithoutVoiceChannel) {
                createChannels(guild, town.getName(), guild.getRolesByName("town-" + town.getName(), true).get(0), true, false, getTownVoiceCategoryId(), null);
            }
        }

        if (!nationsWithoutVoiceChannel.isEmpty()) { // There are nations that don't have a Discord VoiceChannel created yet
            Main.plugin.getLogger().info("Reached nationsWithoutVoiceChannel.isEmpty()");

            for (Nation nation : nationsWithoutVoiceChannel) {
                createChannels(guild, nation.getName(), guild.getRolesByName("nation-" + nation.getName(), true).get(0), true, false, getNationVoiceCategoryId(), null);
            }
        }
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

        getRole(roleprefix + oldName).getManager().setName(roleprefix + newName).queue(success -> {
            TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleRenameSuccess() + " " + roleprefix + oldName + " to " + roleprefix + newName + " [18]");
        }, failure -> {
            TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleRenameFailure() + " " + roleprefix + oldName + " to " + roleprefix + newName + " [18]");
        });

        List<TextChannel> discordTextChannels = guild.getTextChannelsByName(oldName, true);
        for (TextChannel discordTextChannel : discordTextChannels) {
            if (townTextCategoryId == null || discordTextChannel.getParent().getId().equals(townTextCategoryId)) {
                discordTextChannel.getManager().setName(newName).queue(success -> {
                    TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgTextChannelRenameSuccess() + " " + oldName + " to " + newName + " [19]");
                }, failure -> {
                    TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgTextChannelRenameFailure() + " " + oldName + " to " + newName + " [19]");
                });
            }
        }

        List<VoiceChannel> discordVoiceChannels = guild.getVoiceChannelsByName(oldName, true);
        for (VoiceChannel discordVoiceChannel : discordVoiceChannels) {
            if (townVoiceCategoryId == null || discordVoiceChannel.getParent().getId().equals(townVoiceCategoryId)) {
                discordVoiceChannel.getManager().setName(newName).queue(success -> {
                    TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgVoiceChannelRenameSuccess() + " " + oldName + " to " + newName + " [20]");
                }, failure -> {
                    TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgVoiceChannelRenameFailure() + " " + oldName + " to " + newName + " [20]");
                });
            }
        }
    }

    public static void deleteRole(Role role, String name) {
        role.delete().queue(success -> {
            TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleDeleteSuccess() + " " + name + " [1025]");
        }, failure -> {
            TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleDeleteFailure() + " " + name + " [1025]");
        });
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
        deleteRoleAndChannels("town-" + townName, getRole("town-" + townName), getTownTextCategoryId(), getTownVoiceCategoryId());
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
        deleteRoleAndChannels("nation-" + nationName, getRole("nation-" + nationName), getNationTextCategoryId(),
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
            role.delete().queue(success -> {
                TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleDeleteSuccess() + " " + name + " [21]");
            }, failure -> {
                TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleDeleteFailure() + " " + name + " [21]");
            });

        final List<TextChannel> discordTextChannels = guild.getTextChannelsByName(name.substring(name.indexOf("-") + 1), true);
        for (final TextChannel discordTextChannel : discordTextChannels) {
            if (textChannelParentId == null || discordTextChannel.getParent().getId().equals(textChannelParentId)) {
                discordTextChannel.delete().queue(success -> {
                    TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgTextChannelDeleteSuccess() + " " + name.substring(name.indexOf("-") + 1) + " [22]");
                }, failure -> {
                    TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgTextChannelDeleteFailure() + " " + name.substring(name.indexOf("-") + 1) + " [22]");
                });
            }
        }

        final List<VoiceChannel> discordVoiceChannels = guild.getVoiceChannelsByName(name.substring(name.indexOf("-") + 1), true);
        for (final VoiceChannel discordVoiceChannel : discordVoiceChannels) {
            if (voiceChannelParentId == null || discordVoiceChannel.getParent().getId().equals(voiceChannelParentId)) {
                discordVoiceChannel.delete().queue(success -> {
                    TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgVoiceChannelDeleteSuccess() + " " + name.substring(name.indexOf("-") + 1) + " [23]");
                }, failure -> {
                    TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgVoiceChannelDeleteFailure() + " " + name.substring(name.indexOf("-") + 1) + " [23]");
                });
            }
        }
    }

    /**
     * Removes the Role of the Town the Player is in from the Player
     *
     * @param offlinePlayer the Player to remove the role from
     */
    public static final void removePlayerTownRole(@NotNull final OfflinePlayer offlinePlayer) {

        // get the Players Town
        final Town town = getTown(offlinePlayer);
        // check if the player is in a town
        if (town == null) {
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, "You're not in a town!");
            return;
        }

        removePlayerRole(offlinePlayer, town);
    }

    public static final void removePlayerRole(@NotNull final UUID UUID, @NotNull final Town town) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID);
        removePlayerRole(offlinePlayer, town);
    }

    /**
     * Removes the Towns Discord Role from the Player
     *
     * @param offlinePlayer The Player from which the role should be removed
     * @param town          The Town which role should be removed from the Player
     */
    public static final void removePlayerRole(@NotNull final OfflinePlayer offlinePlayer, @NotNull final Town town) {

        // get the LinkedId
        final String linkedId = getLinkedId(offlinePlayer);
        // check if the player has linked their account
        if (linkedId == null) {
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, "You haven't linked your Discord, do /discord link to get started!");
            return;
        }

        // get the discord member from the id
        final Member member = getMember(linkedId);
        // check if the member is in the discord
        if (member == null) {
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, "You are not in the Discord server!");
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
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, "You have been removed from the discord " + town + " channels!");
        }
    }

    public static final void removePlayerRole(@NotNull final UUID UUID, @NotNull final Nation nation) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID);
        removePlayerRole(offlinePlayer, nation);
    }

    /**
     * Removes the Nations Discord Role from the Player
     *
     * @param offlinePlayer The Player from which the role should be removed
     * @param nation        The Town which role should be removed from the Player
     */
    public static final void removePlayerRole(@NotNull final OfflinePlayer offlinePlayer, @NotNull final Nation nation) {

        // get the LinkedId
        final String linkedId = getLinkedId(offlinePlayer);
        // check if the player has linked their account
        if (linkedId == null) {
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, "You haven't linked your Discord, do /discord link to get started!");
            return;
        }

        // get the discord member from the id
        final Member member = getMember(linkedId);
        // check if the member is in the discord
        if (member == null) {
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, "You are not in the Discord server!");
            return;
        }

        // get the towns discord role
        final Role nationRole = getRole(nation);

        // check if the Nation Role exists and if the player has the nation role
        if (nationRole != null && member.getRoles().contains(nationRole)) {

            // remove the nation role from the player
            DiscordUtil.removeRolesFromMember(member, nationRole);

            // notify the player on discord
            DiscordUtil.privateMessage(member.getUser(),
                    "You have been removed from the discord " + nation + " channels!");

            // notify the player in-game
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, "You have been removed from the discord " + nation + " channels!");
        }
    }

    /**
     * Gives the Player it towns Discord Role
     *
     * @param offlinePlayer offlinePlayer to give the role to
     */
    public static final void givePlayerNationRole(@NotNull final OfflinePlayer offlinePlayer) {

        // Get the Players Town
        final Nation nation = getNation(offlinePlayer);
        // check if the player is in a town
        if (nation == null) {
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, "You're not in a nation!");
            return;
        }
        givePlayerRole(offlinePlayer, nation);
    }

    public static final void givePlayerRole(@NotNull final UUID UUID, @NotNull final Nation nation) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID);
        givePlayerRole(offlinePlayer, nation);
    }

    /**
     * Gives the Player the Nations Discord Role
     *
     * @param offlinePlayer offlinePlayer to give the Role to
     * @param nation        The nation which role the Player should get
     */
    public static final void givePlayerRole(@NotNull final OfflinePlayer offlinePlayer, @NotNull final Nation nation) {

        // get the LinkedId
        String linkedId = getLinkedId(offlinePlayer);
        // checking to see if the player's minecraft account is linked with their
        // discord account
        if (linkedId == null) {
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, "You haven't linked your Discord, do /discord link to get started!");
            return;
        }

        // getting the Discord Member instance
        final Member member = getMember(linkedId);
        // check if the member is in the discord
        if (member == null) {
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, "You are not in the Discord server!");
            return;
        }

        // get the towns Role
        final Role townRole = getRole(nation);
        // check if the role exists
        if (townRole != null) {
            // give the player the town role if it exists
            giveRoleToMember(offlinePlayer, member, townRole);
        } else {
            // create the town role if it does not exist
            createRole(offlinePlayer, member, nation);
        }
    }

    /**
     * Gives the Player it towns Discord Role
     *
     * @param offlinePlayer offlinePlayer to give the role to
     */
    public static final void givePlayerTownRole(@NotNull final OfflinePlayer offlinePlayer) {

        // Get the Players Town
        final Town town = getTown(offlinePlayer);
        // check if the player is in a town
        if (town == null) {
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, "You're not in a town!");
            return;
        }
        givePlayerRole(offlinePlayer, town);
    }

    public static final void givePlayerRole(@NotNull final UUID UUID, @NotNull final Town town) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID);
        givePlayerRole(offlinePlayer, town);
    }

    /**
     * Gives the Player the Towns Discord Role
     *
     * @param offlinePlayer offlinePlayer to give the Role to
     * @param town          The town which role the Player should get
     */
    public static final void givePlayerRole(@NotNull final OfflinePlayer offlinePlayer, @NotNull final Town town) {

        // get the LinkedId
        final String linkedId = getLinkedId(offlinePlayer);
        // checking to see if the player's minecraft account is linked with their
        // discord account
        if (linkedId == null) {
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, "You haven't linked your Discord, do '/discord link' to get started!");
            return;
        }

        // getting the Discord Member instance
        final Member member = getMember(linkedId);
        // check if the member is in the discord
        if (member == null) {
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, "You are not in the Discord server!");
            return;
        }

        // get the towns Role
        final Role townRole = getRole(town);
        // check if the role exists
        if (townRole != null) {
            // give the player the town role if it exists
            giveRoleToMember(offlinePlayer, member, townRole);
        } else {
            // create the town role if it does not exist
            createRole(offlinePlayer, member, town);
        }
    }

    /**
     * Gives the player the role and notifies the Player accordingly
     *
     * @param offlinePlayer Player to notify
     * @param member        Member to give the role to
     * @param townRole      The role to give to the Member
     */
    private static void giveRoleToMember(@NotNull final OfflinePlayer offlinePlayer, @NotNull final Member member,
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
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, "You are already a part of the " + townRole.getName() + " role!");
        } else {
            // give the member the role
            DiscordUtil.addRolesToMember(member, townRole);

            // notify on discord
            DiscordUtil.privateMessage(member.getUser(), "Your account has been linked to "
                    + townRole.getName().substring(townRole.getName().indexOf('-') + 1) + "!");
            // notify in-game
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, "Your account has been linked to "
                    + townRole.getName().substring(townRole.getName().indexOf('-') + 1) + "!");
        }
    }

    /**
     * Creates Towns role, creates the corresponding channels and gives the Member
     * the role
     *
     * @param offlinePlayer The Player who initiated the creation
     * @param member        The member corresponding to the Player
     * @param town          The Town to create the role for
     */
    private static void createRole(@NotNull final OfflinePlayer offlinePlayer, @NotNull final Member member,
                                   @NotNull final Town town) {
        final Guild guild = member.getGuild();
        if (Main.plugin.config.getBoolean("town.CreateRoleIfNoneExists")) {
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, town + " Doesn't have a Role, automatically creating one for you...!");
            guild.createRole().setName("town-" + town.getName())
                    .setColor(Color.decode(Main.plugin.config.getString("town.RoleCreateColorCode"))).queue(role -> {
                DiscordUtil.addRolesToMember(member, role);
                createChannels(guild, town, role);

                TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleCreateSuccess() + " town-" + town.getName() + " [24]");
            }, failure -> {
                TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleCreateFailure() + " town-" + town.getName() + " [24]");
            });
        }
    }

    /**
     * Creates Nations role, creates the corresponding channels and gives the
     * Member the role
     *
     * @param offlinePlayer The Player who initiated the creation
     * @param member        The member corresponding to the Player
     * @param nation        The Nation to create the role for
     */
    private static void createRole(@NotNull final OfflinePlayer offlinePlayer, @NotNull final Member member,
                                   @NotNull final Nation nation) {
        final Guild guild = member.getGuild();
        if (Main.plugin.config.getBoolean("nation.CreateRoleIfNoneExists")) {
            TDCMessages.sendMessageToPlayerGame(offlinePlayer, nation + " Doesn't have a Role, automatically creating one for you...!");
            guild.createRole().setName("nation-" + nation.getName())
                    .setColor(Color.decode(Main.plugin.config.getString("nation.RoleCreateColorCode"))).queue(role -> {
                giveRoleToMember(offlinePlayer, member, role);
                createChannels(guild, nation, role);

                TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleCreateSuccess() + " nation-" + nation.getName() + " [25]");
            }, failure -> {
                TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgRoleCreateFailure() + " nation-" + nation.getName() + " [25]");
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

        Preconditions.checkNotNull(guild, "Channel creation error! @param guild null!");
        Preconditions.checkNotNull(name, "Channel creation error! @param name null!");
        Preconditions.checkNotNull(role, "Channel creation error! @param role null!");

        if (createVoiceChannel) {
            final ChannelAction<VoiceChannel> voiceChannelAction = guild.createVoiceChannel(name)
                    .addRolePermissionOverride(everyoneRoleId, noPermission, viewPermission)
                    .addRolePermissionOverride(roleId, viewPermission, noPermission)
                    .addMemberPermissionOverride(botId, viewPermission, noPermission);
            if (voiceChannelCategoryId != null) {
                voiceChannelAction.setParent(guild.getCategoryById(voiceChannelCategoryId));
            }
            voiceChannelAction.queue(success -> {
                TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgVoiceChannelCreateSuccess() + " " + name + " [26]");
            }, failure -> {
                TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgVoiceChannelCreateFailure() + " " + name + " [26]");
            });
        }
        if (createTextChannel) {
            final ChannelAction<TextChannel> textChannelAction = guild.createTextChannel(name)
                    .addRolePermissionOverride(everyoneRoleId, noPermission, viewPermission)
                    .addRolePermissionOverride(roleId, viewPermission, noPermission)
                    .addMemberPermissionOverride(botId, viewPermission, noPermission);
            if (textChannelCategoryId != null) {
                textChannelAction.setParent(guild.getCategoryById(textChannelCategoryId));
            }
            textChannelAction.queue(success -> {
                TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgTextChannelCreateSuccess() + " " + name + " [27]");
            }, failure -> {
                TDCMessages.sendMessageToDiscordLogChannel(TDCMessages.getConfigMsgTextChannelCreateFailure() + " " + name + " [27]");
            });
        }
    }

    /**
     * Gets the Discord ID of the Player
     *
     * @param offlinePlayer offlinePlayer to get the Discord ID from
     * @return The Discord ID or null when the player did not link their Discord
     */
    public static @Nullable String getLinkedId(@NotNull final OfflinePlayer offlinePlayer) {
        return DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(offlinePlayer.getUniqueId());
    }

    /**
     * Gets the List<Member> instance of the Discord ID from the Main Guild
     *
     * @return The Members or null if no members in the discord
     */
    private static @Nullable List<Member> getMembers() {
        return DiscordSRV.getPlugin().getMainGuild().getMembers();
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
     * @param offlinePlayer The Player to get the Town from
     * @return The Players Town or null if the Player is in no Town
     */
    private static @Nullable Town getTown(@NotNull final OfflinePlayer offlinePlayer) {
        try {
            final Resident resident = TownyUniverse.getInstance().getResident(offlinePlayer.getUniqueId());
            return resident.getTown();
        } catch (final NotRegisteredException e) {
            return null;
        }
    }

    /**
     * Gets the Nation of the Player
     *
     * @param offlinePlayer The Player to get the Nation from
     * @return The Players Nation or null if the Player is in no Nation
     */
    private static @Nullable Nation getNation(@NotNull final OfflinePlayer offlinePlayer) {

        Town town = getTown(offlinePlayer);
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
        Role role = null;

        //DiscordUtil.getRoleName(Role role)
        //return DiscordUtil.getRoleByName(DiscordSRV.getPlugin().getMainGuild(), name);
        try {role = DiscordUtil.getJda().getRolesByName(name, true).get(0); } catch (Exception e) {}

        return role;
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