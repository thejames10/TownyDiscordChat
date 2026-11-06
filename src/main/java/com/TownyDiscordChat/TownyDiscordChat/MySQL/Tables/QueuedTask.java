package com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.Queue.DiscordJobStore;
import java.sql.SQLException;

/** Compatibility facade for the SQL-branch reconciliation schedulers. */
public class QueuedTask {
    private final Main plugin;

    public QueuedTask(Main plugin) { this.plugin = plugin; }

    public void createTable() throws SQLException {
        new DiscordJobStore(plugin).initialize();
    }

    private void add(String kind, String name) {
        try {
            new DiscordJobStore(plugin).enqueue(kind, name);
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not enqueue " + kind + ":" + name + ": " + e.getMessage());
        }
    }

    public void createTownRole(String name) { add("TOWN_ROLE", name); }
    public void createNationRole(String name) { add("NATION_ROLE", name); }
    public void createTownCategory(String name) { add("TOWN_CATEGORY", name); }
    public void createNationCategory(String name) { add("NATION_CATEGORY", name); }
    public void createTownTextChannel(String name) { add("TOWN_TEXT", name); }
    public void createNationTextChannel(String name) { add("NATION_TEXT", name); }
    public void createTownVoiceChannel(String name) { add("TOWN_VOICE", name); }
    public void createNationVoiceChannel(String name) { add("NATION_VOICE", name); }

    public int pendingCount(String kind, String name) {
        try {
            return new DiscordJobStore(plugin).pendingCount(kind, name);
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not count queue entries: " + e.getMessage());
            // Fail closed: don't create extra Discord categories if SQL is unavailable.
            return 1;
        }
    }
}
