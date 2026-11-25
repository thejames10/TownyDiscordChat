package com.TownyDiscordChat.TownyDiscordChat.Diagnostics;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.Queue.DiscordJobStore;
import com.TownyDiscordChat.TownyDiscordChat.Queue.MemberIntentStore;
import com.TownyDiscordChat.TownyDiscordChat.Scheduling.TaskSchedulers;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Writes plugins/TownyDiscordChat/debug/debug-*.txt for bug reports: versions,
 * plugin state, the Discord setup check, failed queue work and config.yml with
 * credentials hidden. Server state is read first; storage is queried off-thread.
 */
public final class DebugReport {
    private static final String FOLDER = "debug";
    private static final String FILE_PREFIX = "debug-";
    private static final String FILE_SUFFIX = ".txt";
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int FAILED_JOB_LIMIT = 25;
    private static final String DISCORDSRV = "DiscordSRV";
    private static final String[] DEPENDENCIES = {"Towny", DISCORDSRV, "TownyChat"};

    private final Main plugin;

    public DebugReport(Main plugin) {
        this.plugin = plugin;
    }

    public void write(CommandSender sender) {
        plugin.messages().send(sender, "debug.writing");
        List<String> report = serverState();
        plugin.scheduler().runAsync(() -> finish(sender, report));
    }

    private void finish(CommandSender sender, List<String> report) {
        try {
            report.addAll(storageState());
            Path file = save(report);
            reply(sender, "debug.written", "file", plugin.getDataFolder().getParentFile().toPath()
                    .relativize(file).toString());
        } catch (IOException | RuntimeException error) {
            plugin.getLogger().log(Level.WARNING, "Could not write the debug report", error);
            reply(sender, "debug.failed", "error", String.valueOf(error.getMessage()));
        }
    }

    private void reply(CommandSender sender, String key, String... placeholders) {
        if (plugin.isEnabled()) {
            plugin.scheduler().runGlobal(() -> plugin.messages().send(sender, key, placeholders));
        }
    }

    private List<String> serverState() {
        List<String> lines = new ArrayList<>();
        lines.add("TownyDiscordChat support report, " + ZonedDateTime.now(ZoneOffset.UTC));
        section(lines, "Plugin");
        lines.add("Version: " + plugin.getDescription().getVersion());
        lines.add("Status: " + plugin.statusLabel());
        lines.add("Safe mode reason: " + plugin.getStartupProblem());
        lines.add("Admin warning: " + plugin.getAdminWarning());
        lines.add("Discord setup check passed: " + plugin.isGuildValidated());
        lines.add("Storage: " + (plugin.SQL == null ? "not configured" : plugin.SQL.getBackend()));
        addServer(lines);
        addPlugins(lines);
        addDiscord(lines);
        section(lines, "config.yml");
        lines.addAll(ConfigRedactor.lines(plugin.config));
        return lines;
    }

    private static void addServer(List<String> lines) {
        section(lines, "Server");
        lines.add("Software: " + Bukkit.getName() + " " + Bukkit.getVersion());
        lines.add("API: " + Bukkit.getBukkitVersion());
        lines.add("Folia: " + TaskSchedulers.isFolia());
        lines.add("Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        lines.add("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
    }

    private static void addPlugins(List<String> lines) {
        section(lines, "Plugins");
        for (String name : DEPENDENCIES) {
            Plugin dependency = Bukkit.getPluginManager().getPlugin(name);
            lines.add(name + ": " + describe(dependency));
        }
        StringBuilder all = new StringBuilder("All: ");
        for (Plugin installed : Bukkit.getPluginManager().getPlugins()) {
            all.append(installed.getName()).append(' ').append(describe(installed)).append("; ");
        }
        lines.add(all.toString());
    }

    private static String describe(Plugin installed) {
        if (installed == null) {
            return "not installed";
        }
        return installed.getDescription().getVersion() + (installed.isEnabled() ? "" : " (disabled)");
    }

    private void addDiscord(List<String> lines) {
        section(lines, "Discord");
        if (!Bukkit.getPluginManager().isPluginEnabled(DISCORDSRV)) {
            lines.add("DiscordSRV is not enabled.");
            return;
        }
        try {
            lines.addAll(DiscordDiagnostics.lines(plugin.config));
        } catch (Throwable unavailable) {
            lines.add("Could not read Discord state: " + unavailable);
        }
    }

    private List<String> storageState() {
        List<String> lines = new ArrayList<>();
        section(lines, "Queue");
        if (plugin.SQL == null) {
            lines.add("Storage is not configured.");
            return lines;
        }
        try {
            DiscordJobStore jobs = new DiscordJobStore(plugin);
            MemberIntentStore members = new MemberIntentStore(plugin);
            lines.add("Resource jobs: " + jobs.statusCounts());
            lines.add("Membership changes: " + members.statusCounts());
            lines.add("Most recent failed resource jobs:");
            lines.addAll(indented(jobs.failedJobSummaries(FAILED_JOB_LIMIT)));
            lines.add("Most recent failed membership changes:");
            lines.addAll(indented(members.failedIntentSummaries(FAILED_JOB_LIMIT)));
        } catch (SQLException error) {
            lines.add("Could not read the queue: " + error.getMessage());
        }
        return lines;
    }

    private static List<String> indented(List<String> entries) {
        List<String> lines = new ArrayList<>();
        for (String entry : entries) {
            lines.add("  " + entry);
        }
        if (lines.isEmpty()) {
            lines.add("  none");
        }
        return lines;
    }

    private static void section(List<String> lines, String title) {
        lines.add("");
        lines.add("== " + title + " ==");
    }

    private Path save(List<String> report) throws IOException {
        Path folder = plugin.getDataFolder().toPath().resolve(FOLDER);
        Files.createDirectories(folder);
        String name = FILE_PREFIX + ZonedDateTime.now(ZoneOffset.UTC).format(FILE_TIME) + FILE_SUFFIX;
        return Files.write(folder.resolve(name), report, StandardCharsets.UTF_8);
    }
}
