package com.TownyDiscordChat.TownyDiscordChat;

import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Category;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Locale;

/**
 * Discord guild validation is loaded only after dependency compatibility checks.
 * Keeping JDA references out of SetupValidator permits startup without DiscordSRV.
 */
public final class GuildSetupValidator {
    private GuildSetupValidator() {
    }

    private static String upper(String prefix) {
        return prefix.substring(0, 1).toUpperCase(Locale.ROOT) + prefix.substring(1);
    }

    /**
     * Category group names are canonical. 0/auto means look for a matching
     * private group, adopt verified candidates or create it if missing.
     * Multiple same-name categories are normal after filling 50 slots.
     */
    public static SetupValidator.Result validateGuild(FileConfiguration cfg,Guild guild) {
        SetupValidator.Result out=new SetupValidator.Result();
        if (guild==null) { out.error("DiscordSRV has no connected main guild yet."); return out; }
        if (guild.getSelfMember()==null) {
            out.error("Discord bot is not a member of the selected guild.");
            return out;
        }
        if (!guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES))
            out.error("Discord bot needs Manage Roles to grant/revoke town access.");
        if (!guild.getSelfMember().hasPermission(Permission.MANAGE_CHANNEL))
            out.error("Discord bot needs Manage Channels to create/rename private channels.");

        // Manage Roles alone is insufficient: the bot's highest role must
        // outrank each managed Towny role or revocations will fail.
        java.util.Set<String> managedNames=new java.util.HashSet<String>();
        for (com.palmergames.bukkit.towny.object.Town town :
                com.palmergames.bukkit.towny.TownyUniverse.getInstance().getTowns())
            managedNames.add(("town-"+town.getName()).toLowerCase(Locale.ROOT));
        for (com.palmergames.bukkit.towny.object.Nation nation :
                com.palmergames.bukkit.towny.TownyUniverse.getInstance().getNations())
            managedNames.add(("nation-"+nation.getName()).toLowerCase(Locale.ROOT));
        int blockedRoles=0;
        String example=null;
        for (github.scarsz.discordsrv.dependencies.jda.api.entities.Role role : guild.getRoles()) {
            if (managedNames.contains(role.getName().toLowerCase(Locale.ROOT))
                    && !guild.getSelfMember().canInteract(role)) {
                blockedRoles++;
                if (example==null) example=role.getName();
            }
        }
        if (blockedRoles>0)
            out.error("Discord bot's highest role is below "+blockedRoles
                    +" managed Towny roles (e.g. "+example+"). Move the bot role above"
                    +" Towny roles before enabling synchronization.");

        for (String group : new String[]{"town","nation"}) {
            String expected=cfg.getString("category."+upper(group)+".Name");
            List<Category> categories=guild.getCategoriesByName(expected,true);
            int privateMatches=0;
            for (Category category:categories) {
                if (category.getPermissionOverride(guild.getPublicRole())!=null
                        && category.getPermissionOverride(guild.getPublicRole())
                                .getDenied().contains(Permission.VIEW_CHANNEL))
                    privateMatches++;
            }
            if (categories.size()>privateMatches)
                out.error("Discord has "+(categories.size()-privateMatches)
                        +" publicly accessible or incorrectly configured category/categories named "
                        +expected+". Restrict @everyone's View Channel permission or rename them.");
            for (String field:new String[]{"TextCategoryId","VoiceCategoryId"}) {
                String key=group+"."+field;
                String id=cfg.getString(key,"0");
                if ("0".equals(id) || "auto".equalsIgnoreCase(id)) continue;
                Category category=guild.getCategoryById(id);
                if (category==null || !category.getName().equalsIgnoreCase(expected))
                    out.error(key+" points to a missing or wrongly named category. Change to 0 (auto) to discover by name.");
                else if (category.getPermissionOverride(guild.getPublicRole())==null
                        || !category.getPermissionOverride(guild.getPublicRole())
                                .getDenied().contains(Permission.VIEW_CHANNEL))
                    out.error(key+" points to a public category; deny @everyone View Channel before continuing.");
            }
            if (categories.isEmpty())
                out.warn("No existing "+expected+" category: SQL synchronization will create a private one.");
            else out.warn("Auto-discovered "+privateMatches+" private "+expected+" category/categories by name.");
        }
        String logId=cfg.getString("messages.DiscordLogTextChannelId","0");
        if (!"0".equals(logId) && !"auto".equalsIgnoreCase(logId)) {
            TextChannel channel=guild.getTextChannelById(logId);
            if (channel==null) out.warn("Legacy log channel ID "+logId+" no longer exists; Discord activity logging discovers the private channel by log.Category.Name and log.Channel.Name.");
        }
        return out;
    }
}
