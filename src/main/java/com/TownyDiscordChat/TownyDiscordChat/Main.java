package com.TownyDiscordChat.TownyDiscordChat;

import com.TownyDiscordChat.TownyDiscordChat.Commands.QueueCommand;
import com.TownyDiscordChat.TownyDiscordChat.Commands.TownyDiscordChatCommand;
import com.TownyDiscordChat.TownyDiscordChat.Diagnostics.DebugReport;
import com.TownyDiscordChat.TownyDiscordChat.Listeners.AccountChangeListener;
import com.TownyDiscordChat.TownyDiscordChat.Listeners.MembershipChangeListener;
import com.TownyDiscordChat.TownyDiscordChat.Listeners.OnboardingListener;
import com.TownyDiscordChat.TownyDiscordChat.Messages.MessageCatalog;
import com.TownyDiscordChat.TownyDiscordChat.Naming.RoleNames;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.LogCategory;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.LogTextChannel;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.NationCategories;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.NationRoles;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.NationTextChannels;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.NationVoiceChannels;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.Players;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.TownCategories;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.TownRoles;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.TownTextChannels;
import com.TownyDiscordChat.TownyDiscordChat.MySQL.Tables.TownVoiceChannels;
import com.TownyDiscordChat.TownyDiscordChat.Queue.DiscordJobStore;
import com.TownyDiscordChat.TownyDiscordChat.Queue.MemberIntentStore;
import com.TownyDiscordChat.TownyDiscordChat.Queue.RankGrantStore;
import com.TownyDiscordChat.TownyDiscordChat.Queue.StableEntityStore;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Queue.Queue;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Reconciler;
import com.TownyDiscordChat.TownyDiscordChat.Scheduling.TaskScheduler;
import com.TownyDiscordChat.TownyDiscordChat.Scheduling.TaskSchedulers;
import com.TownyDiscordChat.TownyDiscordChat.Statistics.PluginMetrics;
import com.TownyDiscordChat.TownyDiscordChat.Statistics.PluginStatus;
import com.TownyDiscordChat.TownyDiscordChat.Storage.Database;
import com.TownyDiscordChat.TownyDiscordChat.Storage.StorageMigration;
import com.TownyDiscordChat.TownyDiscordChat.Updates.UpdateChecker;
import github.scarsz.discordsrv.DiscordSRV;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class Main extends JavaPlugin {

    private static final int BSTATS_PLUGIN_ID = 10980;
    private static final String SQLITE_LABEL = "SQLite";
    private static final String MYSQL_LABEL = "MySQL";
    private static final String MAIN_COMMAND = "TownyDiscordChat";
    private static final String QUEUE_COMMAND = "tdcqueue";
    private static final String CONFIG_FILE_NAME = "config.yml";
    private static final String CONFIG_BACKUP_FILE_NAME = "config.yml.bak";
    private static final int TEMPLATE_BUFFER_BYTES = 8192;

    private static final String QUEUE_START_DELAY_KEY = "queue.startDelaySeconds";
    private static final String QUEUE_POLL_KEY = "queue.pollSeconds";
    private static final long DEFAULT_QUEUE_START_DELAY_SECONDS = 10L;
    private static final long DEFAULT_QUEUE_POLL_SECONDS = 1L;
    private static final String MIGRATE_TO_MYSQL_KEY = "storage.migrateToMySQL";
    private static final String MYSQL_USERNAME_KEY = "SQL.username";
    private static final String MYSQL_PASSWORD_KEY = "SQL.password";

    private static final String SAFE_MODE_REPLY_PREFIX = "[TownyDiscordChat] SAFE MODE: ";
    private static final String RELOAD_SUBCOMMAND = "reload";
    private static final String DEBUG_SUBCOMMAND = "debug";
    private static final String MIGRATION_REPLY_PREFIX = "[TDC] ";
    private static final String NOT_INITIALIZED_REASON = "Not initialized";
    private static final String CONFIG_MIGRATION_FAILED = "Automatic config migration failed. "
            + "config.yml.bak was preserved and no new config.yml was published. "
            + "Fix the backup or create a current config.yml manually; see console, then restart.";
    private static final String CONFIG_IMPORTED_REVIEW = "Configuration imported from config.yml.bak. "
            + "Review config.yml (especially storage, categories and permissions), "
            + "then restart to enable Discord synchronization.";
    private static final String DATABASE_CONNECTION_FAILED = "Database connection failed. "
            + "Check storage.type, database credentials and server availability; "
            + "see console for details.";
    private static final String DATABASE_SCHEMA_FAILED = "Database schema initialization failed. "
            + "Back up the database, check migration logs and table columns, then restart.";
    private static final String MIGRATION_REQUIRES_SQLITE = "Migration requires storage.type: sqlite.";
    private static final String MIGRATION_RUNNING = "SQLite to MySQL migration running; "
            + "synchronization paused until a server restart.";

    public static Main plugin;

    public FileConfiguration config;
    public Database SQL;
    public LogCategory logCategoryDB;
    public LogTextChannel logTextChannelDB;
    public Players playersDB;
    public TownRoles townRolesDB;
    public NationRoles nationRolesDB;
    public TownCategories townCategoriesDB;
    public NationCategories nationCategoriesDB;
    public TownTextChannels townTextChannelsDB;
    public TownVoiceChannels townVoiceChannelsDB;
    public NationTextChannels nationTextChannelsDB;
    public NationVoiceChannels nationVoiceChannelsDB;
    public Queue queueScheduler;
    public DiscordActivityLog activityLog;
    public Reconciler reconciler;
    public CompatibilityGuard compatibility;
    public AccountChangeListener accountListener;
    public OnboardingListener onboarding;
    private MembershipChangeListener membershipListener;
    private TaskScheduler scheduler;
    private MessageCatalog messages;
    private UpdateChecker updateChecker;
    private volatile RoleNames roleNames = RoleNames.defaults();

    private boolean operational;
    private volatile boolean guildValidated;
    private volatile String startupProblem;
    private volatile PluginStatus status = PluginStatus.STARTING;
    private volatile String adminWarning;

    private enum BackupImport {
        NONE,
        IMPORTED,
        FAILED
    }

    public boolean isOperational() {
        return operational;
    }

    public TaskScheduler scheduler() {
        return scheduler;
    }

    /** messages.yml, or the built-in English text before it has been read. */
    public MessageCatalog messages() {
        if (messages == null) {
            try {
                messages = MessageCatalog.builtIn(this);
            } catch (IOException missing) {
                throw new IllegalStateException("The plugin JAR is incomplete", missing);
            }
        }
        return messages;
    }

    /** Role name templates from the loaded config.yml. */
    public RoleNames roleNames() {
        return roleNames;
    }

    public String statusLabel() {
        return status.name();
    }

    public boolean isGuildValidated() {
        return guildValidated;
    }

    public void setGuildValidated(boolean ready) {
        guildValidated = ready;
    }

    public String getStartupProblem() {
        return startupProblem;
    }

    public String getSafeModeReason() {
        if (startupProblem != null) {
            return startupProblem;
        }
        return compatibility == null ? NOT_INITIALIZED_REASON : compatibility.problem();
    }

    /** Persistent operator-visible status for recoverable audit failures. */
    public String getAdminWarning() {
        return adminWarning;
    }

    public void clearAdminWarning() {
        adminWarning = null;
    }

    public void reportAdminWarning(String warning) {
        if (warning == null || warning.trim().isEmpty() || warning.equals(adminWarning)) {
            return;
        }
        adminWarning = warning;
        getLogger().warning("[ADMIN ACTION REQUIRED] " + warning);
        if (activityLog != null) {
            activityLog.publish("WARNING", "Administrator action required: " + warning, true);
        }
        if (isEnabled()) {
            scheduler.runGlobal(() -> notifyOnlineAdministrators(warning));
        }
    }

    private void notifyOnlineAdministrators(String warning) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(Permissions.NOTIFY)) {
                messages().send(player, "admin-warning", "warning", warning);
            }
        }
    }

    @Override
    public void onEnable() {
        plugin = this;
        scheduler = TaskSchedulers.create(this);
        // Started before any check that can leave the plugin in safe mode, so
        // new installs and upgrades that still need configuring are counted.
        startMetrics();

        // Optional plugins must pass preflight before config files or storage are touched.
        this.compatibility = new CompatibilityGuard(this);
        getServer().getPluginManager().registerEvents(compatibility, this);
        if (!compatibility.check()) {
            compatibility.warnConsole();
            enterSafeMode(PluginStatus.MISSING_DEPENDENCY, compatibility.problem());
            return;
        }
        startAfterDependencyPreflight();
    }

    private void startAfterDependencyPreflight() {
        BackupImport backupImport = importConfigBackupIfPresent();
        if (backupImport == BackupImport.FAILED) {
            return;
        }
        loadConfiguration();
        loadMessages();
        reportConfigurationChanges();
        validateConfiguration();
        startUpdateChecks();

        // Never start Discord reconciliation on the first boot after importing
        // unknown historical settings: the operator must review and restart once.
        if (backupImport == BackupImport.IMPORTED) {
            enterSafeMode(PluginStatus.CONFIG_REVIEW,
                    startupProblem != null ? startupProblem : CONFIG_IMPORTED_REVIEW);
            return;
        }
        if (startupProblem != null) {
            enterSafeMode(PluginStatus.INVALID_CONFIG, startupProblem);
            return;
        }
        startOperationalComponents();
    }

    private void startMetrics() {
        Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        PluginMetrics.register(metrics, getServer().getPluginManager(),
                PluginMetrics.state(() -> status, () -> guildValidated,
                        this::storageTypeLabel, () -> config));
    }

    /** Null until storage is configured, so bStats leaves the chart out. */
    private String storageTypeLabel() {
        if (SQL == null) {
            return null;
        }
        return SQL.getBackend() == Database.Backend.SQLITE ? SQLITE_LABEL : MYSQL_LABEL;
    }

    private void startOperationalComponents() {
        registerCommand(MAIN_COMMAND, new TownyDiscordChatCommand(this));
        registerCommand(QUEUE_COMMAND, new QueueCommand(this));
        createStorageAccessors();
        if (!connectStorage() || !initializeConnectedStorage()) {
            return;
        }
        // No legacy listeners are active: every membership mutation goes via
        // the durable desired-state queue, never directly to JDA.
        getLogger().info("TownyDiscordChat has been Enabled!");
    }

    private BackupImport importConfigBackupIfPresent() {
        File current = new File(getDataFolder(), CONFIG_FILE_NAME);
        File previous = new File(getDataFolder(), CONFIG_BACKUP_FILE_NAME);
        // Never let a stale .bak overwrite an already-installed config.yml.
        if (current.exists() || !previous.isFile()) {
            return BackupImport.NONE;
        }
        try {
            ConfigUpgrade.Result result = ConfigUpgrade.migrateIfNeeded(
                    getDataFolder(), readBundledConfigTemplate());
            if (!result.migrated) {
                return BackupImport.NONE;
            }
            logImportedBackup(result);
            return BackupImport.IMPORTED;
        } catch (IOException | RuntimeException e) {
            getLogger().severe("[CONFIG UPGRADE] Failed to migrate config.yml.bak: " + e.getMessage());
            enterSafeMode(PluginStatus.CONFIG_UPGRADE_FAILED, CONFIG_MIGRATION_FAILED);
            return BackupImport.FAILED;
        }
    }

    private String readBundledConfigTemplate() throws IOException {
        try (InputStream template = getResource(CONFIG_FILE_NAME)) {
            if (template == null) {
                throw new IOException("Missing bundled config.yml template");
            }
            ByteArrayOutputStream contents = new ByteArrayOutputStream();
            byte[] buffer = new byte[TEMPLATE_BUFFER_BYTES];
            int bytesRead;
            while ((bytesRead = template.read(buffer)) != -1) {
                contents.write(buffer, 0, bytesRead);
            }
            return new String(contents.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void logImportedBackup(ConfigUpgrade.Result result) {
        getLogger().info("[CONFIG UPGRADE] Created fresh config.yml from "
                + result.copied.size() + " compatible settings in config.yml.bak. "
                + "New defaults and comments were retained. The .bak file was not modified.");
        if (!result.ignored.isEmpty()) {
            getLogger().warning("[CONFIG UPGRADE] Ignored " + result.ignored.size()
                    + " obsolete/unknown backup keys: " + result.ignored
                    + ". Their values were not carried forward.");
        }
        getLogger().warning("[CONFIG UPGRADE] First startup will remain in safe mode. "
                + "Review the generated config.yml and restart to enable Discord synchronization.");
    }

    private void loadConfiguration() {
        saveDefaultConfig();
        reloadConfig();
        config = getConfig();
        roleNames = RoleNames.fromConfig(config);
    }

    private void reportConfigurationChanges() {
        List<String> obsolete = ConfigReport.obsoleteSettings(config);
        if (!obsolete.isEmpty()) {
            getLogger().info("[CONFIG] config.yml has settings this version no longer reads; "
                    + "you can delete them: " + String.join(", ", obsolete));
        }
        List<String> missing = ConfigReport.missingSections(config);
        if (!missing.isEmpty()) {
            getLogger().info("[CONFIG] config.yml has no " + String.join(", ", missing)
                    + " section(s), so their defaults are used. To add them with explanations, "
                    + "rename config.yml to config.yml.bak and restart; your values are kept.");
        }
    }

    private void startUpdateChecks() {
        if (updateChecker == null) {
            updateChecker = new UpdateChecker(this);
        }
        updateChecker.start();
    }

    private void loadMessages() {
        try {
            messages = MessageCatalog.load(this, legacyMessageSource());
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().warning("[MESSAGES] messages.yml could not be read, so the built-in "
                    + "English messages are used: " + e.getMessage());
            messages = null;
        }
    }

    // A config.yml.bak import drops 1.1.x message keys, so read them from the backup.
    private org.bukkit.configuration.ConfigurationSection legacyMessageSource() {
        File backup = new File(getDataFolder(), CONFIG_BACKUP_FILE_NAME);
        return backup.isFile() ? YamlConfiguration.loadConfiguration(backup) : config;
    }

    /**
     * /tdc reload. Applies a valid config.yml and messages.yml while running, or
     * starts synchronization when the only startup problem was the configuration.
     */
    public void reloadConfiguration(CommandSender sender) {
        if (status == PluginStatus.INVALID_CONFIG || status == PluginStatus.CONFIG_REVIEW) {
            startAfterReview(sender);
            return;
        }
        if (!operational) {
            messages().send(sender, "reload.not-possible", "reason", getSafeModeReason());
            return;
        }
        String problem = firstConfigProblem();
        if (problem != null) {
            messages().send(sender, "reload.invalid", "error", problem);
            return;
        }
        FileConfiguration previous = config;
        loadConfiguration();
        loadMessages();
        startUpdateChecks();
        messages().send(sender, "reload.success");
        List<String> restartOnly = RestartOnlySettings.changed(previous, config);
        if (!restartOnly.isEmpty()) {
            messages().send(sender, "reload.restart-needed", "settings", String.join(", ", restartOnly));
        }
        if (reconciler != null) {
            reconciler.requestSoon();
        }
    }

    private void startAfterReview(CommandSender sender) {
        String problem = firstConfigProblem();
        if (problem != null) {
            messages().send(sender, "reload.invalid", "error", problem);
            return;
        }
        loadConfiguration();
        loadMessages();
        startupProblem = null;
        status = PluginStatus.STARTING;
        startOperationalComponents();
        if (operational) {
            messages().send(sender, "reload.started");
        } else {
            messages().send(sender, "reload.not-possible", "reason", getSafeModeReason());
        }
    }

    /** Validates config.yml on disk without applying it; null means it is valid. */
    private String firstConfigProblem() {
        File file = new File(getDataFolder(), CONFIG_FILE_NAME);
        SetupValidator.Result fileCheck = SetupValidator.validateFile(file);
        if (!fileCheck.valid()) {
            logConfigErrors(fileCheck);
            return fileCheck.errors().get(0);
        }
        YamlConfiguration candidate = YamlConfiguration.loadConfiguration(file);
        candidate.setDefaults(getConfig().getDefaults());
        SetupValidator.Result configCheck = SetupValidator.validateLocal(candidate);
        if (configCheck.valid()) {
            return null;
        }
        logConfigErrors(configCheck);
        return configCheck.errors().get(0);
    }

    public void writeDebugReport(CommandSender sender) {
        new DebugReport(this).write(sender);
    }

    private void validateConfiguration() {
        SetupValidator.Result fileCheck = SetupValidator.validateFile(
                new File(getDataFolder(), CONFIG_FILE_NAME));
        SetupValidator.Result configCheck = SetupValidator.validateLocal(config);
        for (String warning : configCheck.warnings()) {
            getLogger().warning("[CONFIG] " + warning);
        }
        if (fileCheck.valid() && configCheck.valid()) {
            return;
        }
        logConfigErrors(fileCheck);
        logConfigErrors(configCheck);
        String firstError = !fileCheck.errors().isEmpty()
                ? fileCheck.errors().get(0)
                : configCheck.errors().get(0);
        startupProblem = "Invalid config.yml: " + firstError
                + " Check console for all errors, correct the file, then restart.";
    }

    private void logConfigErrors(SetupValidator.Result result) {
        for (String error : result.errors()) {
            getLogger().severe("[CONFIG] " + error);
        }
    }

    private void createStorageAccessors() {
        this.SQL = new Database(config, getDataFolder());
        this.activityLog = new DiscordActivityLog(this);
        this.logCategoryDB = new LogCategory(this);
        this.logTextChannelDB = new LogTextChannel(this);
        this.playersDB = new Players(this);
        this.townRolesDB = new TownRoles(this);
        this.nationRolesDB = new NationRoles(this);
        this.townCategoriesDB = new TownCategories(this);
        this.nationCategoriesDB = new NationCategories(this);
        this.townTextChannelsDB = new TownTextChannels(this);
        this.townVoiceChannelsDB = new TownVoiceChannels(this);
        this.nationTextChannelsDB = new NationTextChannels(this);
        this.nationVoiceChannelsDB = new NationVoiceChannels(this);
    }

    private boolean connectStorage() {
        try {
            SQL.connect();
            return true;
        } catch (ClassNotFoundException | SQLException e) {
            getLogger().severe("Database connection failed: " + e.getMessage());
            enterSafeMode(PluginStatus.DATABASE_UNAVAILABLE, DATABASE_CONNECTION_FAILED);
            return false;
        }
    }

    /** @return true when startup should finish normally and log that the plugin is enabled. */
    private boolean initializeConnectedStorage() {
        try {
            if (!SQL.isConnected()) {
                return true;
            }
            getLogger().info("Storage connected: " + SQL.getBackend());
            createLegacyTables();
            createQueueSchema();
            if (config.getBoolean(MIGRATE_TO_MYSQL_KEY, false)) {
                startOfflineMigrationMode();
                return false;
            }
            startSynchronizationServices();
            return true;
        } catch (SQLException e) {
            handleSchemaFailure(e);
            return false;
        }
    }

    private void createLegacyTables() {
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
    }

    private void createQueueSchema() throws SQLException {
        new DiscordJobStore(this).initialize();
        new MemberIntentStore(this).initialize();
        new StableEntityStore(this).initialize();
        new RankGrantStore(this).initialize();
        SQL.ensureIndexes();
        SQL.verifyRequiredTables();
    }

    // Explicit offline migration mode: no Discord or Towny schedulers start
    // while database state is being copied.
    private void startOfflineMigrationMode() {
        status = PluginStatus.STORAGE_MIGRATION;
        if (SQL.getBackend() != Database.Backend.SQLITE) {
            startupProblem = MIGRATION_REQUIRES_SQLITE;
            getLogger().severe(startupProblem);
        } else {
            startupProblem = MIGRATION_RUNNING;
            getLogger().warning(startupProblem);
            scheduler.runAsync(this::migrateSqliteToMySql);
        }
        replyWithStatusOnly(MIGRATION_REPLY_PREFIX);
    }

    private void migrateSqliteToMySql() {
        try {
            StorageMigration.Result result = StorageMigration.sqliteToMySQL(
                    SQL, SQL.getMysqlUrl(), config.getString(MYSQL_USERNAME_KEY, ""),
                    config.getString(MYSQL_PASSWORD_KEY, ""), getLogger());
            startupProblem = "Migration completed: " + result.rows + " rows across "
                    + result.tables + " tables copied to MySQL. "
                    + "Set storage.type: mysql and storage.migrateToMySQL: false, "
                    + "then restart the server. SQLite remains unchanged.";
            getLogger().warning(startupProblem);
        } catch (Exception e) {
            startupProblem = "Migration FAILED; SQLite remains authoritative. "
                    + "Fix MySQL connectivity/schema and restart. Cause: " + e.getMessage();
            getLogger().severe(startupProblem);
            getLogger().log(Level.SEVERE, "Storage migration failed", e);
        }
    }

    // Reconciler waits for DiscordSRV's guild instead of blocking server startup on it.
    private void startSynchronizationServices() {
        this.reconciler = new Reconciler(this);
        this.queueScheduler = new Queue(
                config.getLong(QUEUE_START_DELAY_KEY, DEFAULT_QUEUE_START_DELAY_SECONDS),
                config.getLong(QUEUE_POLL_KEY, DEFAULT_QUEUE_POLL_SECONDS), this);
        this.membershipListener = new MembershipChangeListener(this);
        getServer().getPluginManager().registerEvents(membershipListener, this);
        this.onboarding = new OnboardingListener(this);
        getServer().getPluginManager().registerEvents(onboarding, this);
        this.accountListener = new AccountChangeListener(this);
        DiscordSRV.api.subscribe(accountListener);
        this.operational = true;
        this.status = PluginStatus.RUNNING;
    }

    private void handleSchemaFailure(SQLException cause) {
        getLogger().severe("Failed to initialize database/queue schema: " + cause.getMessage());
        enterSafeMode(PluginStatus.DATABASE_SCHEMA_FAILED, DATABASE_SCHEMA_FAILED);
        try {
            if (SQL != null && SQL.isConnected()) {
                SQL.disconnect();
            }
        } catch (SQLException closeError) {
            getLogger().warning("Could not close database after startup failure: "
                    + closeError.getMessage());
        }
    }

    /** Called by CompatibilityGuard when Towny or DiscordSRV stops while the server runs. */
    void handleDependencyDisabled(String dependency) {
        String reason = CompatibilityGuard.disabledAfterStartupProblem(dependency);
        getLogger().severe("[DEPENDENCY] " + reason);
        // Safe mode or storage migration already has an earlier, more specific
        // cause and nothing to stop; replacing it would hide that cause.
        if (startupProblem != null) {
            return;
        }
        stopSynchronizationServices();
        enterSafeMode(PluginStatus.MISSING_DEPENDENCY, reason);
    }

    private void stopSynchronizationServices() {
        operational = false;
        guildValidated = false;
        unsubscribeAccountListener();
        accountListener = null;
        unregisterListener(membershipListener);
        unregisterListener(onboarding);
        membershipListener = null;
        // Cancels the queue, reconciler and onboarding reminders in one step.
        scheduler.cancelAll();
        queueScheduler = null;
        reconciler = null;
    }

    private static void unregisterListener(Listener listener) {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
    }

    private void enterSafeMode(PluginStatus cause, String reason) {
        status = cause;
        operational = false;
        guildValidated = false;
        startupProblem = reason;
        getLogger().severe("SAFE MODE: " + reason);
        replyWithStatusOnly(SAFE_MODE_REPLY_PREFIX);
    }

    // Admins can still reload a fixed configuration or write a support report.
    private void replyWithStatusOnly(String prefix) {
        CommandExecutor statusReply = (sender, command, label, args) -> {
            String requested = args.length == 0 ? "" : args[0];
            if (RELOAD_SUBCOMMAND.equalsIgnoreCase(requested) && sender.hasPermission(Permissions.RELOAD)) {
                reloadConfiguration(sender);
            } else if (DEBUG_SUBCOMMAND.equalsIgnoreCase(requested) && sender.hasPermission(Permissions.DEBUG)) {
                writeDebugReport(sender);
            } else {
                sender.sendMessage(prefix + getSafeModeReason());
            }
            return true;
        };
        for (String name : new String[]{MAIN_COMMAND, QUEUE_COMMAND}) {
            requireCommand(name).setExecutor(statusReply);
            requireCommand(name).setTabCompleter(null);
        }
    }

    private void registerCommand(String name, TabExecutor executor) {
        requireCommand(name).setExecutor(executor);
        requireCommand(name).setTabCompleter(executor);
    }

    private PluginCommand requireCommand(String name) {
        return Objects.requireNonNull(getCommand(name), "Command missing from plugin.yml: " + name);
    }

    @Override
    public void onDisable() {
        operational = false;
        guildValidated = false;
        unsubscribeAccountListener();
        if (queueScheduler != null) {
            queueScheduler.cancel();
        }
        if (reconciler != null) {
            reconciler.cancel();
        }
        disconnectStorage();
        getLogger().info("TownyDiscordChat has been Disabled!");
    }

    private void unsubscribeAccountListener() {
        if (accountListener == null) {
            return;
        }
        // DiscordSRV may already be disabled or partially unloaded during server shutdown.
        try {
            DiscordSRV.api.unsubscribe(accountListener);
        } catch (Throwable e) {
            getLogger().warning("Could not unsubscribe DiscordSRV listener: " + e);
        }
    }

    private void disconnectStorage() {
        try {
            if (SQL != null && SQL.isConnected()) {
                SQL.disconnect();
                getLogger().info("Database is disconnected!");
            }
        } catch (SQLException e) {
            getLogger().log(Level.WARNING, "Could not disconnect database during shutdown", e);
        }
    }
}
