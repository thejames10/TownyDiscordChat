package com.TownyDiscordChat.TownyDiscordChat;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Category;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optional Discord-side activity log for durable SQL queue results.
 * No Discord calls occur inside SQL transactions. Posts are JDA async actions
 * after the database job is acknowledged; console logging always remains on.
 * Uses existing private channel names, never an unverified numeric legacy ID.
 */
public final class DiscordActivityLog {
    private final Main plugin;
    private final AtomicBoolean missingWarned=new AtomicBoolean(false);
    private final AtomicBoolean sendWarned=new AtomicBoolean(false);
    private volatile boolean announced;

    public DiscordActivityLog(Main plugin) { this.plugin=plugin; }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("log.Enabled",true);
    }

    /** Announce the selected channel once per start after guild preflight. */
    public void onGuildReady(Guild guild) {
        if (!enabled() || announced) return;
        TextChannel channel=findPrivateChannel(guild);
        if (channel==null) return;
        announced=true;
        plugin.getLogger().info("[DISCORD LOG] Sending changes and failures to #"
                +channel.getName()+" in "+channel.getParent().getName()+".");
        send(channel,"INFO","SQL synchronization logging is active. "
                +"Discord changes and queue failures will appear here.");
    }

    /**
     * change=true for actual Discord mutations and errors, not for a
     * no-op/reconciled resource that already existed. No replay on restart.
     */
    public void publish(String level,String description,boolean change) {
        if (!enabled() || !change || !plugin.isEnabled()
                || !plugin.isGuildValidated()) return;
        Guild guild=DiscordSRV.getPlugin().getMainGuild();
        if (guild==null) return;
        TextChannel channel=findPrivateChannel(guild);
        if (channel!=null) send(channel,level,description);
    }

    private TextChannel findPrivateChannel(Guild guild) {
        String categoryName=plugin.getConfig().getString(
                "log.Category.Name","MINECRAFT-STAFF");
        String channelName=plugin.getConfig().getString(
                "log.Channel.Name","townydiscordchat-logs");
        List<TextChannel> matches=new ArrayList<TextChannel>();
        for (TextChannel candidate:guild.getTextChannelsByName(channelName,true)) {
            Category parent=candidate.getParent();
            if (parent==null || !parent.getName().equalsIgnoreCase(categoryName))
                continue;
            if (parent.getPermissionOverride(guild.getPublicRole())==null
                    || !parent.getPermissionOverride(guild.getPublicRole())
                        .getDenied().contains(Permission.VIEW_CHANNEL)) continue;
            if (candidate.getPermissionOverride(guild.getPublicRole())!=null
                    && candidate.getPermissionOverride(guild.getPublicRole())
                        .getAllowed().contains(Permission.VIEW_CHANNEL)) continue;
            matches.add(candidate);
        }
        if (matches.size()!=1 || !matches.get(0).canTalk()) {
            if (missingWarned.compareAndSet(false,true))
                plugin.getLogger().warning("[DISCORD LOG] Unable to post: expected "
                    +"exactly one private, writable #"+channelName
                    +" under category "+categoryName+". Check bot View Channel/"
                    +"Send Messages and the category's @everyone visibility. "
                    +"Queue processing and console logs will continue.");
            return null;
        }
        missingWarned.set(false);
        return matches.get(0);
    }

    private void send(TextChannel channel,String level,String message) {
        // Queue names are user-configurable; strip line breaks and neutralize
        // mention syntax so a Towny name can never ping a Discord role/user.
        String safe=message.replace('\r',' ').replace('\n',' ')
                .replace("@","@\u200B");
        if (safe.length()>1700) safe=safe.substring(0,1700)+"...";
        channel.sendMessage("[TDC " + level + "] " + safe).queue(
                ignored -> sendWarned.set(false),
                error -> {
                    if (sendWarned.compareAndSet(false,true))
                        plugin.getLogger().warning("[DISCORD LOG] Message delivery failed "
                                +"to #"+channel.getName()+": "+error.getMessage());
                });
    }
}
