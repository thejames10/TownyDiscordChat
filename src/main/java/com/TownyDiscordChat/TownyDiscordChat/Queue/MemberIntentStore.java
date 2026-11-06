package com.TownyDiscordChat.TownyDiscordChat.Queue;

import com.TownyDiscordChat.TownyDiscordChat.Main;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * One durable desired state for each guild/member/role tuple. This is NOT two
 * independent ADD/REMOVE messages: each newer intent supersedes the older one.
 * A revision fences late JDA callbacks from deleting a newer desired state.
 */
public final class MemberIntentStore {
    private static final String TABLE = "tdc_member_intents";
    private static final String TRACKED = "tdc_managed_member_roles";
    private final Main plugin;
    private final DiscordJobStore jobs;

    public static final class Intent {
        public final long id;
        public final String guildId;
        public final String memberId;
        public final String roleId;
        public final boolean desired;
        public final long revision;
        public final int attempts;
        public final long leaseUntil;

        Intent(long id, String guildId, String memberId, String roleId,
               boolean desired, long revision, int attempts, long leaseUntil) {
            this.id = id;
            this.guildId = guildId;
            this.memberId = memberId;
            this.roleId = roleId;
            this.desired = desired;
            this.revision = revision;
            this.attempts = attempts;
            this.leaseUntil = leaseUntil;
        }

        @Override public String toString() {
            return (desired ? "GRANT " : "REVOKE ") + guildId + "/" + memberId + "/" + roleId
                    + " revision=" + revision + " attempt=" + attempts;
        }
    }

    public static final class Grant {
        public final String memberId;
        public final String roleId;
        public Grant(String memberId, String roleId) {
            this.memberId = memberId;
            this.roleId = roleId;
        }
    }

    public MemberIntentStore(Main plugin) {
        this.plugin = plugin;
        this.jobs = new DiscordJobStore(plugin);
    }

    private Connection connect() throws SQLException { return jobs.connect(); }

    public void initialize() throws SQLException {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                    + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "guild_id VARCHAR(24) NOT NULL,"
                    + "member_id VARCHAR(24) NOT NULL,"
                    + "role_id VARCHAR(24) NOT NULL,"
                    + "desired TINYINT(1) NOT NULL,"
                    + "revision BIGINT NOT NULL DEFAULT 1,"
                    + "priority SMALLINT NOT NULL DEFAULT 100,"
                    + "status VARCHAR(16) NOT NULL DEFAULT 'PENDING',"
                    + "attempts INT NOT NULL DEFAULT 0,"
                    + "available_at BIGINT NOT NULL DEFAULT 0,"
                    + "lease_until BIGINT NOT NULL DEFAULT 0,"
                    + "last_error VARCHAR(512) NULL,"
                    + "UNIQUE KEY uq_member_role (guild_id,member_id,role_id),"
                    + "KEY idx_member_claim (desired,status,priority,available_at,lease_until)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS " + TRACKED + " ("
                    + "guild_id VARCHAR(24) NOT NULL, member_id VARCHAR(24) NOT NULL,"
                    + "role_id VARCHAR(24) NOT NULL, PRIMARY KEY (guild_id,member_id,role_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    private static boolean validId(String id) {
        return id != null && id.matches("[0-9]{1,24}");
    }

