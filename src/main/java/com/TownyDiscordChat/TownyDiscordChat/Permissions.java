package com.TownyDiscordChat.TownyDiscordChat;

/**
 * Permission nodes. Bukkit compares nodes case-insensitively, so the 1.1.x
 * names (TownyDiscordChat.Admin, TownyDiscordChat.Player) are these same nodes.
 */
public final class Permissions {
    public static final String ADMIN = "townydiscordchat.admin";
    public static final String PLAYER = "townydiscordchat.player";
    public static final String RELOAD = "townydiscordchat.reload";
    public static final String DEBUG = "townydiscordchat.debug";
    public static final String QUEUE = "townydiscordchat.queue";
    public static final String RECONCILE = "townydiscordchat.reconcile";
    public static final String NOTIFY = "townydiscordchat.notify";

    private Permissions() {
    }
}
