package com.TownyDiscordChat.TownyDiscordChat;

import com.TownyDiscordChat.TownyDiscordChat.Commands.TDCCommand;
import com.TownyDiscordChat.TownyDiscordChat.Commands.TDCSQL;
import com.TownyDiscordChat.TownyDiscordChat.Listeners.MembershipChangeListener;
import com.TownyDiscordChat.TownyDiscordChat.Listeners.OnboardingListener;
import com.TownyDiscordChat.TownyDiscordChat.Listeners.AccountChangeListener;
import com.TownyDiscordChat.TownyDiscordChat.Queue.MemberIntentStore;
import com.TownyDiscordChat.TownyDiscordChat.Queue.StableEntityStore;
import com.TownyDiscordChat.TownyDiscordChat.Storage.Database;
import com.TownyDiscordChat.TownyDiscordChat.Storage.StorageMigration;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.*;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Log.Log;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Players.Players;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Queue.Queue;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Reconciler;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.SQLException;
import java.util.*;

public class Main extends JavaPlugin {

    public Database SQL;
    public LogCategory logCategoryDB;
    public LogTextChannel logTextChannelDB;
    public com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.Players playersDB;
    public TownRoles townRolesDB;
    public NationRoles nationRolesDB;
    public TownCategories townCategoriesDB;
    public NationCategories nationCategoriesDB;
    public TownTextChannels townTextChannelsDB;
    public TownVoiceChannels townVoiceChannelsDB;
    public NationTextChannels nationTextChannelsDB;
    public NationVoiceChannels nationVoiceChannelsDB;
    public QueuedTask queuedTaskDB;
    public Log logScheduler;
    public Players playersScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Categories.Towns townCategoriesScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Categories.Nations nationCategoriesScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Roles.Nations nationRolesScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Roles.Towns townRolesScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Channels.Text.Towns townTextChannelsScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Channels.Text.Nations nationTextChannelsScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Channels.Voice.Towns townVoiceChannelsScheduler;
    public com.TownyDiscordChat.TownyDiscordChat.Schedulers.Channels.Voice.Nations nationVoiceChannelsScheduler;
    public Queue queueScheduler;
    public Reconciler reconciler;
    public CompatibilityGuard compatibility;
    public AccountChangeListener accountListener;
    public OnboardingListener onboarding;
    private boolean operational;
    private volatile boolean guildValidated;
    private volatile String startupProblem;

    public boolean isGuildValidated() { return guildValidated; }
    public void setGuildValidated(boolean ready) { guildValidated=ready; }
    public String getStartupProblem() { return startupProblem; }
    private volatile String adminWarning;

    /** Persistent operator-visible status for recoverable audit failures. */
    public String getAdminWarning() { return adminWarning; }

