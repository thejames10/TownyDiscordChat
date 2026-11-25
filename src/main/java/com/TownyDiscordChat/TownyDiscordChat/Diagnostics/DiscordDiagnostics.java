package com.TownyDiscordChat.TownyDiscordChat.Diagnostics;

import com.TownyDiscordChat.TownyDiscordChat.GuildSetupValidator;
import com.TownyDiscordChat.TownyDiscordChat.SetupValidator;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * The Discord part of a support report. Kept apart from DebugReport so the
 * report still works when DiscordSRV, and with it JDA, is not installed.
 */
final class DiscordDiagnostics {
    private DiscordDiagnostics() {
    }

    static List<String> lines(FileConfiguration config) {
        List<String> lines = new ArrayList<>();
        JDA jda = DiscordSRV.getPlugin().getJda();
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        lines.add("JDA status: " + (jda == null ? "not started" : jda.getStatus()));
        if (jda != null) {
            lines.add("Gateway intents: " + jda.getGatewayIntents());
        }
        if (guild == null) {
            lines.add("Main guild: not connected");
            return lines;
        }
        lines.add("Main guild: " + guild.getId() + " (" + guild.getMemberCount() + " members)");
        lines.add("Bot permissions: " + guild.getSelfMember().getPermissions());
        lines.add("Linked accounts: " + linkedAccountCount());
        if (config != null) {
            addSetupCheck(lines, GuildSetupValidator.validateGuild(config, guild));
        }
        return lines;
    }

    private static String linkedAccountCount() {
        if (DiscordSRV.getPlugin().getAccountLinkManager() == null) {
            return "account links not loaded";
        }
        return String.valueOf(DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts().size());
    }

    private static void addSetupCheck(List<String> lines, SetupValidator.Result check) {
        lines.add("Discord setup check: " + (check.valid() ? "passed" : "FAILED"));
        for (String error : check.errors()) {
            lines.add("  error: " + error);
        }
        for (String warning : check.warnings()) {
            lines.add("  note: " + warning);
        }
    }
}
