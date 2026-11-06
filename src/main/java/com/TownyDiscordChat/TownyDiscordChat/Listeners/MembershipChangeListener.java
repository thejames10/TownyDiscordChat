package com.TownyDiscordChat.TownyDiscordChat.Listeners;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
import com.palmergames.bukkit.towny.event.NationAddTownEvent;
import com.palmergames.bukkit.towny.event.NationRemoveTownEvent;
import com.palmergames.bukkit.towny.event.RenameTownEvent;
import com.palmergames.bukkit.towny.event.RenameNationEvent;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.event.NewTownEvent;
import com.palmergames.bukkit.towny.event.NewNationEvent;
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeleteTown(DeleteTownEvent event) { changed(); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeleteNation(DeleteNationEvent event) { changed(); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreateTown(NewTownEvent event) { changed(); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreateNation(NewNationEvent event) { changed(); }
}
