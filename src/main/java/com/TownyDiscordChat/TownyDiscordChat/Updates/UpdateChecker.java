package com.TownyDiscordChat.TownyDiscordChat.Updates;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.Permissions;
import com.TownyDiscordChat.TownyDiscordChat.Scheduling.RepeatingTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Asks GitHub every 12 hours whether a newer release exists and tells the
 * console and players with townydiscordchat.notify. Nothing is downloaded.
 */
public final class UpdateChecker implements Listener {
    private static final String RELEASES_URL =
            "https://api.github.com/repos/thejames10/TownyDiscordChat/releases?per_page=30";
    private static final String CHECK_KEY = "updates.check";
    private static final String PRE_RELEASES_KEY = "updates.includePreReleases";
    private static final int TIMEOUT_MILLISECONDS = 10000;
    private static final int READ_BUFFER_BYTES = 8192;
    private static final int HTTP_OK = 200;
    private static final long TICKS_PER_SECOND = 20L;
    private static final long FIRST_CHECK_DELAY_TICKS = 60L * TICKS_PER_SECOND;
    private static final long RECHECK_TICKS = 12L * 60L * 60L * TICKS_PER_SECOND;
    // Lets the join messages of other plugins appear first.
    private static final long JOIN_NOTICE_DELAY_TICKS = 3L * TICKS_PER_SECOND;

    private final Main plugin;
    private RepeatingTask task;
    private volatile PublishedRelease available;
    private volatile ReleaseVersion announced;

    public UpdateChecker(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!plugin.getConfig().getBoolean(CHECK_KEY, true)) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        task = plugin.scheduler().runAsyncTimer(this::check, FIRST_CHECK_DELAY_TICKS, RECHECK_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        HandlerList.unregisterAll(this);
    }

    private void check() {
        try {
            ReleaseVersion current = ReleaseVersion.parse(plugin.getDescription().getVersion());
            PublishedRelease newest = PublishedRelease.newest(download(),
                    plugin.getConfig().getBoolean(PRE_RELEASES_KEY, false));
            if (current == null || newest == null || !newest.version().isNewerThan(current)) {
                return;
            }
            available = newest;
            if (!newest.version().equals(announced)) {
                announced = newest.version();
                announce(newest, current);
            }
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().fine("Update check failed: " + failure.getMessage());
        }
    }

    private void announce(PublishedRelease release, ReleaseVersion current) {
        plugin.getLogger().warning("TownyDiscordChat " + release.version() + " is available (this server has "
                + current + "): " + release.url());
        plugin.scheduler().runGlobal(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                notify(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (available != null && player.hasPermission(Permissions.NOTIFY)) {
            plugin.scheduler().runGlobalLater(() -> notify(player), JOIN_NOTICE_DELAY_TICKS);
        }
    }

    private void notify(Player player) {
        PublishedRelease release = available;
        if (release == null || !player.isOnline() || !player.hasPermission(Permissions.NOTIFY)) {
            return;
        }
        plugin.messages().send(player, "update.available", "latest", release.version().toString(),
                "current", plugin.getDescription().getVersion(), "url", release.url());
    }

    private String download() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(RELEASES_URL).openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLISECONDS);
        connection.setReadTimeout(TIMEOUT_MILLISECONDS);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "TownyDiscordChat/" + plugin.getDescription().getVersion());
        try {
            if (connection.getResponseCode() != HTTP_OK) {
                throw new IOException("GitHub answered HTTP " + connection.getResponseCode());
            }
            try (InputStream body = connection.getInputStream()) {
                return readAll(body);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String readAll(InputStream input) throws IOException {
        ByteArrayOutputStream contents = new ByteArrayOutputStream();
        byte[] buffer = new byte[READ_BUFFER_BYTES];
        int read;
        while ((read = input.read(buffer)) != -1) {
            contents.write(buffer, 0, read);
        }
        return new String(contents.toByteArray(), StandardCharsets.UTF_8);
    }
}
