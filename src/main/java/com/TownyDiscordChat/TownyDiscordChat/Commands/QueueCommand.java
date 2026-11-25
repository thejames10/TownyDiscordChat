package com.TownyDiscordChat.TownyDiscordChat.Commands;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.Messages.MessageCatalog;
import com.TownyDiscordChat.TownyDiscordChat.Queue.DiscordJobStore;
import com.TownyDiscordChat.TownyDiscordChat.Queue.MemberIntentStore;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** /tdcqueue: outstanding work, retrying failed jobs and full member audits. */
public final class QueueCommand implements TabExecutor {
    private static final String STATUS = "status";
    private static final String RETRY_DEAD = "retry-dead";
    private static final String AUDIT = "audit";
    private static final List<String> ACTIONS = Arrays.asList(STATUS, RETRY_DEAD, AUDIT);
    private static final String PENDING = "PENDING";
    private static final String RUNNING = "RUNNING";
    private static final String DEAD = "DEAD";

    private final Main plugin;

    public QueueCommand(Main plugin) {
        this.plugin = plugin;
    }

    private MessageCatalog messages() {
        return plugin.messages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        String action = arguments.length == 0 ? STATUS : arguments[0].toLowerCase(Locale.ROOT);
        if (!ACTIONS.contains(action)) {
            messages().send(sender, "queue.usage");
            return true;
        }
        // Read the guild before going async; storage queries then run off-thread.
        String guildId = AUDIT.equals(action) ? connectedGuildId() : null;
        plugin.scheduler().runAsync(() -> answer(sender, action, guildId));
        return true;
    }

    private static String connectedGuildId() {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        return guild == null ? null : guild.getId();
    }

    private void answer(CommandSender sender, String action, String guildId) {
        try {
            if (AUDIT.equals(action)) {
                requestAudit(sender, guildId);
            } else if (RETRY_DEAD.equals(action)) {
                retryDead(sender);
            } else {
                showStatus(sender);
            }
        } catch (SQLException error) {
            plugin.getLogger().warning("Queue command failed: " + error.getMessage());
            reply(sender, "queue.failed");
        }
    }

    private void requestAudit(CommandSender sender, String guildId) throws SQLException {
        if (guildId == null) {
            reply(sender, "queue.guild-not-ready");
            return;
        }
        new DiscordJobStore(plugin).requestFullAudit(guildId);
        reply(sender, "queue.audit-queued");
    }

    private void retryDead(CommandSender sender) throws SQLException {
        int resources = new DiscordJobStore(plugin).retryDead();
        int members = new MemberIntentStore(plugin).retryDead();
        reply(sender, "queue.requeued", "resources", String.valueOf(resources),
                "members", String.valueOf(members));
    }

    private void showStatus(CommandSender sender) throws SQLException {
        Map<String, Integer> resources = new DiscordJobStore(plugin).statusCounts();
        Map<String, Integer> members = new MemberIntentStore(plugin).statusCounts();
        String state = messages().text(plugin.isGuildValidated()
                ? "queue.state-active" : "queue.state-waiting");
        String activityLog = plugin.activityLog == null ? "-" : plugin.activityLog.status();
        reply(sender, "queue.status", "state", state, "activityLog", activityLog,
                "resourcesPending", count(resources, PENDING),
                "resourcesRunning", count(resources, RUNNING),
                "resourcesDead", count(resources, DEAD),
                "membersPending", count(members, PENDING),
                "membersRunning", count(members, RUNNING),
                "membersDead", count(members, DEAD));
    }

    private static String count(Map<String, Integer> counts, String status) {
        return String.valueOf(counts.getOrDefault(status, 0));
    }

    private void reply(CommandSender sender, String key, String... placeholders) {
        if (plugin.isEnabled()) {
            plugin.scheduler().runGlobal(() -> messages().send(sender, key, placeholders));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias,
                                      String[] arguments) {
        if (arguments.length != 1) {
            return Collections.emptyList();
        }
        List<String> matches = new ArrayList<>();
        for (String action : ACTIONS) {
            if (action.startsWith(arguments[0].toLowerCase(Locale.ROOT))) {
                matches.add(action);
            }
        }
        return matches;
    }
}
