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
 * Stable Towny UUID -> Discord IDs, independent of a mutable Towny name.
 * All calls are asynchronous/off the Minecraft server thread.
 */
public final class StableEntityStore {
    private final DiscordJobStore jobs;
    private static final String TABLE = "tdc_towny_identity";

    public static final class Entity {
        public final String guild, kind, uuid, name, previousName, roleId, textId, voiceId;
        public final boolean renamed;
        Entity(String guild, String kind, String uuid, String name, String old,
               String role, String text, String voice, boolean renamed) {
            this.guild = guild;
            this.kind = kind;
            this.uuid = uuid;
            this.name = name;
            this.previousName = old;
            this.roleId = role;
            this.textId = text;
            this.voiceId = voice;
            this.renamed = renamed;
        }
        public String idFor(String component) {
            if ("ROLE".equals(component)) return roleId;
            if ("TEXT".equals(component)) return textId;
            if ("VOICE".equals(component)) return voiceId;
            return null;
        }
    }

    public StableEntityStore(Main plugin) {
        this.jobs = new DiscordJobStore(plugin);
    }

    public void initialize() throws SQLException {
        try (Connection c = jobs.connect(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                    + "guild_id VARCHAR(24) NOT NULL,"
                    + "kind VARCHAR(8) NOT NULL,"
                    + "towny_uuid VARCHAR(36) NOT NULL,"
                    + "last_name VARCHAR(128) NOT NULL,"
                    + "role_id VARCHAR(24),text_id VARCHAR(24),voice_id VARCHAR(24),"
                    + "PRIMARY KEY(guild_id,kind,towny_uuid),"
                    + "KEY idx_identity_name(guild_id,kind,last_name)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            s.executeUpdate("INSERT IGNORE INTO tdc_schema_migrations "
                    + "(version,applied_at,description) VALUES "
                    + "(3," + System.currentTimeMillis() + ","
                    + "'Stable Towny UUID mapping; preserve original mapping tables')");
        }
    }

