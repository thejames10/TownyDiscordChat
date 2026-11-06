package com.TownyDiscordChat.TownyDiscordChat.Schedulers.Queue;

import com.TownyDiscordChat.TownyDiscordChat.Core.TDCManager;
import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.Queue.DiscordJobStore;
import com.TownyDiscordChat.TownyDiscordChat.Queue.DiscordJobStore.Job;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Category;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.VoiceChannel;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Single-flight Discord dispatcher. The DB poll runs asynchronously; Towny and
 * Bukkit lookups happen on the server thread; JDA owns REST rate-limit handling.
 * No blocking .complete(), artificial rate-limit arithmetic, or API calls in a loop.
 */
public final class Queue {
    private final Main plugin;
    private final DiscordJobStore store;
    private final BukkitTask scheduler;
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private volatile Job current;

    public Queue(long startDelaySeconds, long intervalDelaySeconds, Main plugin) {
        this.plugin = plugin;
        this.store = new DiscordJobStore(plugin);
        long delay = Math.max(1, startDelaySeconds);
        long period = Math.max(1, intervalDelaySeconds);
        this.scheduler = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                new Runnable() {
                    @Override public void run() { tick(); }
                }, delay * 20L, period * 20L);
    }

    private void tick() {
        Job active = current;
        if (active != null && System.currentTimeMillis() >= active.leaseUntil) {
            plugin.getLogger().warning("Discord job lease timed out: " + active
                    + "; releasing worker. A late callback will be ignored by SQL.");
            current = null;
            busy.set(false);
        }
        if (!busy.compareAndSet(false, true)) return;

        try {
            final Job job = store.claim();
            if (job == null) {
                busy.set(false);
                return;
            }
            current = job;
            final Set<String> categoryIds = job.kind.endsWith("CATEGORY")
                    ? store.trackedCategoryIds(job.kind) : Collections.<String>emptySet();
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override public void run() { dispatch(job, categoryIds); }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Queue poll failed: " + e.getMessage());
            current = null;
            busy.set(false);
        }
    }

    private void dispatch(final Job job, Set<String> categoryIds) {
        try {
            Guild guild = DiscordSRV.getPlugin().getMainGuild();
            if (guild == null) {
                defer(job, "DiscordSRV guild is not ready");
                return;
            }
            switch (job.kind) {
                case "TOWN_ROLE": {
                    Town town = TownyUniverse.getInstance().getTown(job.name);
                    if (town == null) { discard(job); return; }
                    Role role = first(guild.getRolesByName("town-" + job.name, true));
                    if (role != null) { success(job, role.getId(), null); return; }
                    TDCManager.createDiscordRole(town, id -> result(job, id, null));
                    break;
                }
                case "NATION_ROLE": {
                    Nation nation = TownyUniverse.getInstance().getNation(job.name);
                    if (nation == null) { discard(job); return; }
                    Role role = first(guild.getRolesByName("nation-" + job.name, true));
                    if (role != null) { success(job, role.getId(), null); return; }
                    TDCManager.createDiscordRole(nation, id -> result(job, id, null));
                    break;
                }
                case "TOWN_CATEGORY":
                case "NATION_CATEGORY": {
                    for (Category category : guild.getCategoriesByName(job.name, true)) {
                        if (!categoryIds.contains(category.getId())) {
                            // Reconcile a Discord category created just before a crash.
                            success(job, category.getId(), null);
                            return;
                        }
                    }
                    TDCManager.createChatCategory(job.name, id -> result(job, id, null));
                    break;
                }
                case "TOWN_TEXT":
                case "NATION_TEXT":
                case "TOWN_VOICE":
                case "NATION_VOICE":
                    dispatchChannel(job, guild);
                    break;
                default:
                    fail(job, "Unrecognized task kind: " + job.kind);
            }
        } catch (Exception e) {
            fail(job, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void dispatchChannel(Job job, Guild guild) {
        boolean town = job.kind.startsWith("TOWN");
        boolean text = job.kind.endsWith("TEXT");
        String prefix = town ? "town-" : "nation-";
        Role role = first(guild.getRolesByName(prefix + job.name, true));
        if (role == null) {
            defer(job, "Waiting for Discord role " + prefix + job.name);
            return;
        }
        String categoryName = plugin.getConfig().getString(
                "category." + (town ? "Town" : "Nation") + ".Name", "");
        List<Category> categories = guild.getCategoriesByName(categoryName, true);
        if (categories.isEmpty()) {
            defer(job, "Waiting for " + categoryName + " category");
            return;
        }

        // After an interrupted API callback, reuse the previously created
        // channel instead of making duplicates. Match both parent and role.
        if (text) {
            for (TextChannel channel : guild.getTextChannelsByName(job.name, true)) {
                if (channel.getParent() != null
                        && categoryName.equalsIgnoreCase(channel.getParent().getName())
                        && channel.getPermissionOverride(role) != null) {
                    success(job, channel.getId(), channel.getParent().getId());
                    return;
                }
            }
        } else {
            for (VoiceChannel channel : guild.getVoiceChannelsByName(job.name, true)) {
                if (channel.getParent() != null
                        && categoryName.equalsIgnoreCase(channel.getParent().getName())
                        && channel.getPermissionOverride(role) != null) {
                    success(job, channel.getId(), channel.getParent().getId());
                    return;
                }
            }
        }

        int maxChannels = Math.max(1, Math.min(50, plugin.getConfig().getInt("category.SubChannelsMax", 50)));
        Category target = null;
        for (Category category : categories) {
            if (category.getChannels().size() < maxChannels) {
                target = category;
                break;
            }
        }
        if (target == null) {
            defer(job, "All " + categoryName + " categories are full; waiting for the next category");
            return;
        }
        final String parent = target.getId();
        Consumer<String> finished = id -> result(job, id, parent);
        if (town && text) TDCManager.createTownTextChannel(job.name, role.getId(), parent, finished::accept);
        else if (town) TDCManager.createTownVoiceChannel(job.name, role.getId(), parent, finished::accept);
        else if (text) TDCManager.createNationTextChannel(job.name, role.getId(), parent, finished::accept);
        else TDCManager.createNationVoiceChannel(job.name, role.getId(), parent, finished::accept);
    }

    private static <T> T first(List<T> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    private void result(Job job, String id, String parent) {
        if (id == null || id.trim().isEmpty()) fail(job, "Discord REST action failed; see DiscordSRV logs");
        else success(job, id, parent);
    }

    private void success(Job job, String id, String parent) {
        finalizeAsync(job, () -> store.succeed(job, id, parent), "Completed " + job);
    }

    private void discard(Job job) {
        // A deleted Towny entity must never cause an unbounded retry loop.
        finalizeAsync(job, () -> store.remove(job), "Discarded missing Towny entity " + job);
    }

    private void defer(Job job, String reason) {
        finalizeAsync(job, () -> store.defer(job, reason), "Deferred " + job + ": " + reason);
    }

    private void fail(Job job, String reason) {
        finalizeAsync(job, () -> store.fail(job, reason), "Failed " + job + ": " + reason);
    }

    private interface SQLAction { void run() throws SQLException; }

    private void finalizeAsync(final Job job, final SQLAction action, final String message) {
        if (!plugin.isEnabled()) return; // lease will be recovered after restart
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override public void run() {
                try {
                    action.run();
                    plugin.getLogger().info(message);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Could not persist Discord job " + job
                            + ": " + e.getMessage() + "; lease will be retried");
                } finally {
                    // A callback may arrive after a newer attempt has started.
                    if (current == job) {
                        current = null;
                        busy.set(false);
                    }
                }
            }
        });
    }

    public void cancel() {
        scheduler.cancel();
        // RUNNING leases are intentionally left for recovery on next startup.
    }
}
