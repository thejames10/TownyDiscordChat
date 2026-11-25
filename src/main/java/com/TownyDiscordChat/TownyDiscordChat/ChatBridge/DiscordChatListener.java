package com.TownyDiscordChat.TownyDiscordChat.ChatBridge;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;

/** DiscordSRV only calls public methods annotated with its own Subscribe. */
public final class DiscordChatListener {
    private final ChatBridge bridge;

    DiscordChatListener(ChatBridge bridge) {
        this.bridge = bridge;
    }

    @Subscribe
    public void onGuildMessage(DiscordGuildMessageReceivedEvent event) {
        bridge.relayFromDiscord(event);
    }
}
