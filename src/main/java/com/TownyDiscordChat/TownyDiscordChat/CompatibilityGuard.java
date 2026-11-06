package com.TownyDiscordChat.TownyDiscordChat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compatibility preflight runs BEFORE loading any DiscordSRV/Towny integration
 * classes. Incompatible installations remain enabled in an inert safe mode,
 * allowing console and in-game admin warnings without stack traces.
 */
public final class CompatibilityGuard implements Listener {
    private final Main plugin;
    private String problem;

    public CompatibilityGuard(Main plugin) { this.plugin = plugin; }

    public String problem() { return problem; }

    public boolean check() {
        Plugin discord = plugin.getServer().getPluginManager().getPlugin("DiscordSRV");
        Plugin towny = plugin.getServer().getPluginManager().getPlugin("Towny");
        if (discord == null || !discord.isEnabled()) {
            problem = "DiscordSRV missing or disabled. Install compatible DiscordSRV 1.24.0+ and restart.";
            return false;
        }
        if (towny == null || !towny.isEnabled()) {
            problem = "Towny missing or disabled. Install a compatible Towny release and restart.";
            return false;
        }
        String discordVersion = discord.getDescription().getVersion();
        String townyVersion = towny.getDescription().getVersion();
        if (compare(discordVersion, "1.24.0") < 0) {
            problem = "Outdated DiscordSRV " + discordVersion
                    + ": TownyDiscordChat requires 1.24.0+ with the legacy JDA API.";
            return false;
        }
        if (compare(townyVersion, "0.97.5.0") < 0) {
            problem = "Outdated Towny " + townyVersion
                    + ": TownyDiscordChat requires Towny 0.97.5.0+.";
            return false;
        }

        try {
            ClassLoader loader = discord.getClass().getClassLoader();
            Class<?> guild = Class.forName(
                    "github.scarsz.discordsrv.dependencies.jda.api.entities.Guild", false, loader);
            Class<?> discordClass = Class.forName("github.scarsz.discordsrv.DiscordSRV", false, loader);
            discordClass.getMethod("getMainGuild");
            guild.getMethod("getRolesByName", String.class, boolean.class);
            guild.getMethod("createRole");
            guild.getMethod("retrieveMemberById", String.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            problem = "DiscordSRV " + discordVersion + " does not expose the legacy JDA API"
                    + " required by this build. Install a compatible TownyDiscordChat/DiscordSRV"
                    + " combination and restart. Detail: " + e.getClass().getSimpleName();
            return false;
        }
        problem = null;
        return true;
    }

    private static int compare(String found, String minimum) {
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+){1,3})").matcher(found);
        if (!matcher.find()) return -1;
        String[] a = matcher.group(1).split("\\.");
        String[] b = minimum.split("\\.");
        for (int i=0; i<Math.max(a.length,b.length); i++) {
            int av = i < a.length ? Integer.parseInt(a[i]) : 0;
            int bv = i < b.length ? Integer.parseInt(b[i]) : 0;
            if (av != bv) return Integer.compare(av,bv);
        }
        return 0;
    }

    public void warnConsole() {
        if (problem != null) plugin.getLogger().severe(
                "SAFE MODE: " + problem + " No Towny/Discord role or channel changes will be made.");
    }

    @EventHandler
    public void adminJoin(PlayerJoinEvent event) {
        if (problem != null) {
            Player player = event.getPlayer();
            if (player.isOp() || player.hasPermission("TownyDiscordChat.Admin")) {
                player.sendMessage(ChatColor.RED + "[TownyDiscordChat] SAFE MODE: " + problem);
            }
        }
    }
}
