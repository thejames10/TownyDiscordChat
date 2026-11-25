package com.TownyDiscordChat.TownyDiscordChat.Commands;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.TownyDiscordChat.TownyDiscordChat.Messages.MessageCatalog;
import com.TownyDiscordChat.TownyDiscordChat.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** /tdc: player status and linking, plus admin reload, debug and checks. */
public final class TownyDiscordChatCommand implements TabExecutor {
    private static final String HELP = "help";
    // 1.1.x players typed /tdc linked; it still shows their status.
    private static final Map<String, String> ALIASES = Collections.singletonMap("linked", "status");

    private final Main plugin;
    private final Map<String, SubCommand> subcommands = new LinkedHashMap<>();

    public TownyDiscordChatCommand(Main plugin) {
        this.plugin = plugin;
        register(new SubCommand("status", Permissions.PLAYER, true,
                (sender, arguments) -> plugin.onboarding.showStatus((Player) sender)));
        register(new SubCommand("link", Permissions.PLAYER, true,
                (sender, arguments) -> plugin.onboarding.link((Player) sender)));
        register(new SubCommand(HELP, Permissions.PLAYER, false,
                (sender, arguments) -> showHelp(sender)));
        register(new SubCommand("reload", Permissions.RELOAD, false,
                (sender, arguments) -> plugin.reloadConfiguration(sender)));
        register(new SubCommand("debug", Permissions.DEBUG, false,
                (sender, arguments) -> plugin.writeDebugReport(sender)));
        // 1.1.x used /tdc Check Role|TextChannel|VoiceChannel ...; extra words are ignored.
        register(new SubCommand("check", Permissions.RECONCILE, false,
                (sender, arguments) -> requestCheck(sender)));
    }

    public void register(SubCommand subcommand) {
        subcommands.put(subcommand.name(), subcommand);
    }

    private MessageCatalog messages() {
        return plugin.messages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        if (arguments.length == 0) {
            showInfo(sender);
            return true;
        }
        SubCommand subcommand = find(arguments[0]);
        if (subcommand == null) {
            messages().send(sender, "unknown-subcommand");
        } else if (subcommand.playerOnly() && !(sender instanceof Player)) {
            messages().send(sender, "players-only");
        } else if (!subcommand.permitted(sender)) {
            messages().send(sender, "no-permission");
        } else {
            subcommand.run(sender, Arrays.copyOfRange(arguments, 1, arguments.length));
        }
        return true;
    }

    private SubCommand find(String typed) {
        String name = typed.toLowerCase(Locale.ROOT);
        return subcommands.get(ALIASES.getOrDefault(name, name));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias,
                                      String[] arguments) {
        if (arguments.length != 1) {
            return Collections.emptyList();
        }
        String typed = arguments[0].toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (SubCommand subcommand : subcommands.values()) {
            if (subcommand.availableTo(sender) && subcommand.name().startsWith(typed)) {
                matches.add(subcommand.name());
            }
        }
        return matches;
    }

    private void showInfo(CommandSender sender) {
        messages().send(sender, "info", "version", plugin.getDescription().getVersion());
        if (sender instanceof Player && sender.hasPermission(Permissions.PLAYER)) {
            plugin.onboarding.showStatus((Player) sender);
        }
    }

    private void showHelp(CommandSender sender) {
        messages().send(sender, "help.header");
        for (SubCommand subcommand : subcommands.values()) {
            if (subcommand.availableTo(sender)) {
                messages().send(sender, "help.line", "command", subcommand.name(),
                        "description", messages().text("help.descriptions." + subcommand.name()));
            }
        }
    }

    private void requestCheck(CommandSender sender) {
        if (plugin.reconciler == null) {
            messages().send(sender, "check.not-running");
            return;
        }
        plugin.reconciler.requestSoon();
        messages().send(sender, "check.requested");
    }
}
