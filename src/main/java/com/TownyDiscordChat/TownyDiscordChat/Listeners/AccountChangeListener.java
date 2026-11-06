package com.TownyDiscordChat.TownyDiscordChat.Listeners;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.api.events.AccountUnlinkedEvent;
import org.bukkit.Bukkit;

/** Request a single reconciled final state after account linking/unlinking. */
public final class AccountChangeListener {
    private final Main plugin;
    public AccountChangeListener(Main plugin) { this.plugin = plugin; }

    @Subscribe
    public void onLinked(AccountLinkedEvent event) {
        final java.util.UUID uuid=event.getPlayer().getUniqueId();
        changed(uuid);
    }

    @Subscribe
    public void onUnlinked(AccountUnlinkedEvent event) { changed(null); }

    private void changed(java.util.UUID linkedUuid) {
        if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin, () -> {
            if (plugin.reconciler != null) plugin.reconciler.requestSoon();
            if (linkedUuid!=null && plugin.onboarding!=null)
                plugin.onboarding.onAccountLinked(linkedUuid);
        });
    }
}
