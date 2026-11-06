package com.TownyDiscordChat.TownyDiscordChat.Queue;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Durable, deduplicated JDBC work queue (SQLite or MySQL). All methods must be called off the Bukkit
 * server thread. Each operation owns its JDBC connection (legacy SQL uses a
 * different connection), so Discord's callbacks never share a JDBC statement.
 */
public final class DiscordJobStore {
    private final Main plugin;
    private static final String TABLE = "tdc_discord_jobs";

    public static final class Job {
        public final long id;
        public final String kind;
        public final String name;
        public final int attempts;
        public final long leaseUntil;

        Job(long id, String kind, String name, int attempts, long leaseUntil) {
            this.id = id;
            this.kind = kind;
            this.name = name;
            this.attempts = attempts;
            this.leaseUntil = leaseUntil;
        }

        @Override
        public String toString() {
            return kind + ":" + name + " (#" + id + ", attempt " + attempts + ")";
        }
    }

    public DiscordJobStore(Main plugin) {
        this.plugin = plugin;
    }

    Connection connect() throws SQLException {
        // All legacy SQL tables and durable stores share the selected backend.
        return plugin.SQL.openConnection();
    }

    public void initialize() throws SQLException {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                    + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "job_key VARCHAR(191) NOT NULL UNIQUE,"
                    + "kind VARCHAR(40) NOT NULL,"
                    + "resource_name VARCHAR(128) NOT NULL,"
                    + "status VARCHAR(16) NOT NULL DEFAULT 'PENDING',"
                    + "priority SMALLINT NOT NULL DEFAULT 100,"
                    + "attempts INT NOT NULL DEFAULT 0,"
                    + "available_at BIGINT NOT NULL DEFAULT 0,"
                    + "lease_until BIGINT NOT NULL DEFAULT 0,"
                    + "last_error VARCHAR(512) NULL,"
                    + "INDEX idx_ready (status,available_at,lease_until)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS tdc_audit_runs ("
                    + "guild_id VARCHAR(24) PRIMARY KEY,last_success_at BIGINT NOT NULL"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
        ensurePriorityColumn();
        migrateLegacyRows();
        migrateLegacyFalseRows();
        try (Connection c = connect()) {
            LegacyExpiryMigration.migrate(c);
        }
    }

    private void ensurePriorityColumn() throws SQLException {
        try (Connection c = connect()) {
            boolean existing;
            try (ResultSet columns = c.getMetaData().getColumns(
                    c.getCatalog(), null, TABLE, "priority")) {
                existing = columns.next();
            }
            if (!existing) {
                try (Statement statement = c.createStatement()) {
                    statement.executeUpdate("ALTER TABLE " + TABLE
                            + " ADD COLUMN priority SMALLINT NOT NULL DEFAULT 100");
                }
            }
        }
    }

    private static String key(String kind, String name) {
        return kind + ":" + name;
    }

    /** INSERT IGNORE guarantees at most one pending or running job per kind+name. */
    public void enqueue(String kind, String name) throws SQLException {
        if (name == null || name.trim().isEmpty() || name.length() > 128
                || key(kind, name).length() > 191) return;
        try (Connection c = connect()) {
            enqueue(c, kind, name);
        }
    }

