package com.TownyDiscordChat.TownyDiscordChat.Schedulers;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.Queue.DiscordJobStore;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Nation;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Category;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Towny snapshots must run on the Bukkit server thread. Database reconciliation
 * runs on a worker thread; it never accesses Bukkit's world or Towny state.
 */
public final class Reconciler {
    private final Main plugin;
    private final DiscordJobStore store;
    private final BukkitTask task;
    private final AtomicBoolean reconciling = new AtomicBoolean(false);

    public Reconciler(Main plugin) {
        this.plugin = plugin;
        this.store = new DiscordJobStore(plugin);
        long period = Math.max(30L, plugin.getConfig().getLong("queue.reconcileSeconds", 180L));
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override public void run() { snapshot(); }
        }, 100L, period * 20L);
    }

    private void snapshot() {
        if (!reconciling.compareAndSet(false, true)) return;
        try {
            Guild guild = DiscordSRV.getPlugin().getMainGuild();
            if (guild == null) {
                reconciling.set(false);
                return;
            }
            final List<String> towns = new ArrayList<String>();
            final List<String> nations = new ArrayList<String>();
            for (Town town : TownyUniverse.getInstance().getTowns()) towns.add(town.getName());
            for (Nation nation : TownyUniverse.getInstance().getNations()) nations.add(nation.getName());

            final Set<String> roles = new HashSet<String>();
            guild.getRoles().forEach(role -> roles.add(role.getName()));
            final Set<String> textChannels = new HashSet<String>();
            guild.getTextChannels().forEach(channel -> textChannels.add(channel.getName()));
            final Set<String> voiceChannels = new HashSet<String>();
            guild.getVoiceChannels().forEach(channel -> voiceChannels.add(channel.getName()));

            String townCategoryName = plugin.getConfig().getString("category.Town.Name", "MINECRAFT-TOWN-CHAT");
            String nationCategoryName = plugin.getConfig().getString("category.Nation.Name", "MINECRAFT-NATION-CHAT");
            int max = Math.max(1, Math.min(50, plugin.getConfig().getInt("category.SubChannelsMax", 50)));
            int townSlots = 0;
            int nationSlots = 0;
            for (Category cat : guild.getCategories()) {
                if (cat.getName().equalsIgnoreCase(townCategoryName))
                    townSlots += Math.max(0, max - cat.getChannels().size());
                if (cat.getName().equalsIgnoreCase(nationCategoryName))
                    nationSlots += Math.max(0, max - cat.getChannels().size());
            }

            final int townCapacity = townSlots;
            final int nationCapacity = nationSlots;
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override public void run() {
                    try {
                        reconcile(towns, nations, roles, textChannels, voiceChannels,
                                townCapacity, nationCapacity, townCategoryName, nationCategoryName);
                    } catch (SQLException e) {
                        plugin.getLogger().warning("Reconciliation failed: " + e.getMessage());
                    } finally {
                        reconciling.set(false);
                    }
                }
            });
        } catch (Exception e) {
            reconciling.set(false);
            plugin.getLogger().warning("Could not snapshot Towny state: " + e.getMessage());
        }
    }

    private void reconcile(List<String> towns, List<String> nations, Set<String> roles,
                           Set<String> textChannels, Set<String> voiceChannels,
                           int townCapacity, int nationCapacity,
                           String townCategoryName, String nationCategoryName) throws SQLException {
        Set<String> knownTowns = store.knownNames("TOWN_ROLE");
        Set<String> knownNations = store.knownNames("NATION_ROLE");
        Set<String> knownTownText = store.knownNames("TOWN_TEXT");
        Set<String> knownNationText = store.knownNames("NATION_TEXT");
        Set<String> knownTownVoice = store.knownNames("TOWN_VOICE");
        Set<String> knownNationVoice = store.knownNames("NATION_VOICE");

        // A previously recorded role may have been manually deleted in Discord.
        for (String name : towns) {
            if (!roles.contains("town-" + name) || !knownTowns.contains(name))
                store.enqueue("TOWN_ROLE", name);
        }
        for (String name : nations) {
            if (!roles.contains("nation-" + name) || !knownNations.contains(name))
                store.enqueue("NATION_ROLE", name);
        }
        if (!towns.isEmpty() && townCapacity < Math.max(1, towns.size() * 2 - townCapacity)
                && store.pendingCount("TOWN_CATEGORY", townCategoryName) == 0)
            store.enqueue("TOWN_CATEGORY", townCategoryName);
        if (!nations.isEmpty() && nationCapacity < Math.max(1, nations.size() * 2 - nationCapacity)
                && store.pendingCount("NATION_CATEGORY", nationCategoryName) == 0)
            store.enqueue("NATION_CATEGORY", nationCategoryName);

        if (plugin.getConfig().getBoolean("town.CreateTextChannelForRole", true))
            for (String name : towns)
                if (!textChannels.contains(name) || !knownTownText.contains(name))
                    store.enqueue("TOWN_TEXT", name);

        if (plugin.getConfig().getBoolean("nation.CreateTextChannelForRole", true))
            for (String name : nations)
                if (!textChannels.contains(name) || !knownNationText.contains(name))
                    store.enqueue("NATION_TEXT", name);

        if (plugin.getConfig().getBoolean("town.CreateVoiceChannelForRole", true))
            for (String name : towns)
                if (!voiceChannels.contains(name) || !knownTownVoice.contains(name))
                    store.enqueue("TOWN_VOICE", name);

        if (plugin.getConfig().getBoolean("nation.CreateVoiceChannelForRole", true))
            for (String name : nations)
                if (!voiceChannels.contains(name) || !knownNationVoice.contains(name))
                    store.enqueue("NATION_VOICE", name);
    }

    public void cancel() { task.cancel(); }
}