    /**
     * Coalesce rapid join/kick/rejoin updates. If one REST call is already
     * underway, leave it RUNNING until its callback arrives; the revision
     * forces the superseding intent to run afterwards, avoiding REST reordering.
     */
    public void desire(String guild, String member, String role, boolean shouldHave) throws SQLException {
        if (!validId(guild) || !validId(member) || !validId(role))
            throw new SQLException("Invalid Discord snowflake supplied for membership intent");
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO " + TABLE + " (guild_id,member_id,role_id,desired,priority,status)"
              + " VALUES (?,?,?,?,?,'PENDING') ON DUPLICATE KEY UPDATE"
              + " status=IF(desired<>VALUES(desired),IF(status='RUNNING','RUNNING','PENDING'),status),"
              + " attempts=IF(desired<>VALUES(desired) AND status<>'RUNNING',0,attempts),"
              + " available_at=IF(desired<>VALUES(desired),0,available_at),"
              + " revision=IF(desired<>VALUES(desired),revision+1,revision),"
              + " priority=VALUES(priority),desired=VALUES(desired)")) {
            ps.setString(1, guild);
            ps.setString(2, member);
            ps.setString(3, role);
            ps.setBoolean(4, shouldHave);
            ps.setInt(5, shouldHave ? 100 : 0);
            ps.executeUpdate();
        }
    }

    /**
     * If an intent exists from an earlier kick but Towny has since restored
     * access, update it even if JDA already reports the role present.
     * Do not enqueue a fresh grant when no outstanding intent exists.
     */
    public void supersedeExisting(String guild, String member, String role, boolean desired)
            throws SQLException {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "UPDATE " + TABLE + " SET "
                + "status=IF(status='RUNNING','RUNNING','PENDING'),"
                + "attempts=IF(status='RUNNING',attempts,0),"
                + "available_at=0,revision=revision+1,priority=?,desired=?"
                + " WHERE guild_id=? AND member_id=? AND role_id=? AND desired<>?")) {
            ps.setInt(1, desired ? 100 : 0);
            ps.setBoolean(2, desired);
            bindTriple(ps, guild, member, role);
            ps.setBoolean(6, desired);
            ps.executeUpdate();
        }
    }

    public Intent claim(boolean revocations) throws SQLException {
        final long now = System.currentTimeMillis();
        final long lease = now + Math.max(60, plugin.getConfig().getLong("queue.leaseSeconds", 300)) * 1000L;
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id,guild_id,member_id,role_id,desired,revision,attempts FROM " + TABLE
                    + " WHERE desired=? AND ((status='PENDING' AND available_at<=?)"
                    + " OR (status='RUNNING' AND lease_until<=?))"
                    + " ORDER BY priority,available_at,id LIMIT 1 FOR UPDATE")) {
                ps.setBoolean(1, !revocations);
                ps.setLong(2, now);
                ps.setLong(3, now);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { c.commit(); return null; }
                    long id = rs.getLong("id");
                    int attempts = rs.getInt("attempts") + 1;
                    Intent intent = new Intent(id, rs.getString("guild_id"), rs.getString("member_id"),
                            rs.getString("role_id"), rs.getBoolean("desired"), rs.getLong("revision"),
                            attempts, lease);
                    try (PreparedStatement update = c.prepareStatement(
                            "UPDATE " + TABLE + " SET status='RUNNING',attempts=?,lease_until=? WHERE id=?")) {
                        update.setInt(1, attempts);
                        update.setLong(2, lease);
                        update.setLong(3, id);
                        update.executeUpdate();
                    }
                    c.commit();
                    return intent;
                }
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    /**
     * A successful mutation is recorded with acknowledgement in one transaction.
     * When the desired state changed in-flight, the old callback only re-queues
     * the newest revision (it cannot erase a new security revocation).
     */
    public void succeed(Intent intent) throws SQLException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                long revision = currentRevision(c, intent);
                if (revision == -1) { c.rollback(); return; }
                if (revision != intent.revision) {
                    restartSuperseded(c, intent);
                    c.commit();
                    return;
                }
                if (intent.desired) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT IGNORE INTO " + TRACKED + " (guild_id,member_id,role_id) VALUES (?,?,?)")) {
                        bindTriple(ps, intent.guildId, intent.memberId, intent.roleId);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = c.prepareStatement(
                            "DELETE FROM " + TRACKED + " WHERE guild_id=? AND member_id=? AND role_id=?")) {
                        bindTriple(ps, intent.guildId, intent.memberId, intent.roleId);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM " + TABLE + " WHERE id=? AND revision=? AND attempts=?")) {
                    ps.setLong(1, intent.id);
                    ps.setLong(2, intent.revision);
                    ps.setInt(3, intent.attempts);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    private long currentRevision(Connection c, Intent intent) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT revision FROM " + TABLE + " WHERE id=? AND attempts=? AND status='RUNNING' FOR UPDATE")) {
            ps.setLong(1, intent.id);
            ps.setInt(2, intent.attempts);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : -1L; }
        }
    }

    private void restartSuperseded(Connection c, Intent intent) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE " + TABLE + " SET status='PENDING',attempts=0,available_at=0,lease_until=0"
              + " WHERE id=? AND attempts=? AND revision<>?")) {
            ps.setLong(1, intent.id);
            ps.setInt(2, intent.attempts);
            ps.setLong(3, intent.revision);
            ps.executeUpdate();
        }
    }


    /** Dependency or network absence is retried indefinitely without burning attempts. */
    public void defer(Intent intent, String reason) throws SQLException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                long revision = currentRevision(c, intent);
                if (revision == -1) { c.rollback(); return; }
                if (revision != intent.revision) {
                    restartSuperseded(c, intent);
                } else {
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE " + TABLE + " SET status='PENDING',attempts=?,"
                            + "available_at=?,lease_until=0,last_error=? WHERE id=? AND revision=? AND attempts=?")) {
                        ps.setInt(1, Math.max(0, intent.attempts-1));
                        ps.setLong(2, System.currentTimeMillis() + 30000L);
                        ps.setString(3, reason);
                        ps.setLong(4, intent.id);
                        ps.setLong(5, intent.revision);
                        ps.setInt(6, intent.attempts);
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    public void fail(Intent intent, String reason) throws SQLException {
        long delay = DiscordJobStore.retryDelayMillis(intent.attempts,
                Math.max(1, plugin.getConfig().getInt("queue.retryBaseSeconds", 30)),
                Math.max(30, plugin.getConfig().getInt("queue.retryMaxSeconds", 900)));
        int maxAttempts = Math.max(1, plugin.getConfig().getInt("queue.maxAttempts", 10));
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                long revision = currentRevision(c, intent);
                if (revision == -1) { c.rollback(); return; }
                if (revision != intent.revision) {
                    restartSuperseded(c, intent);
                } else {
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE " + TABLE + " SET status=?,available_at=?,lease_until=0,last_error=?"
                          + " WHERE id=? AND attempts=? AND revision=?")) {
                        ps.setString(1, intent.attempts >= maxAttempts ? "DEAD" : "PENDING");
                        ps.setLong(2, System.currentTimeMillis() + delay);
                        ps.setString(3, reason == null ? null : reason.substring(0, Math.min(512,reason.length())));
                        ps.setLong(4, intent.id);
                        ps.setInt(5, intent.attempts);
                        ps.setLong(6, intent.revision);
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    private static void bindTriple(PreparedStatement ps, String guild, String member, String role)
            throws SQLException {
        ps.setString(1, guild);
        ps.setString(2, member);
        ps.setString(3, role);
    }

    public List<Grant> tracked(String guild) throws SQLException {
        List<Grant> list = new ArrayList<Grant>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT member_id,role_id FROM " + TRACKED + " WHERE guild_id=?")) {
            ps.setString(1, guild);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Grant(rs.getString(1), rs.getString(2)));
            }
        }
        return list;
    }

    /** Save observed pre-existing Discord membership, including on an empty database. */
    public void observe(String guild, List<Grant> present) throws SQLException {
        if (present.isEmpty()) return;
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT IGNORE INTO " + TRACKED + " (guild_id,member_id,role_id) VALUES (?,?,?)")) {
            for (Grant grant : present) {
                bindTriple(ps, guild, grant.memberId, grant.roleId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public int retryDead() throws SQLException {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "UPDATE " + TABLE + " SET status='PENDING',attempts=0,available_at=0,last_error=NULL"
              + " WHERE status='DEAD'")) { return ps.executeUpdate(); }
    }

    public java.util.Map<String,Integer> statusCounts() throws SQLException {
        java.util.Map<String,Integer> results = new java.util.HashMap<String,Integer>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT status,COUNT(*) FROM " + TABLE + " GROUP BY status");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) results.put(rs.getString(1),rs.getInt(2));
        }
        return results;
    }
}
