package com.TownyDiscordChat.TownyDiscordChat.Listeners;

import com.TownyDiscordChat.TownyDiscordChat.TDCManager;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class TDCDiscordSRVListener {

    @Subscribe
    public void accountLinked(AccountLinkedEvent event) {

        System.out.println("AccountLinkedEvent fired!");

        OfflinePlayer offlinePlayer = event.getPlayer();

        if (event.getUser().isBot() && !offlinePlayer.hasPlayedBefore()) {
            return;
        }

        Player player = offlinePlayer.getPlayer();

        TDCManager.givePlayerTownRole(player);
        TDCManager.givePlayerNationRole(player);

    }
}
