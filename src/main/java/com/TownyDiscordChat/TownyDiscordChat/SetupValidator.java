package com.TownyDiscordChat.TownyDiscordChat;

import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Category;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import com.TownyDiscordChat.TownyDiscordChat.Storage.Database;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Fail-closed, actionable configuration preflight. No SQL tables or queued
 * Discord operations are started until local values validate. Once connected,
 * the guild check verifies existing category candidates before admitting
 * Discord jobs. A legacy category ID of 0 means AUTO, not an error.
 */
public final class SetupValidator {
    private static final Pattern SNOWFLAKE = Pattern.compile("[0-9]{15,24}");

    public static final class Result {
        private final List<String> errors = new ArrayList<String>();
        private final List<String> warnings = new ArrayList<String>();
        public List<String> errors() { return Collections.unmodifiableList(errors); }
        public List<String> warnings() { return Collections.unmodifiableList(warnings); }
        public boolean valid() { return errors.isEmpty(); }
        private void error(String text) { errors.add(text); }
        private void warn(String text) { warnings.add(text); }
    }

    private SetupValidator() { }

    /** Bukkit's normal config loader can log YAML errors and fall back to defaults. */
    public static Result validateFile(File file) {
        Result result = new Result();
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            result.error("config.yml cannot be read as valid YAML: " + e.getMessage());
        }
        return result;
    }

    public static Result validateLocal(FileConfiguration config) {
        Result out = new Result();
        String storageType=config.getString("storage.type","auto").trim().toLowerCase(Locale.ROOT);
        if (!storageType.equals("auto") && !storageType.equals("sqlite")
                && !storageType.equals("mysql"))
            out.error("storage.type must be auto, sqlite or mysql.");
        String sqliteFile=config.getString("storage.sqlite.file","townydiscordchat.db");
        if (!sqliteFile.matches("[A-Za-z0-9_-]+\\.db"))
            out.error("storage.sqlite.file must be a simple .db filename (no paths or '..').");

        boolean legacyConfigured=Database.meaningful(config.getString("SQL.database"))
                || Database.meaningful(config.getString("SQL.username"))
                || Database.meaningful(config.getString("SQL.password"));
        boolean migration=config.getBoolean("storage.migrateToMySQL",false);
        if (!config.isBoolean("storage.migrateToMySQL"))
            out.error("storage.migrateToMySQL must be true or false.");
        boolean mysqlRequired=storageType.equals("mysql")
                || storageType.equals("auto") && legacyConfigured || migration;
        if (mysqlRequired) {
            for (String key:new String[]{"SQL.host","SQL.database","SQL.username"}) {
                if (!Database.meaningful(config.getString(key)))
                    out.error(key+" must be configured for MySQL (or choose storage.type: sqlite).");
            }
            if (!Database.meaningful(config.getString("SQL.password")))
                out.error("SQL.password is required for MySQL or SQLite-to-MySQL migration.");
            try {
                int port=Integer.parseInt(config.getString("SQL.port","3306"));
                if (port<1 || port>65535) out.error("SQL.port must be 1–65535.");
            } catch (NumberFormatException e) { out.error("SQL.port must be a valid integer."); }
        }
        if (migration && storageType.equals("mysql"))
            out.error("For migration choose storage.type: sqlite and storage.migrateToMySQL: true; switch to mysql only after migration completes.");
        if (!mysqlRequired)
            out.warn("SQLite selected: no MySQL server or credentials are required.");
        String townName = groupName(config,"category.Town.Name",out);
        String nationName = groupName(config,"category.Nation.Name",out);
        groupName(config,"category.Admin.Name",out);
        groupName(config,"log.Category.Name",out);
        groupName(config,"log.Channel.Name",out);
        if (townName != null && townName.equalsIgnoreCase(nationName))
            out.error("category.Town.Name and category.Nation.Name must be different to prevent cross-town access.");

        for (String prefix : new String[]{"town","nation"}) {
            for (String key : new String[]{"CreateTextChannelForRole",
                    "CreateVoiceChannelForRole","CreateRoleIfNoneExists",
                    "UseCategoryForText","UseCategoryForVoice"}) {
                String path=prefix+"."+key;
                if (!config.isBoolean(path)) out.error(path + " must be true or false.");
            }
            // Current SQL worker manages secure private categories and creates
            // a missing role; these legacy toggles are not implemented by it.
            for (String required : new String[]{"CreateRoleIfNoneExists",
                    "UseCategoryForText","UseCategoryForVoice"}) {
                if (!config.getBoolean(prefix+"."+required,true))
                    out.error(prefix+"."+required+"=false is unsupported by the SQL synchronization engine; set true.");
            }
            String color = config.getString(prefix+".RoleCreateColorCode");
            try {
                if (color == null || !color.matches("(?:0[xX]|#)[0-9a-fA-F]{6}"))
                    throw new NumberFormatException("expected 0xRRGGBB or #RRGGBB");
                java.awt.Color.decode(color);
            } catch (NumberFormatException e) {
                out.error(prefix+".RoleCreateColorCode must be a six-digit hex color (example: 0x006798).");
            }
            for (String key : new String[]{"TextCategoryId","VoiceCategoryId"}) {
                String path=prefix+"."+key;
                String id=config.getString(path,"0");
                if (!"0".equals(id) && !"auto".equalsIgnoreCase(id)
                        && !SNOWFLAKE.matcher(id).matches())
                    out.error(path+" must be 0/auto or a valid Discord numeric category ID.");
                else if (!"0".equals(id) && !"auto".equalsIgnoreCase(id))
                    out.warn(path+" is a legacy ID hint; the SQL worker selects secure categories by category."+upper(prefix)+".Name.");
            }
        }
        String logId=config.getString("messages.DiscordLogTextChannelId","0");
        if (!"0".equals(logId) && !"auto".equalsIgnoreCase(logId)
                && !SNOWFLAKE.matcher(logId).matches())
            out.error("messages.DiscordLogTextChannelId must be 0/auto or a Discord numeric channel ID.");
        else if (!"0".equals(logId) && !"auto".equalsIgnoreCase(logId))
            out.warn("messages.DiscordLogTextChannelId is not used for SQL queue logging; use log.Channel.Name.");

        integerRange(config,out,"category.SubChannelsMax",1,50);
        integerRange(config,out,"queue.startDelaySeconds",1,3600);
        integerRange(config,out,"queue.pollSeconds",1,3600);
        integerRange(config,out,"queue.reconcileSeconds",30,86400);
        integerRange(config,out,"queue.leaseSeconds",60,86400);
        integerRange(config,out,"queue.maxAttempts",1,1000);
        integerRange(config,out,"queue.retryBaseSeconds",1,86400);
        integerRange(config,out,"queue.retryMaxSeconds",1,604800);
        integerRange(config,out,"queue.deletionGraceSeconds",30,86400);
        integerRange(config,out,"queue.fullAudit.intervalSeconds",900,2592000);
        integerRange(config,out,"onboarding.cooldownMinutes",1,10080);
        integerRange(config,out,"onboarding.joinDelayTicks",1,1200);
        for (String key : new String[]{"security.revokeUnlinkedAccounts","queue.fullAudit.enabled",
                "onboarding.enabled","onboarding.remindOnJoin","onboarding.remindOnTownJoin"}) {
            if (!config.isBoolean(key)) out.error(key+" must be true or false.");
        }
        String template=config.getString("onboarding.message");
        if (template == null || template.trim().isEmpty() || template.length()>512)
            out.error("onboarding.message must contain 1–512 characters.");
        try { ZoneId.of(config.getString("messages.TimeZone","UTC")); }
        catch (Exception e) { out.error("messages.TimeZone must be a valid Java zone ID, e.g. UTC or America/Toronto."); }
        try { DateTimeFormatter.ofPattern(config.getString("messages.DateFormat","dd/MM/yyyy - hh:mm a")); }
        catch (Exception e) { out.error("messages.DateFormat contains an invalid Java date/time pattern."); }
        if (config.getInt("queue.retryMaxSeconds",900)
                < config.getInt("queue.retryBaseSeconds",30))
            out.error("queue.retryMaxSeconds must be >= queue.retryBaseSeconds.");
        // Legacy guessed Discord API limits are deliberately ignored; JDA
        // handles rate-limit responses and route-specific limits.
        if (config.contains("Discord.Api.RateLimit"))
            out.warn("Discord.Api.RateLimit.* is obsolete and ignored; JDA handles real Discord rate limits.");
        return out;
    }

    private static String upper(String prefix) {
        return prefix.substring(0,1).toUpperCase(Locale.ROOT)+prefix.substring(1);
    }

    private static String groupName(FileConfiguration config,String key,Result out) {
        String value=config.getString(key);
        if (value==null || value.trim().isEmpty() || value.length()>100) {
            out.error(key+" must contain 1–100 characters.");
            return null;
        }
        return value.trim();
    }

    private static void integerRange(FileConfiguration config,Result out,String key,int min,int max) {
        if (!config.isInt(key) && !config.isLong(key)) {
            out.error(key+" must be an integer between "+min+" and "+max+".");
            return;
        }
        long value=config.getLong(key);
        if (value<min || value>max) out.error(key+" must be between "+min+" and "+max+".");
    }

    /**
     * Category group names are canonical. 0/auto means look for a matching
     * private group, adopt verified candidates or create it if missing.
     * Multiple same-name categories are normal after filling 50 slots.
     */
    public static Result validateGuild(FileConfiguration cfg,Guild guild) {
        Result out=new Result();
        if (guild==null) { out.error("DiscordSRV has no connected main guild yet."); return out; }
        if (guild.getSelfMember()==null) {
            out.error("Discord bot is not a member of the selected guild.");
            return out;
        }
        if (!guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES))
            out.error("Discord bot needs Manage Roles to grant/revoke town access.");
        if (!guild.getSelfMember().hasPermission(Permission.MANAGE_CHANNEL))
            out.error("Discord bot needs Manage Channels to create/rename private channels.");
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
            if (channel==null) out.warn("Legacy log channel ID "+logId+" no longer exists; SQL queue logging uses names.");
        }
        return out;
    }
}
