package com.TownyDiscordChat.TownyDiscordChat.Listeners;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.TDCManager;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.api.events.AccountUnlinkedEvent;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.bukkit.OfflinePlayer;
import java.util.UUID;

public class TDCDiscordSRVListener {

    @Subscribe
    public void accountLinked(AccountLinkedEvent event) {

        System.out.println("AccountLinkedEvent fired!");

        OfflinePlayer offlinePlayer = event.getPlayer();

        if (event.getUser().isBot() && !offlinePlayer.hasPlayedBefore()) {
            return;
        }

        Guild guild = DiscordSRV.getPlugin().getMainGuild();

        String discordId = event.getUser().getId();
        UUID UUID = offlinePlayer.getUniqueId();

        if (Main.plugin.SQL.isConnected()) {
            String textChannelId = Main.plugin.data.getTextChannelId(discordId);
            if (textChannelId != null) {
                guild.getTextChannelById(textChannelId).delete().queue( success -> {
                    if (Main.plugin.SQL.isConnected()) {
                        Main.plugin.data.deleteEntry(discordId);
                    }
                });
            }
        }

        TDCManager.discordUserRoleCheck(discordId, UUID);
    }

    @Subscribe
    public void onReady(DiscordReadyEvent event) {
        DiscordSRV.getPlugin().getJda().addEventListener(new TDCJDAListener());
    }
}
