package com.TownyDiscordChat.TownyDiscordChat.Core;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Nation;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Category;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.VoiceChannel;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.ChannelAction;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.RoleAction;
import java.awt.Color;
import java.util.function.Consumer;

/**
 * REST creation callbacks used exclusively by the durable Discord queue.
 * The old direct permission-mutating helpers were removed: all membership
 * decisions and ownership checks belong to Queue and Reconciler.
 */
public final class TDCManager {
    private static final long MEMBERSHIP_ROLE_PERMISSIONS = 0L;

    private TDCManager() { }

    private static Guild guild() {
        Guild result = DiscordSRV.getPlugin().getMainGuild();
        if (result == null) throw new IllegalStateException("DiscordSRV main guild unavailable");
        return result;
    }

    private static void fail(String action, Throwable cause, Consumer<String> callback) {
        Main.plugin.getLogger().warning("Discord API "+action+" failed: "
                + cause.getClass().getSimpleName()+": "+cause.getMessage());
        callback.accept(null);
    }

    static RoleAction newMembershipRole(Guild guild, String roleName, Color color) {
        // Discord defaults omitted role permissions to the guild's @everyone permissions.
        // Access must come only from explicit private channel overrides.
        return guild.createRole()
                .setName(roleName)
                .setColor(color)
                .setPermissions(MEMBERSHIP_ROLE_PERMISSIONS);
    }

    public static void createDiscordRole(Town town, Consumer<String> callback) {
        try {
            newMembershipRole(guild(), "town-" + town.getName(),
                    Color.decode(Main.plugin.config.getString("town.RoleCreateColorCode")))
                    .queue(role -> callback.accept(role.getId()),
                            error -> fail("create town role", error, callback));
        } catch (Exception error) { fail("create town role", error, callback); }
    }

    public static void createDiscordRole(Nation nation, Consumer<String> callback) {
        try {
            newMembershipRole(guild(), "nation-" + nation.getName(),
                    Color.decode(Main.plugin.config.getString("nation.RoleCreateColorCode")))
                    .queue(role -> callback.accept(role.getId()),
                            error -> fail("create nation role", error, callback));
        } catch (Exception error) { fail("create nation role", error, callback); }
    }

    public static void createChatCategory(String name, Consumer<String> callback) {
        try {
            Guild g=guild();
            long everyone=g.getPublicRole().getIdLong();
            long bot=g.getSelfMember().getIdLong();
            long view=Permission.VIEW_CHANNEL.getRawValue();
            g.createCategory(name)
                    .addRolePermissionOverride(everyone,0L,view)
                    .addMemberPermissionOverride(bot,view,0L)
                    .queue(category -> callback.accept(category.getId()),
                            error -> fail("create private category",error,callback));
        } catch (Exception error) { fail("create private category",error,callback); }
    }

    private static void createText(String name,String roleId,String categoryId,
                                   Consumer<String> callback) {
        try {
            Guild g=guild();
            Category parent=g.getCategoryById(categoryId);
            if (parent==null) throw new IllegalStateException("Category "+categoryId+" not found");
            long view=Permission.VIEW_CHANNEL.getRawValue();
            ChannelAction<TextChannel> action=g.createTextChannel(name)
                    .addRolePermissionOverride(g.getPublicRole().getIdLong(),0L,view)
                    .addRolePermissionOverride(Long.parseLong(roleId),view,0L)
                    .addMemberPermissionOverride(g.getSelfMember().getIdLong(),view,0L)
                    .setParent(parent);
            action.queue(channel -> callback.accept(channel.getId()),
                    error -> fail("create private text channel",error,callback));
        } catch (Exception error) { fail("create private text channel",error,callback); }
    }

    private static void createVoice(String name,String roleId,String categoryId,
                                    Consumer<String> callback) {
        try {
            Guild g=guild();
            Category parent=g.getCategoryById(categoryId);
            if (parent==null) throw new IllegalStateException("Category "+categoryId+" not found");
            long view=Permission.VIEW_CHANNEL.getRawValue();
            ChannelAction<VoiceChannel> action=g.createVoiceChannel(name)
                    .addRolePermissionOverride(g.getPublicRole().getIdLong(),0L,view)
                    .addRolePermissionOverride(Long.parseLong(roleId),view,0L)
                    .addMemberPermissionOverride(g.getSelfMember().getIdLong(),view,0L)
                    .setParent(parent);
            action.queue(channel -> callback.accept(channel.getId()),
                    error -> fail("create private voice channel",error,callback));
        } catch (Exception error) { fail("create private voice channel",error,callback); }
    }

    public static void createTownTextChannel(String name,String roleId,String parentId,
                                             Consumer<String> callback) {
        createText(name,roleId,parentId,callback);
    }

    public static void createNationTextChannel(String name,String roleId,String parentId,
                                               Consumer<String> callback) {
        createText(name,roleId,parentId,callback);
    }

    public static void createTownVoiceChannel(String name,String roleId,String parentId,
                                              Consumer<String> callback) {
        createVoice(name,roleId,parentId,callback);
    }

    public static void createNationVoiceChannel(String name,String roleId,String parentId,
                                                Consumer<String> callback) {
        createVoice(name,roleId,parentId,callback);
    }
}
