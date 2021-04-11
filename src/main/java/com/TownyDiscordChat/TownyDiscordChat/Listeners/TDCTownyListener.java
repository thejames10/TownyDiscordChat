package com.TownyDiscordChat.TownyDiscordChat.Listeners;

import java.util.List;
import java.util.Timer;
import java.util.UUID;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.TDCManager;
import com.google.common.base.Preconditions;
import com.palmergames.bukkit.towny.event.*;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.util.DiscordUtil;

// see javadocs for all towny listeners
// https://javadoc.jitpack.io/com/github/TownyAdvanced/Towny/0.96.5.0/javadoc/

public class TDCTownyListener implements Listener {

    public TDCTownyListener(Main plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onNewDay(NewDayEvent event) {

        System.out.println("NewDayEvent fired!");

        TDCManager.discordRoleCheckAllTownsAllNations();

        TDCManager.discordTextChannelCheckAllTownsAllNations();

        TDCManager.discordVoiceChannelCheckAllTownsAllNations();

        Timer t = new java.util.Timer();
        t.schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("Running delayed task...");
                        TDCManager.discordUserRoleCheckAllLinked();
                        //t.cancel();
                    }
                },
                300000

        );
    }

    @EventHandler
    public void onPlayerJoinTown(TownAddResidentEvent event) {

        Main.plugin.getLogger().info("TownAddResidentEvent fired!");

        UUID UUID = event.getResident().getUUID();
        Town town = event.getTown();

        Preconditions.checkNotNull(UUID);
        Preconditions.checkNotNull(town);

        TDCManager.givePlayerRole(UUID, town);

        if (town.hasNation()) {
            Nation nation = null;
            try {
                nation = town.getNation();
            } catch (NotRegisteredException e) {
                e.printStackTrace();
            }
            Preconditions.checkNotNull(nation);
            TDCManager.givePlayerRole(UUID, nation);
        }
    }

    @EventHandler
    public void onPlayerLeaveTown(TownRemoveResidentEvent event) {

        Main.plugin.getLogger().info("TownRemoveResidentEvent fired!");

        UUID UUID = event.getResident().getUUID();
        Town town = event.getTown();

        Preconditions.checkNotNull(UUID);
        Preconditions.checkNotNull(town);

        TDCManager.removePlayerRole(UUID, town);

        if (town.hasNation()) {
            Nation nation = null;
            try {
                nation = town.getNation();
            } catch (NotRegisteredException e) {
                e.printStackTrace();
            }
            Preconditions.checkNotNull(nation);
            TDCManager.removePlayerRole(UUID, nation);
        }
    }

    @EventHandler
    public void onTownJoinNation(NationAddTownEvent event) {

        Main.plugin.getLogger().info("NationAddTownEvent fired!");

        List<Resident> townResidents = event.getTown().getResidents();
        for (Resident townResident : townResidents) {
            TDCManager.givePlayerRole(townResident.getUUID(), event.getNation());
        }
    }

    @EventHandler
    public void onTownLeaveNation(NationRemoveTownEvent event) {

        System.out.println("NationRemoveTownEvent fired!");

        for (Resident townResident : event.getTown().getResidents()) {
            TDCManager.removePlayerRole(townResident.getUUID(), event.getNation());
        }
    }

    @EventHandler
    public void onRenameTown(RenameTownEvent event) {

        System.out.println("RenameTownEvent fired!");

        final String OLD_NAME = event.getOldName();
        final String NEW_NAME = event.getTown().getName();

        Guild guild = DiscordSRV.getPlugin().getMainGuild();

        TDCManager.renameTown(OLD_NAME, NEW_NAME);

        final String DISCORDSRV_GLOBAL_CHANNEL_ID = DiscordSRV.getPlugin().getChannels().get("global");
        DiscordUtil.sendMessage(guild.getTextChannelById(DISCORDSRV_GLOBAL_CHANNEL_ID),
                OLD_NAME + " text and voice channels have been renamed to " + NEW_NAME + "!");

    }

    @EventHandler
    public void onRenameNation(RenameNationEvent event) {

        System.out.println("RenameNationEvent fired!");

        final String OLD_NAME = event.getOldName();
        final String NEW_NAME = event.getNation().getName();

        Guild guild = DiscordSRV.getPlugin().getMainGuild();

        TDCManager.renameNation(OLD_NAME, NEW_NAME);

        final String DISCORDSRV_GLOBAL_CHANNEL_ID = DiscordSRV.getPlugin().getChannels().get("global");
        DiscordUtil.sendMessage(guild.getTextChannelById(DISCORDSRV_GLOBAL_CHANNEL_ID),
                OLD_NAME + " text and voice channels have been renamed to " + NEW_NAME + "!");

    }

    @EventHandler
    public void onDeleteTown(DeleteTownEvent event) {

        System.out.println("DeleteTownEvent fired!");

        Guild guild = DiscordSRV.getPlugin().getMainGuild();

        TDCManager.deleteRoleAndChannelsFromTown(event.getTownName());

        final String DISCORDSRV_GLOBAL_CHANNEL_ID = DiscordSRV.getPlugin().getChannels().get("global");
        DiscordUtil.sendMessage(guild.getTextChannelById(DISCORDSRV_GLOBAL_CHANNEL_ID),
                event.getTownName() + " text and voice channels have been removed");

    }

    @EventHandler
    public void onDeleteNation(DeleteNationEvent event) {

        System.out.println("DeleteNationEvent fired!");

        Guild guild = DiscordSRV.getPlugin().getMainGuild();

        TDCManager.deleteRoleAndChannelsFromNation(event.getNationName());

        final String DISCORDSRV_GLOBAL_CHANNEL_ID = DiscordSRV.getPlugin().getChannels().get("global");
        DiscordUtil.sendMessage(guild.getTextChannelById(DISCORDSRV_GLOBAL_CHANNEL_ID),
                event.getNationName() + " text and voice channels have been removed");

    }
}
