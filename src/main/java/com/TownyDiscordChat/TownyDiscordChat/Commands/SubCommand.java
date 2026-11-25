package com.TownyDiscordChat.TownyDiscordChat.Commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

/** One /tdc subcommand: its name, who may run it and what it does. */
public final class SubCommand {
    private final String name;
    private final String permission;
    private final boolean playerOnly;
    private final BiConsumer<CommandSender, String[]> action;

    public SubCommand(String name, String permission, boolean playerOnly,
                      BiConsumer<CommandSender, String[]> action) {
        this.name = name;
        this.permission = permission;
        this.playerOnly = playerOnly;
        this.action = action;
    }

    public String name() {
        return name;
    }

    boolean playerOnly() {
        return playerOnly;
    }

    boolean permitted(CommandSender sender) {
        return sender.hasPermission(permission);
    }

    boolean availableTo(CommandSender sender) {
        return permitted(sender) && (!playerOnly || sender instanceof Player);
    }

    void run(CommandSender sender, String[] arguments) {
        action.accept(sender, arguments);
    }
}
