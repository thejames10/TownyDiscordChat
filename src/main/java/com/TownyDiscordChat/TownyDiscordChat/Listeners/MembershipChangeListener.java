package com.TownyDiscordChat.TownyDiscordChat.Listeners;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
import com.palmergames.bukkit.towny.event.NationAddTownEvent;
import com.palmergames.bukkit.towny.event.NationRemoveTownEvent;
import com.palmergames.bukkit.towny.event.RenameTownEvent;
import com.palmergames.bukkit.towny.event.RenameNationEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Only requests a post-event authoritative snapshot. Never fire-and-forget a
 * REST grant from an event handler; the state may already have changed again.
 */
public final class MembershipChangeListener implements Listener {
    private final Main plugin;
    public MembershipChangeListener(Main plugin) { this.plugin = plugin; }

    private void changed() {
        if (plugin.reconciler != null) plugin.reconciler.requestSoon();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTownJoin(TownAddResidentEvent event) { changed(); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTownLeave(TownRemoveResidentEvent event) { changed(); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNationJoin(NationAddTownEvent event) { changed(); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNationLeave(NationRemoveTownEvent event) { changed(); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRenameTown(RenameTownEvent event) { changed(); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRenameNation(RenameNationEvent event) { changed(); }
}
