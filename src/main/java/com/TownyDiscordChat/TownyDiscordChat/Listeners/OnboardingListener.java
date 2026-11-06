package com.TownyDiscordChat.TownyDiscordChat.Listeners;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Contextual player onboarding. DiscordSRV remains the sole authority for
 * account links, codes and unlink operations. Never generate our own code.
 */
public final class OnboardingListener implements Listener {
    private final Main plugin;
    private final Map<UUID,Long> lastPrompt = new HashMap<UUID,Long>();

    public OnboardingListener(Main plugin) { this.plugin=plugin; }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("onboarding.remindOnJoin",true)) return;
        schedule(event.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onTownJoin(TownAddResidentEvent event) {
        if (!plugin.getConfig().getBoolean("onboarding.remindOnTownJoin",true)) return;
        Player player=Bukkit.getPlayer(event.getResident().getUUID());
        if (player!=null) schedule(player);
    }

    private void schedule(final Player player) {
        if (!plugin.isOperational()
                || !plugin.getConfig().getBoolean("onboarding.enabled",true)) return;
        long delay=plugin.getConfig().getLong("onboarding.joinDelayTicks",60L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> remindIfNeeded(player), delay);
    }

    public boolean hasTown(Player player) {
        try {
            Resident resident=TownyUniverse.getInstance().getResident(player.getUniqueId());
            return resident!=null && resident.hasTown();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLinked(UUID uuid) {
        return DiscordSRV.getPlugin().getAccountLinkManager()!=null
                && DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid)!=null;
    }

    private void remindIfNeeded(Player player) {
        if (!player.isOnline() || !plugin.isOperational() || !hasTown(player)
                || isLinked(player.getUniqueId())) return;
        long now=System.currentTimeMillis();
        long timeout=Math.max(1,plugin.getConfig().getLong("onboarding.cooldownMinutes",60L))*60000L;
        Long last=lastPrompt.get(player.getUniqueId());
        if (last!=null && now-last<timeout) return;
        lastPrompt.put(player.getUniqueId(),now);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("onboarding.message",
                        "&eYour town has private Discord channels. Use &a/tdc link&e to link.")));
    }

    public void showStatus(Player player) {
        Resident resident=TownyUniverse.getInstance().getResident(player.getUniqueId());
        if (resident==null || !resident.hasTown()) {
            player.sendMessage(ChatColor.YELLOW+"[TDC] You are not in a town. No Discord linking is required for Towny channels yet.");
            return;
        }
        String townName="unknown";
        String nationName=null;
        try {
            Town town=resident.getTown();
            townName=town.getName();
            if (town.hasNation()) nationName=town.getNation().getName();
        } catch (NotRegisteredException e) {
            player.sendMessage(ChatColor.YELLOW+"[TDC] Towny is updating your membership. Try again shortly.");
            return;
        }
        player.sendMessage(ChatColor.GREEN+"[TDC] Town: "+ChatColor.WHITE+townName
                +(nationName!=null ? ChatColor.GREEN+" | Nation: "+ChatColor.WHITE+nationName : ""));
        if (isLinked(player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN+"[TDC] Your Minecraft and Discord accounts are linked through DiscordSRV."
                    +ChatColor.GRAY+" Access is synchronized by the queue; use /discord linked for details.");
        } else {
            player.sendMessage(ChatColor.YELLOW+"[TDC] Your account is not linked. Use "
                    +ChatColor.AQUA+"/tdc link"+ChatColor.YELLOW+" to start DiscordSRV linking.");
        }
    }

    /**
     * An explicit player request delegates to the canonical DiscordSRV command;
     * TDC never creates an independent link or stores a link token.
     */
    public void link(Player player) {
        if (isLinked(player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN+"[TDC] Already linked via DiscordSRV. "
                    +ChatColor.GRAY+"Use /discord linked to view the connection.");
            return;
        }
        if (!hasTown(player)) {
            player.sendMessage(ChatColor.YELLOW+"[TDC] You aren't in a Towny town yet. "
                    +"You can still link your account, but there's no town channel to unlock until you join a town.");
        }
        if (!player.hasPermission("discordsrv.link")) {
            player.sendMessage(ChatColor.RED+"[TDC] Your server has disabled DiscordSRV linking "
                    +"(permission discordsrv.link). Contact a server administrator.");
            return;
        }
        if (Bukkit.getPluginCommand("discord")==null) {
            player.sendMessage(ChatColor.RED+"[TDC] DiscordSRV's /discord command is unavailable. "
                    +"Ask an administrator to check DiscordSRV.");
            return;
        }
        Bukkit.dispatchCommand(player,"discord link");
    }

    public void onAccountLinked(UUID uuid) {
        if (!plugin.isOperational() || uuid==null) return;
        Player player=Bukkit.getPlayer(uuid);
        if (player==null || !player.isOnline() || !hasTown(player)) return;
        player.sendMessage(ChatColor.GREEN+"[TDC] Discord linked successfully through DiscordSRV. "
                +"Your Towny channel access will synchronize automatically.");
    }
}
