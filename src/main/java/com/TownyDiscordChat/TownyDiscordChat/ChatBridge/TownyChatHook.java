package com.TownyDiscordChat.TownyDiscordChat.ChatBridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Listens to TownyChat's AsyncChatHookEvent by reflection: TownyChat
 * publishes no Maven artifact, and this keeps it optional at runtime.
 * TownyChat fires the event only for channels marked "hooked: true".
 */
final class TownyChatHook {
    private static final String TOWNY_CHAT = "TownyChat";
    private static final String HOOK_EVENT = "com.palmergames.bukkit.TownyChat.events.AsyncChatHookEvent";
    private static final String CHANNEL = "com.palmergames.bukkit.TownyChat.channels.Channel";

    /** Receives each accepted TownyChat message. Runs on the chat thread. */
    interface ChatHandler {
        void onTownyChat(Player player, String channelType, String message);
    }

    private final Listener owner = new Listener() {
    };
    private final AtomicBoolean warned = new AtomicBoolean(false);
    private final Logger logger;
    private final Method getChannel;
    private final Method getMessage;
    private final Method getPlayer;
    private final Method isCancelled;
    private final Method getType;

    private TownyChatHook(Logger logger, Class<?> eventClass, Class<?> channelClass)
            throws NoSuchMethodException {
        this.logger = logger;
        this.getChannel = eventClass.getMethod("getChannel");
        this.getMessage = eventClass.getMethod("getMessage");
        this.getPlayer = eventClass.getMethod("getPlayer");
        this.isCancelled = eventClass.getMethod("isCancelled");
        this.getType = channelClass.getMethod("getType");
    }

    /** Null when TownyChat is missing or its API changed. */
    static TownyChatHook register(Plugin plugin, ChatHandler handler) {
        Plugin townyChat = Bukkit.getPluginManager().getPlugin(TOWNY_CHAT);
        if (townyChat == null || !townyChat.isEnabled()) {
            return null;
        }
        try {
            ClassLoader loader = townyChat.getClass().getClassLoader();
            Class<? extends Event> eventClass = Class.forName(HOOK_EVENT, true, loader).asSubclass(Event.class);
            TownyChatHook hook = new TownyChatHook(plugin.getLogger(), eventClass,
                    Class.forName(CHANNEL, true, loader));
            Bukkit.getPluginManager().registerEvent(eventClass, hook.owner, EventPriority.MONITOR,
                    (listener, event) -> hook.handle(eventClass, event, handler), plugin);
            return hook;
        } catch (ReflectiveOperationException | RuntimeException error) {
            plugin.getLogger().warning("Could not hook into TownyChat for the chat bridge: " + error);
            return null;
        }
    }

    void unregister() {
        HandlerList.unregisterAll(owner);
    }

    private void handle(Class<? extends Event> eventClass, Event event, ChatHandler handler) {
        if (!eventClass.isInstance(event)) {
            return;
        }
        try {
            if (Boolean.TRUE.equals(isCancelled.invoke(event))) {
                return;
            }
            Object channel = getChannel.invoke(event);
            Object type = channel == null ? null : getType.invoke(channel);
            if (type == null) {
                return;
            }
            handler.onTownyChat((Player) getPlayer.invoke(event), type.toString(),
                    String.valueOf(getMessage.invoke(event)));
        } catch (ReflectiveOperationException | RuntimeException error) {
            if (warned.compareAndSet(false, true)) {
                logger.warning("TownyChat bridge could not read a chat message: " + error);
            }
        }
    }
}