    /**
     * Records observed resources for an existing UUID. On a rename, moves the
     * original SQL keys without changing any Discord snowflake IDs, cancels
     * unclaimed jobs for the old name, and queues in-place Discord renames.
     *
     * Never infer a new UUID by matching a name owned by another UUID.
     */
    public Entity sync(String guild, String kind, String uuid, String name,
                       String observedRole, String observedText, String observedVoice)
            throws SQLException {
        if (!("TOWN".equals(kind) || "NATION".equals(kind))
                || uuid == null || !uuid.matches("[0-9a-fA-F-]{36}")
                || name == null || name.isEmpty() || name.length() > 128)
            throw new SQLException("Invalid Towny identity input");

        try (Connection c = jobs.connect()) {
            c.setAutoCommit(false);
            try {
                Entity before = findForUpdate(c, guild, kind, uuid);
                if (before == null) {
                    // Another UUID's record may represent a deleted/replaced
                    // Towny object. Do not adopt its Discord resources.
                    if (nameClaimed(c, guild, kind, name, uuid)) {
                        throw new SQLException("Towny identity collision: " + kind + "/" + name
                                + " is still owned by an older UUID. Cleanup must complete"
                                + " before granting access to a new town/nation with the same name.");
                    }
                    try (PreparedStatement insert = c.prepareStatement(
                            "INSERT INTO " + TABLE + " (guild_id,kind,towny_uuid,last_name,"
                              + "role_id,text_id,voice_id) VALUES (?,?,?,?,?,?,?)")) {
                        insert.setString(1,guild);
                        insert.setString(2,kind);
                        insert.setString(3,uuid);
                        insert.setString(4,name);
                        insert.setString(5,observedRole);
                        insert.setString(6,observedText);
                        insert.setString(7,observedVoice);
                        insert.executeUpdate();
                    }
                    c.commit();
                    return new Entity(guild,kind,uuid,name,null,observedRole,observedText,observedVoice,false);
                }

                boolean renamed = !before.name.equals(name);
                if (renamed) {
                    if (nameClaimed(c,guild,kind,name,uuid))
                        throw new SQLException("Towny name " + name + " already bound to a different UUID");
                    moveLegacy(c,kind,before.name,name);
                    cancelObsolete(c,kind,before.name);
                }
                // Never overwrite stable IDs during an ordinary snapshot.
                // A resource replacement is recorded on completed creation.
                String role = before.roleId == null ? observedRole : before.roleId;
                String text = before.textId == null ? observedText : before.textId;
                String voice = before.voiceId == null ? observedVoice : before.voiceId;
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE " + TABLE + " SET last_name=?,role_id=?,text_id=?,voice_id=?"
                        + " WHERE guild_id=? AND kind=? AND towny_uuid=?")) {
                    ps.setString(1,name);
                    ps.setString(2,role);
                    ps.setString(3,text);
                    ps.setString(4,voice);
                    ps.setString(5,guild);
                    ps.setString(6,kind);
                    ps.setString(7,uuid);
                    ps.executeUpdate();
                }
                if (renamed) {
                    if (role != null) addJob(c,kind,"ROLE",uuid);
                    if (text != null) addJob(c,kind,"TEXT",uuid);
                    if (voice != null) addJob(c,kind,"VOICE",uuid);
                }
                c.commit();
                return new Entity(guild,kind,uuid,name,before.name,role,text,voice,renamed);
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public Entity find(String guild, String kind, String uuid) throws SQLException {
        try (Connection c = jobs.connect()) {
            return findForUpdate(c,guild,kind,uuid);
        }
    }

    private Entity findForUpdate(Connection c, String guild, String kind, String uuid)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT last_name,role_id,text_id,voice_id FROM " + TABLE
                + " WHERE guild_id=? AND kind=? AND towny_uuid=? FOR UPDATE")) {
            ps.setString(1,guild);
            ps.setString(2,kind);
            ps.setString(3,uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Entity(guild,kind,uuid,rs.getString(1),null,
                        rs.getString(2),rs.getString(3),rs.getString(4),false);
            }
        }
    }

    public List<Entity> all(String guild, String kind) throws SQLException {
        List<Entity> result = new ArrayList<Entity>();
        try (Connection c = jobs.connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT towny_uuid,last_name,role_id,text_id,voice_id FROM " + TABLE
                + " WHERE guild_id=? AND kind=?")) {
            ps.setString(1,guild);
            ps.setString(2,kind);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    result.add(new Entity(guild,kind,rs.getString(1),rs.getString(2),
                            null,rs.getString(3),rs.getString(4),rs.getString(5),false));
            }
        }
        return result;
    }

    /** Keep mapping IDs coherent with a completed create/adoption on the SQL branch. */
    void recordCreated(Connection c, String kind, String name, String discordId)
            throws SQLException {
        if (!("TOWN_ROLE".equals(kind) || "NATION_ROLE".equals(kind)
                || "TOWN_TEXT".equals(kind) || "NATION_TEXT".equals(kind)
                || "TOWN_VOICE".equals(kind) || "NATION_VOICE".equals(kind))) return;
        String type = kind.startsWith("TOWN") ? "TOWN" : "NATION";
        String field = kind.endsWith("ROLE") ? "role_id" :
                kind.endsWith("TEXT") ? "text_id" : "voice_id";
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE " + TABLE + " SET " + field + "=? WHERE kind=? AND last_name=?")) {
            ps.setString(1,discordId);
            ps.setString(2,type);
            ps.setString(3,name);
            ps.executeUpdate();
        }
    }

    /**
     * A recorded Discord ID is evidence that a pending cleanup may actually
     * target a renamed entity. Abort deletion if a still-live UUID owns it.
     */
    public java.util.Set<String> ownersOfId(String guild, String kind, String discordId)
            throws SQLException {
        String component = kind.endsWith("ROLE") ? "role_id" :
                kind.endsWith("TEXT") ? "text_id" : "voice_id";
        String type = kind.contains("TOWN") ? "TOWN" : "NATION";
        java.util.Set<String> owners = new java.util.HashSet<String>();
        if (discordId == null) return owners;
        try (Connection c = jobs.connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT towny_uuid FROM " + TABLE + " WHERE guild_id=? AND kind=? AND "
                + component + "=?")) {
            ps.setString(1,guild);
            ps.setString(2,type);
            ps.setString(3,discordId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) owners.add(rs.getString(1));
            }
        }
        return owners;
    }

    /**
     * After all components were deleted, retire the old identity so a fresh
     * Towny object can adopt the name. The historical legacy tables remain.
     */
    public void retireIfEmpty(String guild,String kind,String uuid) throws SQLException {
        try (Connection c = jobs.connect(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM " + TABLE + " WHERE guild_id=? AND kind=? AND towny_uuid=? "
                + "AND role_id IS NULL AND text_id IS NULL AND voice_id IS NULL")) {
            ps.setString(1,guild);
            ps.setString(2,kind);
            ps.setString(3,uuid);
            ps.executeUpdate();
        }
    }

    void clearDeleted(Connection c, String kind, String name, String deletedId)
            throws SQLException {
        if (!kind.startsWith("DELETE_")) return;
        String original = kind.substring(7);
        String type = original.startsWith("TOWN") ? "TOWN" : "NATION";
        String field = original.endsWith("ROLE") ? "role_id" :
                original.endsWith("TEXT") ? "text_id" : "voice_id";
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE " + TABLE + " SET " + field + "=NULL WHERE kind=? "
                + "AND last_name=? AND " + field + "=?")) {
            ps.setString(1,type);
            ps.setString(2,name);
            ps.setString(3,deletedId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM " + TABLE + " WHERE kind=? AND last_name=? "
                + "AND role_id IS NULL AND text_id IS NULL AND voice_id IS NULL")) {
            ps.setString(1,type);
            ps.setString(2,name);
            ps.executeUpdate();
        }
    }

    private boolean nameClaimed(Connection c,String guild,String kind,String name,String uuid)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT towny_uuid FROM " + TABLE
                + " WHERE guild_id=? AND kind=? AND last_name=? AND towny_uuid<>?")) {
            ps.setString(1,guild);
            ps.setString(2,kind);
            ps.setString(3,name);
            ps.setString(4,uuid);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static void addJob(Connection c,String kind,String component,String uuid)
            throws SQLException {
        String jobKind = "RENAME_" + kind + "_" + component;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT IGNORE INTO tdc_discord_jobs"
                + " (job_key,kind,resource_name,status,priority,available_at) "
                + "VALUES (?,?,?,'PENDING',20,0)")) {
            ps.setString(1,jobKind + ":" + uuid);
            ps.setString(2,jobKind);
            ps.setString(3,uuid);
            ps.executeUpdate();
        }
    }

    private static void cancelObsolete(Connection c,String kind,String oldName)
            throws SQLException {
        for (String component : new String[]{"ROLE","TEXT","VOICE"}) {
            for (String prefix : new String[]{"","DELETE_"}) {
                String jobKind = prefix + kind + "_" + component;
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM tdc_discord_jobs WHERE job_key=? "
                        + "AND status IN ('PENDING','DEAD')")) {
                    ps.setString(1,jobKind + ":" + oldName);
                    ps.executeUpdate();
                }
            }
        }
    }

    private static void moveLegacy(Connection c,String kind,String previous,String next)
            throws SQLException {
        String base = "TOWN".equals(kind) ? "town" : "nation";
        String role = base + "RoleName";
        for (String table : new String[]{"tdc_" + base + "_roles",
                "tdc_" + base + "_text_channels",
                "tdc_" + base + "_voice_channels"}) {
            boolean previousExists;
            try (PreparedStatement check = c.prepareStatement(
                    "SELECT COUNT(*) FROM " + table + " WHERE " + role + "=?")) {
                check.setString(1,previous);
                try (ResultSet rows = check.executeQuery()) {
                    rows.next();
                    previousExists = rows.getInt(1) > 0;
                }
            }
            if (!previousExists) continue;
            try (PreparedStatement check = c.prepareStatement(
                    "SELECT COUNT(*) FROM " + table + " WHERE " + role + "=?")) {
                check.setString(1,next);
                try (ResultSet rows = check.executeQuery()) {
                    rows.next();
                    if (rows.getInt(1)>0) throw new SQLException(
                            "Cannot migrate " + table + ": desired name already used");
                }
            }
            String sql = "UPDATE " + table + " SET " + role + "=?";
            if (table.endsWith("_text_channels")) sql += "," + base + "TextChannelName=?";
            if (table.endsWith("_voice_channels")) sql += "," + base + "VoiceChannelName=?";
            sql += " WHERE " + role + "=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1,next);
                if (table.endsWith("_roles")) ps.setString(2,previous);
                else {
                    ps.setString(2,next);
                    ps.setString(3,previous);
                }
                ps.executeUpdate();
            }
        }
    }
}
