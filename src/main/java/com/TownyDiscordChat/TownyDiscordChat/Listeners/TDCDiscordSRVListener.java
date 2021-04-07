package com.TownyDiscordChat.TownyDiscordChat.Listeners;

import com.TownyDiscordChat.TownyDiscordChat.TDCManager;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TDCDiscordSRVListener {

    @Subscribe
    public void accountLinked(AccountLinkedEvent event) {

        System.out.println("AccountLinkedEvent fired!");

        OfflinePlayer offlinePlayer = event.getPlayer();

        if (event.getUser().isBot() && !offlinePlayer.hasPlayedBefore()) {
            return;
        }

        String discordId = event.getUser().getId();
        UUID UUID = offlinePlayer.getUniqueId();

        TDCManager.discordUserRoleCheck(discordId, UUID);
    }
}
