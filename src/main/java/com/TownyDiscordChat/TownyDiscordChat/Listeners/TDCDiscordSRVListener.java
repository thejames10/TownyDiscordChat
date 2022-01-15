package com.TownyDiscordChat.TownyDiscordChat.Listeners;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.Towns;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.api.events.AccountUnlinkedEvent;
import org.bukkit.OfflinePlayer;
import java.util.UUID;

public class TDCDiscordSRVListener {

    @Subscribe
    public void accountLinked(AccountLinkedEvent event) {

        System.out.println("AccountLinkedEvent fired!");

        OfflinePlayer offlinePlayer = event.getPlayer();

        if (event.getUser().isBot() /*&& !offlinePlayer.hasPlayedBefore()*/) {
            return;
        }

        String discordId = event.getUser().getId();
        UUID UUID = offlinePlayer.getUniqueId();
        Resident resident = TownyUniverse.getInstance().getResident(UUID);
        boolean isMayor = false;
        boolean isKing = false;
        if (resident != null) {
            isMayor = resident.isMayor();
            isKing = resident.isKing();
        }

        if (Main.plugin.playersDB.entryExists(UUID.toString(), "UUID")) { // Entry already exists
        } else { // Doesn't exist

            Main.plugin.playersDB.createEntry(UUID, discordId,"EMPTY","EMPTY",
                    "EMPTY","EMPTY","EMPTY",
                    "EMPTY",Boolean.toString(isMayor), Boolean.toString(isKing));
        }

        //TDCManager.discordUserRoleCheck(discordId, UUID);
    }

    @Subscribe
    public void accountUnlinked(AccountUnlinkedEvent event) {
        System.out.println("AccountUnlinkedEvent fired!");

        OfflinePlayer offlinePlayer = event.getPlayer();

        String UUID = offlinePlayer.getUniqueId().toString();
        Main.plugin.playersDB.updateEntry("EMPTY","discordUserId", UUID, "UUID");
        Main.plugin.playersDB.updateEntry("EMPTY","townRoleName", UUID, "UUID");
        Main.plugin.playersDB.updateEntry("EMPTY","townRoleId", UUID, "UUID");
        Main.plugin.playersDB.updateEntry("EMPTY","townTextChannelId", UUID, "UUID");
        Main.plugin.playersDB.updateEntry("EMPTY","townVoiceChannelId", UUID, "UUID");
        Main.plugin.playersDB.updateEntry("EMPTY","nationTextChannelId", UUID, "UUID");
        Main.plugin.playersDB.updateEntry("EMPTY","nationVoiceChannelId", UUID, "UUID");
    }
}
