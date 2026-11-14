package com.TownyDiscordChat.TownyDiscordChat;

import com.TownyDiscordChat.TownyDiscordChat.Commands.TDCCommand;
import com.TownyDiscordChat.TownyDiscordChat.Commands.TDCSQL;
import com.TownyDiscordChat.TownyDiscordChat.Listeners.AccountChangeListener;
import com.TownyDiscordChat.TownyDiscordChat.Listeners.MembershipChangeListener;
import com.TownyDiscordChat.TownyDiscordChat.Listeners.OnboardingListener;
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
import com.TownyDiscordChat.TownyDiscordChat.Queue.StableEntityStore;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Queue.Queue;
import com.TownyDiscordChat.TownyDiscordChat.Schedulers.Reconciler;
import com.TownyDiscordChat.TownyDiscordChat.Storage.Database;
import com.TownyDiscordChat.TownyDiscordChat.Storage.StorageMigration;
import github.scarsz.discordsrv.DiscordSRV;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;

public class Main extends JavaPlugin {

    private static final int BSTATS_PLUGIN_ID = 10980;
    private static final String MAIN_COMMAND = "TownyDiscordChat";
    private static final String QUEUE_COMMAND = "tdcqueue";
    private static final String ADMIN_PERMISSION = "TownyDiscordChat.Admin";
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

    private boolean operational;
    private volatile boolean guildValidated;
    private volatile String startupProblem;
    private volatile String adminWarning;

    private enum BackupImport {
        NONE,
        IMPORTED,
        FAILED
    }

    public boolean isOperational() {
        return operational;
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
            Bukkit.getScheduler().runTask(this, () -> notifyOnlineAdministrators(warning));
        }
    }

    private void notifyOnlineAdministrators(String warning) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp() || player.hasPermission(ADMIN_PERMISSION)) {
                player.sendMessage(ChatColor.RED + "[TownyDiscordChat] " + warning);
            }
        }
    }

    @Override
    public void onEnable() {
        plugin = this;

        // Optional plugins must pass preflight before config files or storage are touched.
        this.compatibility = new CompatibilityGuard(this);
        getServer().getPluginManager().registerEvents(compatibility, this);
        if (!compatibility.check()) {
            compatibility.warnConsole();
            enterSafeMode(compatibility.problem());
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
        validateConfiguration();

        // Never start Discord reconciliation on the first boot after importing
        // unknown historical settings: the operator must review and restart once.
        if (backupImport == BackupImport.IMPORTED) {
            enterSafeMode(startupProblem != null ? startupProblem : CONFIG_IMPORTED_REVIEW);
            return;
        }
        if (startupProblem != null) {
            enterSafeMode(startupProblem);
            return;
        }
        startOperationalComponents();
    }

    private void startOperationalComponents() {
        new Metrics(this, BSTATS_PLUGIN_ID);
        requireCommand(MAIN_COMMAND).setExecutor(new TDCCommand());
        requireCommand(QUEUE_COMMAND).setExecutor(new TDCSQL());
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
            enterSafeMode(CONFIG_MIGRATION_FAILED);
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
            enterSafeMode(DATABASE_CONNECTION_FAILED);
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
        SQL.ensureIndexes();
        SQL.verifyRequiredTables();
    }

    // Explicit offline migration mode: no Discord or Towny schedulers start
    // while database state is being copied.
    private void startOfflineMigrationMode() {
        if (SQL.getBackend() != Database.Backend.SQLITE) {
            startupProblem = MIGRATION_REQUIRES_SQLITE;
            getLogger().severe(startupProblem);
        } else {
            startupProblem = MIGRATION_RUNNING;
            getLogger().warning(startupProblem);
            getServer().getScheduler().runTaskAsynchronously(this, this::migrateSqliteToMySql);
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
        getServer().getPluginManager().registerEvents(new MembershipChangeListener(this), this);
        this.onboarding = new OnboardingListener(this);
        getServer().getPluginManager().registerEvents(onboarding, this);
        this.accountListener = new AccountChangeListener(this);
        DiscordSRV.api.subscribe(accountListener);
        this.operational = true;
    }

    private void handleSchemaFailure(SQLException cause) {
        getLogger().severe("Failed to initialize database/queue schema: " + cause.getMessage());
        enterSafeMode(DATABASE_SCHEMA_FAILED);
        try {
            if (SQL != null && SQL.isConnected()) {
                SQL.disconnect();
            }
        } catch (SQLException closeError) {
            getLogger().warning("Could not close database after startup failure: "
                    + closeError.getMessage());
        }
    }

    private void enterSafeMode(String reason) {
        operational = false;
        guildValidated = false;
        startupProblem = reason;
        getLogger().severe("SAFE MODE: " + reason);
        replyWithStatusOnly(SAFE_MODE_REPLY_PREFIX);
    }

    private void replyWithStatusOnly(String prefix) {
        CommandExecutor statusReply = (sender, command, label, args) -> {
            sender.sendMessage(prefix + getSafeModeReason());
            return true;
        };
        requireCommand(MAIN_COMMAND).setExecutor(statusReply);
        requireCommand(QUEUE_COMMAND).setExecutor(statusReply);
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
