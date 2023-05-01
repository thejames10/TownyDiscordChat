package com.TownyDiscordChat.TownyDiscordChat.Listeners;

import java.util.List;
import java.util.Timer;
import java.util.UUID;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.Core.TDCManager;
import com.google.common.base.Strings;
import com.palmergames.bukkit.towny.event.*;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;

import com.palmergames.bukkit.towny.object.Town;
import github.scarsz.discordsrv.dependencies.google.common.base.Preconditions;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;

// see javadocs for all towny listeners
// https://javadoc.jitpack.io/com/github/TownyAdvanced/Towny/0.96.5.0/javadoc/

public class TDCTownyListener implements Listener {

    public TDCTownyListener(Main plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onNewDay(NewDayEvent event) {
        Main.plugin.getLogger().info("NewDayEvent fired!");
    }

    @EventHandler
    public void onPlayerJoinTown(TownAddResidentEvent event) {
        Main.plugin.getLogger().info("TownAddResidentEvent fired!");

        String UUID = event.getResident().getUUID().toString();
        String townName = event.getTown().getName();

        Preconditions.checkNotNull(UUID);
        Preconditions.checkNotNull(townName);

        Main.plugin.playersDB.updateEntry(townName, "townRoleName", UUID, "UUID");

        if (event.getTown().hasNation()) {
            try {
                Main.plugin.playersDB.updateEntry(event.getTown().getNation().getName(), "nationRoleName", UUID, "UUID");
            } catch (NotRegisteredException e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onPlayerLeaveTown(TownRemoveResidentEvent event) {
        Main.plugin.getLogger().info("TownRemoveResidentEvent fired!");

        String UUID = event.getResident().getUUID().toString();

        Preconditions.checkNotNull(UUID);

        Main.plugin.playersDB.updateEntry("EMPTY", "townRoleName", UUID, "UUID");

        if (event.getResident().hasNation()) {
            Main.plugin.playersDB.updateEntry("EMPTY", "nationRoleName", UUID, "UUID");
        }
    }

    @EventHandler
    public void onTownJoinNation(NationAddTownEvent event) {
        Main.plugin.getLogger().info("NationAddTownEvent fired!");

        String nationName = event.getNation().getName();

        List<Resident> townResidents = event.getTown().getResidents();

        Preconditions.checkNotNull(townResidents);

        for (Resident townResident : townResidents) {
            String UUID = townResident.getUUID().toString();

            Preconditions.checkNotNull(UUID);

            Main.plugin.playersDB.updateEntry(nationName, "nationRoleName", UUID, "UUID");
        }
    }

    @EventHandler
    public void onTownLeaveNation(NationRemoveTownEvent event) {
        Main.plugin.getLogger().info("NationRemoveTownEvent fired!");

        String nationName = event.getNation().getName();

        Preconditions.checkNotNull(nationName);

        Main.plugin.playersDB.updateEntry("EMPTY", "nationRoleName", nationName, "nationRoleName");
    }

    @EventHandler
    public void onRenameTown(RenameTownEvent event) {
        Main.plugin.getLogger().info("RenameTownEvent fired!");

        String OLD_NAME = event.getOldName();
        String NEW_NAME = event.getTown().getName();

        Preconditions.checkNotNull(OLD_NAME);
        Preconditions.checkNotNull(NEW_NAME);

        Main.plugin.townRolesDB.updateEntry(NEW_NAME, "townRoleName", OLD_NAME, "townRoleName");
        Main.plugin.playersDB.updateEntry(NEW_NAME, "townRoleName", OLD_NAME, "townRoleName");
    }

    @EventHandler
    public void onRenameNation(RenameNationEvent event) {
        Main.plugin.getLogger().info("RenameNationEvent fired!");

        String OLD_NAME = event.getOldName();
        String NEW_NAME = event.getNation().getName();

        Preconditions.checkNotNull(OLD_NAME);
        Preconditions.checkNotNull(NEW_NAME);

        Main.plugin.nationRolesDB.updateEntry(NEW_NAME, "nationRoleName", OLD_NAME, "nationRoleName");
        Main.plugin.playersDB.updateEntry(NEW_NAME, "nationRoleName", OLD_NAME, "nationRoleName");
    }

    @EventHandler
    public void onNewTownCreated(NewTownEvent event) {
        Main.plugin.getLogger().info("NewTownEvent fired!");

        Town town = event.getTown();
        String townName = town.getName();
        String townUUID = town.getUUID().toString();

        Preconditions.checkNotNull(town);
        Preconditions.checkNotNull(townName);
        Preconditions.checkNotNull(townUUID);

        TDCManager.createDiscordRole(town);

        Main.plugin.townRolesDB.createEntry(townName, townUUID);
    }

    @EventHandler
    public void onNewNationCreated(NewNationEvent event) {
        Main.plugin.getLogger().info("NewNationEvent fired!");

        Nation nation = event.getNation();
        String nationName = nation.getName();
        String nationUUID = nation.getUUID().toString();

        Preconditions.checkNotNull(nation);
        Preconditions.checkNotNull(nationName);
        Preconditions.checkNotNull(nationUUID);

        TDCManager.createDiscordRole(nation);

        Main.plugin.nationRolesDB.createEntry(nationName, nationUUID);
    }

    @EventHandler
    public void onDeleteTown(DeleteTownEvent event) {
        Main.plugin.getLogger().info("DeleteTownEvent fired!");

        String townName = event.getTownName();

        Preconditions.checkNotNull(townName);

        //DiscordSRV.getPlugin().getMainGuild().getRoleById()

        //TDCManager.deleteRole();

        Main.plugin.townRolesDB.deleteEntry(townName, "townRoleName");

        Main.plugin.playersDB.updateEntry("EMPTY", "townRoleName", townName, "townRoleName");
    }

    @EventHandler
    public void onDeleteNation(DeleteNationEvent event) {
        Main.plugin.getLogger().info("DeleteNationEvent fired!");

        String nationName = event.getNationName();

        Preconditions.checkNotNull(nationName);

        Main.plugin.nationRolesDB.deleteEntry(nationName, "nationRoleName");

        Main.plugin.playersDB.updateEntry("EMPTY", "nationRoleName", nationName, "nationRoleName");
    }
}
