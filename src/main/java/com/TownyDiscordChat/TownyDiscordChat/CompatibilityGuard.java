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
    private static final String MISSING_DISCORD =
            "DiscordSRV missing or disabled. Install compatible DiscordSRV 1.24.0+ and restart.";
    private static final String MISSING_TOWNY =
            "Towny missing or disabled. Install a compatible Towny release and restart.";

    private final Main plugin;
    private String problem;

    public CompatibilityGuard(Main plugin) { this.plugin = plugin; }

    public String problem() { return problem; }

    public boolean check() {
        Plugin discord = plugin.getServer().getPluginManager().getPlugin("DiscordSRV");
        Plugin towny = plugin.getServer().getPluginManager().getPlugin("Towny");
        problem = missingDependencyProblem(
                discord != null && discord.isEnabled(),
                towny != null && towny.isEnabled());
        if (problem != null) {
            return false;
        }
        String discordVersion = discord.getDescription().getVersion();
        String serverName = plugin.getServer().getName().toLowerCase(java.util.Locale.ROOT);
        String townyVersion = towny.getDescription().getVersion();
        if (compare(discordVersion, "1.24.0") < 0) {
            problem = "Outdated DiscordSRV " + discordVersion
                    + ": TownyDiscordChat requires 1.24.0+ with the legacy JDA API.";
            return false;
        }
        if (serverName.contains("folia")) {
            problem = "Folia requires a region-aware scheduler, which this test build does not yet provide. "
                    + "Use supported Paper/Purpur for now.";
            return false;
        }
        if (compare(townyVersion, "0.102.0.0") >= 0
                && !serverName.contains("paper") && !serverName.contains("purpur")) {
            problem = "Towny 0.102.0.0+ requires Paper or Purpur with this plugin. "
                    + "Spigot/CraftBukkit and unknown server types are unsupported. "
                    + "Detected server: " + plugin.getServer().getName();
            return false;
        }
        if (compare(discordVersion, "1.31.0") >= 0) {
            problem = "DiscordSRV " + discordVersion + " is newer than the verified 1.30.x API family. "
                    + "Use 1.30.5 for testing until compatibility is checked.";
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
            guild.getMethod("loadMembers", java.util.function.Consumer.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            problem = "DiscordSRV " + discordVersion + " does not expose the legacy JDA API"
                    + " required by this build. Install a compatible TownyDiscordChat/DiscordSRV"
                    + " combination and restart. Detail: " + e.getClass().getSimpleName();
            return false;
        }
        problem = null;
        return true;
    }

    static String missingDependencyProblem(boolean discordReady, boolean townyReady) {
        if (discordReady && townyReady) {
            return null;
        }
        if (!discordReady && !townyReady) {
            return MISSING_DISCORD + " " + MISSING_TOWNY;
        }
        return discordReady ? MISSING_TOWNY : MISSING_DISCORD;
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
        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission("TownyDiscordChat.Admin")) {
            if (plugin.getSafeModeReason() != null)
                player.sendMessage(ChatColor.RED + "[TownyDiscordChat] SAFE MODE: " + plugin.getSafeModeReason());
            if (plugin.getAdminWarning() != null)
                player.sendMessage(ChatColor.RED + "[TownyDiscordChat] "
                        + plugin.getAdminWarning());
        }
    }
}
