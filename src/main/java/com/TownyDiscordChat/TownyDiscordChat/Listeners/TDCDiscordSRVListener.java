package com.TownyDiscordChat.TownyDiscordChat.Listeners;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.TDCManager;
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

        Main.plugin.data.createEntry(UUID, discordId,"-1","-1",
                "-1","-1","-1","-1");

        TDCManager.discordUserRoleCheck(discordId, UUID);
    }

    @Subscribe
    public void accountUnlinked(AccountUnlinkedEvent event) {
        System.out.println("AccountUnlinkedEvent fired!");

        OfflinePlayer offlinePlayer = event.getPlayer();

        UUID UUID = offlinePlayer.getUniqueId();
        //Main.plugin.data.update
    }
}
