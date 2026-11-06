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
    private volatile String activeChannel;

    public DiscordActivityLog(Main plugin) { this.plugin=plugin; }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("log.Enabled",true);
    }

    /** Reports current logging health to administrators. */
    public String status() {
        if (!enabled()) return "disabled (log.Enabled: false)";
        if (sendWarned.get()) return "delivery error; check console";
        if (missingWarned.get()) return "unavailable; check private channel/permissions";
        if (announced) return "enabled ("+activeChannel+")";
        return "waiting for Discord connection";
    }

    /** Announce the selected channel once per start after guild preflight. */
    public void onGuildReady(Guild guild) {
        if (!enabled() || announced) return;
        TextChannel channel=findPrivateChannel(guild);
        if (channel==null) return;
        announced=true;
        activeChannel="#"+channel.getName();
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
            // Staff channels may be private through their own override even
            // when the containing staff category is otherwise public.
            boolean categoryDenied=parent.getPermissionOverride(guild.getPublicRole())!=null
                    && parent.getPermissionOverride(guild.getPublicRole())
                        .getDenied().contains(Permission.VIEW_CHANNEL);
            boolean channelDenied=candidate.getPermissionOverride(guild.getPublicRole())!=null
                    && candidate.getPermissionOverride(guild.getPublicRole())
                        .getDenied().contains(Permission.VIEW_CHANNEL);
            boolean channelAllowed=candidate.getPermissionOverride(guild.getPublicRole())!=null
                    && candidate.getPermissionOverride(guild.getPublicRole())
                        .getAllowed().contains(Permission.VIEW_CHANNEL);
            if (!channelDenied && (!categoryDenied || channelAllowed)) continue;
            matches.add(candidate);
        }
        if (matches.size()!=1 || !matches.get(0).canTalk()) {
            if (missingWarned.compareAndSet(false,true))
                plugin.getLogger().warning("[DISCORD LOG] Unable to post: expected "
                    +"exactly one private, writable #"+channelName
                    +" under category "+categoryName+". Check bot View Channel/"
                    +"Send Messages and the channel/category's @everyone visibility. "
                    +"Queue processing and console logs will continue.");
            return null;
        }
        missingWarned.set(false);
        return matches.get(0);
    }

    /** Never forward mentions or multi-line untrusted resource names to Discord. */
    public static String format(String level,String message) {
        String safe=message.replace('\r',' ').replace('\n',' ')
                .replace("@","@\u200B");
        if (safe.length()>1700) safe=safe.substring(0,1700)+"...";
        return "[TDC "+level+"] "+safe;
    }

    private void send(TextChannel channel,String level,String message) {
        channel.sendMessage(format(level,message)).queue(
                ignored -> sendWarned.set(false),
                error -> {
                    if (sendWarned.compareAndSet(false,true))
                        plugin.getLogger().warning("[DISCORD LOG] Message delivery failed "
                                +"to #"+channel.getName()+": "+error.getMessage());
                });
    }
}
