package com.TownyDiscordChat.TownyDiscordChat.ChatBridge;

import com.TownyDiscordChat.TownyDiscordChat.ChatBridge.ChannelDirectory.Group;
import com.TownyDiscordChat.TownyDiscordChat.ChatBridge.ChannelDirectory.GroupType;
import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.util.WebhookUtil;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Relays TownyChat town and nation chat to the matching private Discord
 * channel and back. Only linked players who belong to the town or nation
 * right now are relayed from Discord, so a stale Discord role cannot speak.
 */
public final class ChatBridge {
    private static final String FROM_DISCORD_MESSAGE = "chat-bridge.from-discord";
    private static final String ATTACHMENT_NOTE = "[attachment]";

    private final Main plugin;
    private final DiscordChatListener discordListener;
    private TownyChatHook townyChatHook;

    private ChatBridge(Main plugin) {
        this.plugin = plugin;
        this.discordListener = new DiscordChatListener(this);
    }

    /** Listens in both directions; each message checks whether the bridge is enabled. */
    public static ChatBridge start(Main plugin) {
        ChatBridge bridge = new ChatBridge(plugin);
        DiscordSRV.api.subscribe(bridge.discordListener);
        bridge.townyChatHook = TownyChatHook.register(plugin, bridge::relayFromMinecraft);
        if (bridge.townyChatHook == null && bridge.settings().enabled()) {
            plugin.getLogger().warning("chatBridge is enabled but TownyChat is not available,"
                    + " so only Discord messages reach the game.");
        }
        return bridge;
    }

    public void stop() {
        if (townyChatHook != null) {
            townyChatHook.unregister();
        }
        try {
            DiscordSRV.api.unsubscribe(discordListener);
        } catch (Throwable error) {
            plugin.getLogger().warning("Could not unsubscribe the chat bridge from DiscordSRV: " + error);
        }
    }

    /** Runs on TownyChat's chat thread. */
    void relayFromMinecraft(Player player, String channelType, String message) {
        ChatBridgeSettings settings = settings();
        GroupType type = settings.bridgedType(channelType);
        UUID group = type == null ? null : groupOf(player.getUniqueId(), type);
        TextChannel channel = group == null ? null : bridgedChannel(type, group);
        String text = ChatText.forDiscord(message, settings.maxMessageLength());
        if (channel == null || text.isEmpty()) {
            return;
        }
        if (settings.useWebhooks()) {
            WebhookUtil.deliverMessage(channel, player, text);
            return;
        }
        String line = ChatText.format(settings.toDiscordFormat(),
                ChatText.escapeMarkdown(player.getName()), text);
        channel.sendMessage(line).allowedMentions(Collections.<Message.MentionType>emptySet())
                .queue(null, error -> plugin.getLogger().warning(
                        "Could not relay chat to Discord channel " + channel.getName() + ": " + error.getMessage()));
    }

    /** Runs on DiscordSRV's JDA event thread. */
    void relayFromDiscord(DiscordGuildMessageReceivedEvent event) {
        ChatBridgeSettings settings = settings();
        if (!settings.enabled() || event.getAuthor().isBot() || event.getMessage().isWebhookMessage()) {
            return;
        }
        Group group = plugin.channelDirectory().groupFor(event.getChannel().getId());
        String text = ChatText.forMinecraft(contentOf(event.getMessage()), settings.maxMessageLength());
        if (group == null || !settings.bridges(group.type()) || text.isEmpty()) {
            return;
        }
        AccountLinkManager links = DiscordSRV.getPlugin().getAccountLinkManager();
        UUID author = links == null ? null : links.getUuid(event.getAuthor().getId());
        if (author != null) {
            plugin.scheduler().runGlobal(() -> deliverInGame(group, author, text));
        }
    }

    private void deliverInGame(Group group, UUID author, String text) {
        Resident resident = TownyUniverse.getInstance().getResident(author);
        Audience audience = resident == null ? Audience.NONE : audienceFor(group, resident);
        if (audience.residents.isEmpty()) {
            return;
        }
        String line = plugin.messages().text(FROM_DISCORD_MESSAGE,
                "channel", audience.name, "name", resident.getName(), "message", text);
        for (Resident member : audience.residents) {
            Player player = member.getPlayer();
            if (player != null && player.isOnline()) {
                player.sendMessage(line);
            }
        }
    }

    private static Audience audienceFor(Group group, Resident author) {
        TownyUniverse towny = TownyUniverse.getInstance();
        if (group.type() == GroupType.TOWN) {
            Town town = towny.getTown(group.uuid());
            return town != null && town.hasResident(author)
                    ? new Audience(town.getName(), town.getResidents()) : Audience.NONE;
        }
        Nation nation = towny.getNation(group.uuid());
        return nation != null && nation.hasResident(author)
                ? new Audience(nation.getName(), nation.getResidents()) : Audience.NONE;
    }

    private static UUID groupOf(UUID player, GroupType type) {
        Resident resident = TownyUniverse.getInstance().getResident(player);
        Town town = resident == null ? null : resident.getTownOrNull();
        if (town == null) {
            return null;
        }
        if (type == GroupType.TOWN) {
            return town.getUUID();
        }
        Nation nation = town.getNationOrNull();
        return nation == null ? null : nation.getUUID();
    }

    private TextChannel bridgedChannel(GroupType type, UUID group) {
        String channelId = plugin.channelDirectory().textChannelFor(type, group);
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        return channelId == null || guild == null ? null : guild.getTextChannelById(channelId);
    }

    private static String contentOf(Message message) {
        String content = message.getContentDisplay();
        if (message.getAttachments().isEmpty()) {
            return content;
        }
        return content.trim().isEmpty() ? ATTACHMENT_NOTE : content + " " + ATTACHMENT_NOTE;
    }

    private ChatBridgeSettings settings() {
        return ChatBridgeSettings.fromConfig(plugin.getConfig());
    }

    /** Who sees a Discord message in game, and the town or nation name to show. */
    private static final class Audience {
        static final Audience NONE = new Audience("", Collections.<Resident>emptyList());

        final String name;
        final List<Resident> residents;

        Audience(String name, List<Resident> residents) {
            this.name = name;
            this.residents = residents;
        }
    }
}