    private void enqueue(Connection c, String kind, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT IGNORE INTO " + TABLE
                        + " (job_key,kind,resource_name,status,available_at,priority) VALUES (?,?,?,'PENDING',?,?)")) {
            ps.setString(1, key(kind, name));
            ps.setString(2, kind);
            ps.setString(3, name);
            long delay = kind.startsWith("DELETE_")
                    ? Math.max(30L,plugin.getConfig().getLong("queue.deletionGraceSeconds",120L))*1000L : 0L;
            ps.setLong(4,System.currentTimeMillis()+delay);
            ps.setInt(5, kind.startsWith("DELETE_") ? 10
                    : kind.startsWith("RENAME_") ? 20
                    : "AUDIT_GUILD".equals(kind) ? 30
                    : "AUDIT_MEMBER".equals(kind) ? 40 : 100);
            ps.executeUpdate();
        }
    }

    /**
     * Non-destructive migration from the 2023 SQL branch. Legacy tables are
     * never dropped or emptied: a transactional migration marker makes this
     * idempotent even after multiple restarts. Back up MySQL before upgrading.
     */
    private void migrateLegacyRows() throws SQLException {
        try (Connection c = connect(); Statement statement = c.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS tdc_schema_migrations ("
                    + "version INT PRIMARY KEY, applied_at BIGINT NOT NULL,"
                    + "description VARCHAR(190) NOT NULL) ENGINE=InnoDB");
            c.setAutoCommit(false);
            try (PreparedStatement check = c.prepareStatement(
                    "SELECT version FROM tdc_schema_migrations WHERE version=2 FOR UPDATE");
                    ResultSet exists = check.executeQuery()) {
                if (exists.next()) { c.commit(); return; }
            }
            boolean legacyExists;
            try (ResultSet tables = c.getMetaData().getTables(c.getCatalog(), null,
                    "tdc_queued_tasks", null)) { legacyExists = tables.next(); }
            if (legacyExists) {
                try (PreparedStatement read = c.prepareStatement(
                        "SELECT Time,townRoleName,nationRoleName,townCategoryName,nationCategoryName,"
                        + "townTextChannelName,nationTextChannelName,townVoiceChannelName,nationVoiceChannelName"
                        + " FROM tdc_queued_tasks WHERE Expired IS NULL");
                     ResultSet rows = read.executeQuery()) {
                    String[] kinds = {"TOWN_ROLE","NATION_ROLE","TOWN_CATEGORY","NATION_CATEGORY",
                            "TOWN_TEXT","NATION_TEXT","TOWN_VOICE","NATION_VOICE"};
                    while (rows.next()) {
                        for (int i = 0; i < kinds.length; i++) {
                            String name = rows.getString(i+2);
                            if (name != null && !name.isEmpty() && !name.equalsIgnoreCase("null")
                                    && name.length() <= 128) enqueue(c, kinds[i], name);
                        }
                    }
                }
            }
            try (PreparedStatement marker = c.prepareStatement(
                    "INSERT IGNORE INTO tdc_schema_migrations(version,applied_at,description)"
                    + " VALUES (2,?,'Import legacy queue; leave legacy SQL tables intact')")) {
                marker.setLong(1, System.currentTimeMillis());
                marker.executeUpdate();
            }
            try { c.commit(); }
            catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    /**
     * Follow-up import for 2023 installations that stored unexpired status as
     * the VARCHAR strings 'false', '0' or ''. Version 2 only imported NULL.
     * This is a one-time, idempotent migration; no legacy rows are deleted.
     */
    private void migrateLegacyFalseRows() throws SQLException {
        try (Connection c=connect()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement check=c.prepareStatement(
                        "SELECT version FROM tdc_schema_migrations WHERE version=4 FOR UPDATE");
                     ResultSet rs=check.executeQuery()) {
                    if (rs.next()) { c.commit(); return; }
                }
                boolean oldTable;
                try (ResultSet rs=c.getMetaData().getTables(c.getCatalog(),null,
                        "tdc_queued_tasks",null)) { oldTable=rs.next(); }
                if (oldTable) {
                    try (PreparedStatement ps=c.prepareStatement(
                            "SELECT Time,townRoleName,nationRoleName,townCategoryName,nationCategoryName,"
                            + "townTextChannelName,nationTextChannelName,townVoiceChannelName,nationVoiceChannelName"
                            + " FROM tdc_queued_tasks WHERE LOWER(TRIM(Expired)) IN ('false','0','')");
                         ResultSet rows=ps.executeQuery()) {
                        String[] kinds={"TOWN_ROLE","NATION_ROLE","TOWN_CATEGORY","NATION_CATEGORY",
                                "TOWN_TEXT","NATION_TEXT","TOWN_VOICE","NATION_VOICE"};
                        while (rows.next()) {
                            for (int i=0;i<kinds.length;i++) {
                                String value=rows.getString(i+2);
                                if (value!=null && !value.isEmpty() && value.length()<=128
                                        && !value.equalsIgnoreCase("null"))
                                    enqueue(c,kinds[i],value);
                            }
                        }
                    }
                }
                try (PreparedStatement ps=c.prepareStatement(
                        "INSERT IGNORE INTO tdc_schema_migrations(version,applied_at,description)"
                        + " VALUES (4,?,'Import legacy queue rows marked false/0')")) {
                    ps.setLong(1,System.currentTimeMillis());
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    /**
     * Locks one due row and claims it atomically. Expired leases are eligible so
     * queued work survives process crashes. Only one worker is used per plugin.
     */
    public Job claim() throws SQLException {
        long now = System.currentTimeMillis();
        long leaseUntil = now + Math.max(60, plugin.getConfig().getLong("queue.leaseSeconds", 300)) * 1000L;
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id,kind,resource_name,attempts FROM " + TABLE + " WHERE "
                    + "(status='PENDING' AND available_at<=?) OR "
                    + "(status='RUNNING' AND lease_until<=?) ORDER BY priority,available_at,id LIMIT 1 FOR UPDATE")) {
                ps.setLong(1, now);
                ps.setLong(2, now);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        c.commit();
                        return null;
                    }
                    long id = rs.getLong("id");
                    int attempts = rs.getInt("attempts") + 1;
                    String kind = rs.getString("kind");
                    String name = rs.getString("resource_name");
                    if ("AUDIT_GUILD".equals(kind))
                        leaseUntil = Math.max(leaseUntil, now + 1800000L);
                    try (PreparedStatement update = c.prepareStatement(
                            "UPDATE " + TABLE + " SET status='RUNNING',attempts=?,lease_until=? WHERE id=?")) {
                        update.setInt(1, attempts);
                        update.setLong(2, leaseUntil);
                        update.setLong(3, id);
                        update.executeUpdate();
                    }
                    c.commit();
                    return new Job(id, kind, name, attempts, leaseUntil);
                }
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    /**
     * Only SQL-mapped IDs are eligible for automated resource deletion. A stale
     * name is never sufficient proof that a Discord role/channel belongs to TDC.
     */
    public String mappedId(String kind, String name) throws SQLException {
        String original = kind.startsWith("DELETE_") ? kind.substring("DELETE_".length()) : kind;
        return mappedIds(original).get(name);
    }

    public void succeedDeletion(Job job, String remoteId) throws SQLException {
        if (!job.kind.startsWith("DELETE_")) throw new SQLException("Unexpected delete kind");
        String original = job.kind.substring("DELETE_".length());
        final String table, nameColumn, idColumn;
        switch (original) {
            case "TOWN_ROLE":
                table="tdc_town_roles"; nameColumn="townRoleName"; idColumn="townRoleId"; break;
            case "NATION_ROLE":
                table="tdc_nation_roles"; nameColumn="nationRoleName"; idColumn="nationRoleId"; break;
            case "TOWN_TEXT":
                table="tdc_town_text_channels"; nameColumn="townTextChannelName"; idColumn="townTextChannelId"; break;
            case "NATION_TEXT":
                table="tdc_nation_text_channels"; nameColumn="nationTextChannelName"; idColumn="nationTextChannelId"; break;
            case "TOWN_VOICE":
                table="tdc_town_voice_channels"; nameColumn="townVoiceChannelName"; idColumn="townVoiceChannelId"; break;
            case "NATION_VOICE":
                table="tdc_nation_voice_channels"; nameColumn="nationVoiceChannelName"; idColumn="nationVoiceChannelId"; break;
            default: throw new SQLException("Unknown deletion type " + original);
        }
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                if (!stillOwned(c, job)) { c.rollback(); return; }
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM " + table + " WHERE " + nameColumn + "=? AND " + idColumn + "=?")) {
                    ps.setString(1,job.name);
                    ps.setString(2,remoteId);
                    ps.executeUpdate();
                }
                new StableEntityStore(plugin).clearDeleted(c,job.kind,job.name,remoteId);
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM " + TABLE + " WHERE id=? AND attempts=?")) {
                    ps.setLong(1,job.id);
                    ps.setInt(2,job.attempts);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException ex) { c.rollback(); throw ex; }
        }
    }

    /** Existing category IDs let the worker adopt orphaned Discord categories after a crash. */
    public Set<String> trackedCategoryIds(String kind) throws SQLException {
        String table;
        String column;
        if ("TOWN_CATEGORY".equals(kind)) {
            table = "tdc_town_categories";
            column = "townCategoryId";
        } else if ("NATION_CATEGORY".equals(kind)) {
            table = "tdc_nation_categories";
            column = "nationCategoryId";
        } else {
            return Collections.emptySet();
        }
        Set<String> ids = new HashSet<String>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT " + column + " FROM " + table);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ids.add(rs.getString(1));
        }
        return ids;
    }


    /** Returns names already committed to the existing SQL mapping tables. */
    public Set<String> knownNames(String kind) throws SQLException {
        final String table;
        final String column;
        switch (kind) {
            case "TOWN_ROLE": table = "tdc_town_roles"; column = "townRoleName"; break;
            case "NATION_ROLE": table = "tdc_nation_roles"; column = "nationRoleName"; break;
            case "TOWN_TEXT": table = "tdc_town_text_channels"; column = "townTextChannelName"; break;
            case "NATION_TEXT": table = "tdc_nation_text_channels"; column = "nationTextChannelName"; break;
            case "TOWN_VOICE": table = "tdc_town_voice_channels"; column = "townVoiceChannelName"; break;
            case "NATION_VOICE": table = "tdc_nation_voice_channels"; column = "nationVoiceChannelName"; break;
            default: return Collections.emptySet();
        }
        Set<String> names = new HashSet<String>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT " + column + " FROM " + table + " WHERE Expired IS NULL");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) names.add(rs.getString(1));
        }
        return names;
    }


    /** Read the authoritative resource IDs previously recorded in MySQL. */
    public java.util.Map<String,String> mappedIds(String kind) throws SQLException {
        String table;
        String nameColumn;
        String idColumn;
        switch (kind) {
            case "TOWN_ROLE":
                table = "tdc_town_roles"; nameColumn = "townRoleName"; idColumn = "townRoleId"; break;
            case "NATION_ROLE":
                table = "tdc_nation_roles"; nameColumn = "nationRoleName"; idColumn = "nationRoleId"; break;
            case "TOWN_TEXT":
                table = "tdc_town_text_channels"; nameColumn = "townTextChannelName"; idColumn = "townTextChannelId"; break;
            case "NATION_TEXT":
                table = "tdc_nation_text_channels"; nameColumn = "nationTextChannelName"; idColumn = "nationTextChannelId"; break;
            case "TOWN_VOICE":
                table = "tdc_town_voice_channels"; nameColumn = "townVoiceChannelName"; idColumn = "townVoiceChannelId"; break;
            case "NATION_VOICE":
                table = "tdc_nation_voice_channels"; nameColumn = "nationVoiceChannelName"; idColumn = "nationVoiceChannelId"; break;
            default: return java.util.Collections.emptyMap();
        }
        java.util.Map<String,String> result = new java.util.HashMap<String,String>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT " + nameColumn + "," + idColumn + " FROM " + table + " WHERE Expired IS NULL");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.put(rs.getString(1), rs.getString(2));
        }
        return result;
    }

    /**
     * Adopt verified categories found in Discord, even when this MySQL schema
     * is empty. Never delete or overwrite a different category ID.
     */
    public void adoptCategory(String kind, String name, String id) throws SQLException {
        String table, nameColumn, idColumn;
        if ("TOWN_CATEGORY".equals(kind)) {
            table = "tdc_town_categories"; nameColumn = "townCategoryName"; idColumn = "townCategoryId";
        } else if ("NATION_CATEGORY".equals(kind)) {
            table = "tdc_nation_categories"; nameColumn = "nationCategoryName"; idColumn = "nationCategoryId";
        } else {
            throw new SQLException("Unexpected category kind " + kind);
        }
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO " + table + " (" + nameColumn + "," + idColumn + ") VALUES (?,?)"
                        + " ON DUPLICATE KEY UPDATE " + nameColumn + "=VALUES(" + nameColumn + "),Expired=NULL")) {
            ps.setString(1, name);
            ps.setString(2, id);
            ps.executeUpdate();
        }
    }
    public int pendingCount(String kind, String name) throws SQLException {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM " + TABLE + " WHERE job_key=? AND status!='DEAD'")) {
            ps.setString(1, key(kind, name));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    static boolean stillOwned(Connection c, Job job) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM " + TABLE + " WHERE id=? AND status='RUNNING' AND attempts=? FOR UPDATE")) {
            ps.setLong(1, job.id);
            ps.setInt(2, job.attempts);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Save the Discord ID and acknowledge in the SAME database transaction. */
    public void succeed(Job job, String discordId, String parentId) throws SQLException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                if (!stillOwned(c, job)) {
                    c.rollback();
                    return; // stale JDA callback from an expired lease
                }
                record(c, job, discordId, parentId);
                new StableEntityStore(plugin).recordCreated(c,job.kind,job.name,discordId);
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM " + TABLE + " WHERE id=? AND attempts=?")) {
                    ps.setLong(1, job.id);
                    ps.setInt(2, job.attempts);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private void record(Connection c, Job job, String id, String parent) throws SQLException {
        String sql;
        String category = null;
        switch (job.kind) {
            case "TOWN_ROLE":
                sql = "INSERT INTO tdc_town_roles (townRoleName,townRoleId) VALUES (?,?) "
                        + "ON DUPLICATE KEY UPDATE townRoleId=VALUES(townRoleId),Expired=NULL";
                break;
            case "NATION_ROLE":
                sql = "INSERT INTO tdc_nation_roles (nationRoleName,nationRoleId) VALUES (?,?) "
                        + "ON DUPLICATE KEY UPDATE nationRoleId=VALUES(nationRoleId),Expired=NULL";
                break;
            case "TOWN_CATEGORY":
                sql = "INSERT INTO tdc_town_categories (townCategoryName,townCategoryId) VALUES (?,?) "
                        + "ON DUPLICATE KEY UPDATE townCategoryName=VALUES(townCategoryName),Expired=NULL";
                break;
            case "NATION_CATEGORY":
                sql = "INSERT INTO tdc_nation_categories (nationCategoryName,nationCategoryId) VALUES (?,?) "
                        + "ON DUPLICATE KEY UPDATE nationCategoryName=VALUES(nationCategoryName),Expired=NULL";
                break;
            case "TOWN_TEXT":
            case "NATION_TEXT":
            case "TOWN_VOICE":
            case "NATION_VOICE":
                boolean town = job.kind.startsWith("TOWN");
                boolean text = job.kind.endsWith("TEXT");
                String prefix = town ? "town" : "nation";
                String type = text ? "Text" : "Voice";
                String table = "tdc_" + (town ? "town" : "nation") + "_" + (text ? "text" : "voice") + "_channels";
                String roleCol = prefix + "RoleName";
                String nameCol = prefix + type + "ChannelName";
                String idCol = prefix + type + "ChannelId";
                String parentNameCol = prefix + type + "ChannelParentCategoryName";
                String parentIdCol = prefix + type + "ChannelParentCategoryId";
                category = plugin.getConfig().getString("category." + (town ? "Town" : "Nation") + ".Name", "");
                sql = "INSERT INTO " + table + " (" + roleCol + "," + nameCol + "," + idCol + ","
                        + parentNameCol + "," + parentIdCol + ") VALUES (?,?,?,?,?) "
                        + "ON DUPLICATE KEY UPDATE " + nameCol + "=VALUES(" + nameCol + "),"
                        + idCol + "=VALUES(" + idCol + ")," + parentNameCol + "=VALUES("
                        + parentNameCol + ")," + parentIdCol + "=VALUES(" + parentIdCol + "),Expired=NULL";
                break;
            default:
                throw new SQLException("Unknown job kind " + job.kind);
        }
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, job.name);
            if (category == null) {
                ps.setString(2, id);
            } else {
                ps.setString(2, job.name);
                ps.setString(3, id);
                ps.setString(4, category);
                ps.setString(5, parent);
            }
            ps.executeUpdate();
        }
    }

    /**
     * If a dependent role/category isn't ready, reschedule without burning the
     * finite retry budget. A permanent API failure does consume attempts.
     */
    public void defer(Job job, String reason) throws SQLException {
        reschedule(job, reason, true);
    }

    public void fail(Job job, String reason) throws SQLException {
        reschedule(job, reason, false);
    }

    public static long retryDelayMillis(int attempts, int baseSeconds, int maxSeconds) {
        long value = Math.max(1, baseSeconds) * 1000L;
        for (int i = 1; i < attempts; i++) value = Math.min((long) Math.max(1, maxSeconds) * 1000L, value * 2L);
        return Math.min(value, (long) Math.max(1, maxSeconds) * 1000L);
    }

    private void reschedule(Job job, String reason, boolean prerequisite) throws SQLException {
        int maxAttempts = Math.max(1, plugin.getConfig().getInt("queue.maxAttempts", 10));
        int base = Math.max(1, plugin.getConfig().getInt("queue.retryBaseSeconds", 30));
        int max = Math.max(base, plugin.getConfig().getInt("queue.retryMaxSeconds", 900));
        long delay = prerequisite ? 30000L : retryDelayMillis(job.attempts, base, max);
        String status = !prerequisite && job.attempts >= maxAttempts ? "DEAD" : "PENDING";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "UPDATE " + TABLE + " SET status=?, attempts=?,available_at=?,lease_until=0,last_error=? "
                + "WHERE id=? AND status='RUNNING' AND attempts=?")) {
            ps.setString(1, status);
            ps.setInt(2, prerequisite ? Math.max(0, job.attempts - 1) : job.attempts);
            ps.setLong(3, System.currentTimeMillis() + delay);
            ps.setString(4, reason == null ? null : reason.substring(0, Math.min(reason.length(), 512)));
            ps.setLong(5, job.id);
            ps.setInt(6, job.attempts);
            ps.executeUpdate();
        }
    }


    /** Delete a task whose Towny entity has been removed. */
    public void remove(Job job) throws SQLException {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM " + TABLE + " WHERE id=? AND status='RUNNING' AND attempts=?")) {
            ps.setLong(1, job.id);
            ps.setInt(2, job.attempts);
            ps.executeUpdate();
        }
    }


    /** Counts statuses for administrative diagnostics. */
    public java.util.Map<String, Integer> statusCounts() throws SQLException {
        java.util.Map<String, Integer> counts = new java.util.HashMap<String, Integer>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT status,COUNT(*) FROM " + TABLE + " GROUP BY status");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) counts.put(rs.getString(1), rs.getInt(2));
        }
        return counts;
    }

    /** Durable audit cadence; a successful full-guild scan is not repeated on every poll. */
    public void enqueueFullAuditIfDue(String guildId, long intervalSeconds) throws SQLException {
        long cutoff = System.currentTimeMillis() - Math.max(900L,intervalSeconds)*1000L;
        try (Connection c=connect(); PreparedStatement check=c.prepareStatement(
                "SELECT last_success_at FROM tdc_audit_runs WHERE guild_id=?")) {
            check.setString(1,guildId);
            try (ResultSet rs=check.executeQuery()) {
                if (rs.next() && rs.getLong(1) >= cutoff) return;
            }
        }
        enqueue("AUDIT_GUILD",guildId);
    }

    /** Manual admin request reactivates an exhausted audit without changing other jobs. */
    public void requestFullAudit(String guildId) throws SQLException {
        if (guildId == null || !guildId.matches("[0-9]{1,24}"))
            throw new SQLException("Invalid Discord guild ID");
        try (Connection c=connect(); PreparedStatement ps=c.prepareStatement(
                "INSERT INTO " + TABLE
                + " (job_key,kind,resource_name,status,available_at,priority)"
                + " VALUES (?,'AUDIT_GUILD',?,'PENDING',0,30)"
                + " ON DUPLICATE KEY UPDATE"
                + " attempts=IF(status='DEAD',0,attempts),"
                + " available_at=IF(status='DEAD',0,available_at),"
                + " last_error=IF(status='DEAD',NULL,last_error),"
                + " status=IF(status='DEAD','PENDING',status)")) {
            ps.setString(1,key("AUDIT_GUILD",guildId));
            ps.setString(2,guildId);
            ps.executeUpdate();
        }
    }

    /** Persist a complete guild audit checkpoint and acknowledge atomically. */
    public void finishFullAudit(Job job) throws SQLException {
        if (!"AUDIT_GUILD".equals(job.kind)) throw new SQLException("Not a guild audit");
        try (Connection c=connect()) {
            c.setAutoCommit(false);
            try {
                if (!stillOwned(c,job)) { c.rollback(); return; }
                try (PreparedStatement ps=c.prepareStatement(
                        "INSERT INTO tdc_audit_runs(guild_id,last_success_at) VALUES (?,?)"
                        + " ON DUPLICATE KEY UPDATE last_success_at=VALUES(last_success_at)")) {
                    ps.setString(1,job.name);
                    ps.setLong(2,System.currentTimeMillis());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps=c.prepareStatement(
                        "DELETE FROM " + TABLE + " WHERE id=? AND attempts=?")) {
                    ps.setLong(1,job.id);
                    ps.setInt(2,job.attempts);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    /** Allows recovery from a DEAD job without duplicating healthy work. */
    public int retryDead() throws SQLException {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "UPDATE " + TABLE + " SET status='PENDING',attempts=0,available_at=0,last_error=NULL WHERE status='DEAD'")) {
            return ps.executeUpdate();
        }
    }
}
