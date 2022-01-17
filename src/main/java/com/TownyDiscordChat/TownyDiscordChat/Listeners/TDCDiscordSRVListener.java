package com.TownyDiscordChat.TownyDiscordChat.Listeners;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.api.events.AccountUnlinkedEvent;
import github.scarsz.discordsrv.dependencies.google.common.base.Preconditions;
import org.bukkit.OfflinePlayer;

public class TDCDiscordSRVListener {

    @Subscribe
    public void accountLinked(AccountLinkedEvent event) {
        Main.plugin.getLogger().info("AccountLinkedEvent fired!");

        OfflinePlayer offlinePlayer = event.getPlayer();

        if (event.getUser().isBot() /*&& !offlinePlayer.hasPlayedBefore()*/) {
            return;
        }

        String discordId = event.getUser().getId();
        String UUID = offlinePlayer.getUniqueId().toString();

        Preconditions.checkNotNull(discordId);
        Preconditions.checkNotNull(UUID);

        Main.plugin.playersDB.updateEntry(discordId, "discordUserId", UUID, "UUID");
    }

    @Subscribe
    public void accountUnlinked(AccountUnlinkedEvent event) {
        Main.plugin.getLogger().info("AccountUnlinkedEvent fired!");

        String UUID = event.getPlayer().getUniqueId().toString();

        Preconditions.checkNotNull(UUID);

        Main.plugin.playersDB.updateEntry("EMPTY","discordUserId", UUID, "UUID");
    }
}