    public void reportAdminWarning(String warning) {
        if (warning == null || warning.trim().isEmpty()) return;
        if (warning.equals(adminWarning)) return;
        adminWarning = warning;
        getLogger().warning("[ADMIN ACTION REQUIRED] " + warning);
        if (isEnabled()) {
            Bukkit.getScheduler().runTask(this, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isOp() || player.hasPermission("TownyDiscordChat.Admin"))
                        player.sendMessage(ChatColor.RED + "[TownyDiscordChat] " + warning);
                }
            });
        }
    }

    public void clearAdminWarning() { adminWarning = null; }

    public boolean isOperational() { return operational; }
    public String getSafeModeReason() {
        if (startupProblem!=null) return startupProblem;
        return compatibility == null ? "Not initialized" : compatibility.problem();
    }

    public static Main plugin;

    public FileConfiguration config = getConfig();

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        reloadConfig();
        config = getConfig();

        SetupValidator.Result fileCheck=SetupValidator.validateFile(
                new java.io.File(getDataFolder(),"config.yml"));
        SetupValidator.Result configCheck=SetupValidator.validateLocal(config);
        for (String warning:configCheck.warnings()) getLogger().warning("[CONFIG] "+warning);
        if (!fileCheck.valid() || !configCheck.valid()) {
            for (String error:fileCheck.errors()) getLogger().severe("[CONFIG] "+error);
            for (String error:configCheck.errors()) getLogger().severe("[CONFIG] "+error);
            String firstError=!fileCheck.errors().isEmpty() ? fileCheck.errors().get(0)
                    : configCheck.errors().get(0);
            startupProblem="Invalid config.yml: "+firstError
                    +" Check console for all errors, correct the file, then restart.";
        }

        // Soft dependencies permit a visible, inert safe mode when outdated or
        // missing plugins would otherwise prevent TownyDiscordChat loading.
        this.compatibility = new CompatibilityGuard(this);
        getServer().getPluginManager().registerEvents(compatibility, this);
        boolean dependenciesReady=compatibility.check();
        if (!dependenciesReady || startupProblem!=null) {
            if (!dependenciesReady) compatibility.warnConsole();
            if (startupProblem!=null) getLogger().severe("SAFE MODE: "+startupProblem);
            java.util.Objects.requireNonNull(getCommand("TownyDiscordChat"))
                    .setExecutor((sender,command,label,args) -> {
                        sender.sendMessage("[TownyDiscordChat] SAFE MODE: " + getSafeModeReason());
                        return true;
                    });
            java.util.Objects.requireNonNull(getCommand("tdcqueue"))
                    .setExecutor((sender,command,label,args) -> {
                        sender.sendMessage("[TownyDiscordChat] SAFE MODE: " + getSafeModeReason());
                        return true;
                    });
            return;
        }

        new Metrics(this, 10980);
        java.util.Objects.requireNonNull(getCommand("TownyDiscordChat")).setExecutor(new TDCCommand());
        java.util.Objects.requireNonNull(getCommand("tdcqueue")).setExecutor(new TDCSQL());

        this.SQL = new Database(config,getDataFolder());
        this.logCategoryDB = new LogCategory(this);
        this.logTextChannelDB = new LogTextChannel(this);
        this.playersDB = new com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.Players(this);
        this.townRolesDB = new TownRoles(this);
        this.nationRolesDB = new NationRoles(this);
        this.townCategoriesDB = new TownCategories(this);
        this.nationCategoriesDB = new NationCategories(this);
        this.townTextChannelsDB = new TownTextChannels(this);
        this.townVoiceChannelsDB = new TownVoiceChannels(this);
        this.nationTextChannelsDB = new NationTextChannels(this);
        this.nationVoiceChannelsDB = new NationVoiceChannels(this);
        this.queuedTaskDB = new QueuedTask(this);

        try {
            SQL.connect();
        } catch (ClassNotFoundException | SQLException e) {
            getLogger().severe("Database not connected: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            if (SQL != null && SQL.isConnected()) {
                getLogger().info("Storage connected: "+SQL.getBackend());
                logCategoryDB.createTable();
                logTextChannelDB.createTable();
                playersDB.createTable();
                townRolesDB.createTable();
                nationRolesDB.createTable();
                townCategoriesDB.createTable();
                nationCategoriesDB.createTable();
                townTextChannelsDB.createTable();
                townVoiceChannelsDB.createTable();
                nationTextChannelsDB.createTable();
                nationVoiceChannelsDB.createTable();
                queuedTaskDB.createTable();
                new MemberIntentStore(this).initialize();
                new StableEntityStore(this).initialize();
                SQL.ensureIndexes();
                SQL.verifyRequiredTables();

                // Explicit offline migration mode. No Discord or Towny
                // schedulers are started while copying database state.
                if (config.getBoolean("storage.migrateToMySQL",false)) {
                    if (SQL.getBackend()!=Database.Backend.SQLITE) {
                        startupProblem="Migration requires storage.type: sqlite.";
                        getLogger().severe(startupProblem);
                    } else {
                        startupProblem="SQLite to MySQL migration running; "
                                +"synchronization paused until a server restart.";
                        getLogger().warning(startupProblem);
                        getServer().getScheduler().runTaskAsynchronously(this, () -> {
                            try {
                                StorageMigration.Result result=StorageMigration.sqliteToMySQL(
                                        SQL,SQL.getMysqlUrl(),config.getString("SQL.username",""),
                                        config.getString("SQL.password",""),getLogger());
                                startupProblem="Migration completed: "+result.rows+" rows across "
                                        +result.tables+" tables copied to MySQL. "
                                        +"Set storage.type: mysql and storage.migrateToMySQL: false, "
                                        +"then restart the server. SQLite remains unchanged.";
                                getLogger().warning(startupProblem);
                            } catch (Exception e) {
                                startupProblem="Migration FAILED; SQLite remains authoritative. "
                                        +"Fix MySQL connectivity/schema and restart. Cause: "+e.getMessage();
                                getLogger().severe(startupProblem);
                                getLogger().log(java.util.logging.Level.SEVERE,
                                        "Storage migration failed",e);
                            }
                        });
                    }
                    java.util.Objects.requireNonNull(getCommand("TownyDiscordChat"))
                            .setExecutor((sender,command,label,args) -> {
                                sender.sendMessage("[TDC] "+getSafeModeReason());
                                return true;
                            });
                    java.util.Objects.requireNonNull(getCommand("tdcqueue"))
                            .setExecutor((sender,command,label,args) -> {
                                sender.sendMessage("[TDC] "+getSafeModeReason());
                                return true;
                            });
                    return;
                }

                // Do not wait synchronously for DiscordSRV to connect.
                // Reconciler will start once the guild is available.
                this.reconciler = new Reconciler(this);
                this.queueScheduler = new Queue(
                        config.getLong("queue.startDelaySeconds", 10L),
                        config.getLong("queue.pollSeconds", 1L), this);
                getServer().getPluginManager().registerEvents(new MembershipChangeListener(this), this);
                this.onboarding = new OnboardingListener(this);
                getServer().getPluginManager().registerEvents(onboarding,this);
                this.accountListener = new AccountChangeListener(this);
                DiscordSRV.api.subscribe(accountListener);
                this.operational = true;
            }
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize queue: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // No legacy listeners are active: every membership mutation goes via
        // the durable desired-state queue, never directly to JDA.
        getLogger().info("TownyDiscordChat has been Enabled!");
    }

    @Override
    public void onDisable() {
        operational = false;
        guildValidated = false;
        if (accountListener != null) {
            try { DiscordSRV.api.unsubscribe(accountListener); }
            catch (Throwable e) { getLogger().warning("Could not unsubscribe DiscordSRV listener: " + e); }
        }
        if (queueScheduler != null) queueScheduler.cancel();
        if (reconciler != null) reconciler.cancel();

        try {
            if (SQL != null && SQL.isConnected()) {
                SQL.disconnect();
                getLogger().info("Database is disconnected!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getLogger().info("TownyDiscordChat has been Disabled!");
    }
}