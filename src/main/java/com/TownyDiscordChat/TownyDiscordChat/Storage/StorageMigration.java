package com.TownyDiscordChat.TownyDiscordChat.Storage;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Offline, full-fidelity SQLite -> MySQL export for a single TDC server.
 *
 * This runs before the Discord worker/schedulers are constructed. It copies all
 * tdc_* tables (legacy mappings, stable UUIDs, in-flight jobs, revisions, dead
 * letters, audit metadata) without accessing Discord or changing SQLite.
 *
 * The MySQL destination must contain NO TDC rows. Copy is transactional;
 * DDL is idempotent, and failures roll back all copied data. Source remains
 * authoritative until the administrator explicitly selects MySQL and restarts.
 */
public final class StorageMigration {
    private StorageMigration() { }

    public static final class Result {
        public final int tables;
        public final long rows;
        public Result(int tables,long rows) { this.tables=tables; this.rows=rows; }
    }

    public static Result sqliteToMySQL(Database source, String targetUrl,
                                       String username, String password,
                                       Logger log) throws SQLException {
        if (source.getBackend()!=Database.Backend.SQLITE)
            throw new SQLException("Source must be SQLite. Set storage.type: sqlite.");
        try (Connection sqlite=source.openConnection();
             Connection mysql=Database.openMySQL(targetUrl,username,password)) {
            // Startup has NOT launched a queue, so the SQLite snapshot is stable.
            sqlite.setAutoCommit(false);
            List<String> tables = new ArrayList<String>();
            Map<String,String> schemas = new TreeMap<String,String>();
            try (Statement ps=sqlite.createStatement();
                 ResultSet rs=ps.executeQuery("SELECT name,sql FROM sqlite_master "
                         + "WHERE type='table' AND name LIKE 'tdc_%' ORDER BY name")) {
                while (rs.next()) {
                    String table=rs.getString("name");
                    if (!safeName(table)) throw new SQLException("Unsafe table name in source: "+table);
                    String ddl=rs.getString("sql");
                    if (ddl==null) throw new SQLException("Missing SQLite schema for "+table);
                    tables.add(table);
                    schemas.put(table,ddl);
                }
            }
            if (tables.isEmpty())
                throw new SQLException("No tdc_* tables found in SQLite: aborting empty export.");

            // Hard stop if the chosen MySQL schema has existing data. It might
            // be an old production DB; merging it without manual decisions is unsafe.
            for (String table:existingPluginTables(mysql)) {
                try (Statement check=mysql.createStatement();
                     ResultSet rows=check.executeQuery("SELECT COUNT(*) FROM `"+table+"`")) {
                    rows.next();
                    if (rows.getLong(1)!=0L)
                        throw new SQLException("MySQL destination table "+table
                                +" is not empty. Use a NEW, EMPTY database/schema; "
                                +"no data was copied.");
                }
            }

            // MySQL DDL commits implicitly, so create all empty tables BEFORE
            // starting the destination data transaction. Data can then be
            // rolled back completely if any insert or verification fails.
            for (String table:tables) {
                String ddl=toMySQLDDL(schemas.get(table));
                try (Statement statement=mysql.createStatement()) {
                    statement.executeUpdate(ddl);
                }
                verifyColumns(sqlite,mysql,table);
            }
            ensureMySQLIndexes(mysql,tables);

            long total=0L;
            mysql.setAutoCommit(false);
            try {
                for (String table:tables) {
                    long inserted=copyTable(sqlite,mysql,table);
                    total+=inserted;
                    if (inserted!=count(mysql,table))
                        throw new SQLException("Data verification failed for "+table
                                +": copied "+inserted+", target has "+count(mysql,table));
                    if (log!=null) log.info("[STORAGE MIGRATION] "+table+": "+inserted+" rows copied");
                }
                if (tables.contains("tdc_schema_migrations")) {
                    try (PreparedStatement marker=mysql.prepareStatement(
                            "INSERT INTO tdc_schema_migrations(version,applied_at,description)"
                            + " VALUES (6,?,'Offline SQLite to MySQL copy')")) {
                        marker.setLong(1,System.currentTimeMillis());
                        marker.executeUpdate();
                    }
                }
                mysql.commit();
            } catch (Exception error) {
                mysql.rollback();
                if (error instanceof SQLException) throw (SQLException)error;
                throw new SQLException("Storage migration failed; target rolled back",error);
            }
            sqlite.commit();
            return new Result(tables.size(),total);
        }
    }

    private static boolean safeName(String name) {
        return name!=null && name.matches("tdc_[A-Za-z0-9_]+");
    }

    private static List<String> existingPluginTables(Connection mysql) throws SQLException {
        List<String> tables=new ArrayList<String>();
        DatabaseMetaData meta=mysql.getMetaData();
        try (ResultSet rs=meta.getTables(mysql.getCatalog(),null,"tdc_%",new String[]{"TABLE"})) {
            while (rs.next()) {
                String table=rs.getString("TABLE_NAME");
                if (safeName(table)) tables.add(table);
            }
        }
        return tables;
    }

    private static String toMySQLDDL(String sqlite) throws SQLException {
        String ddl=sqlite.replaceAll("(?i)\\bINTEGER\\s+PRIMARY\\s+KEY\\s+AUTOINCREMENT\\b",
                "BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY");
        if (!ddl.matches("(?is)^\\s*CREATE\\s+TABLE\\s+.*"))
            throw new SQLException("Unexpected SQLite schema; refusing to execute: "+ddl);
        if (!ddl.toUpperCase(java.util.Locale.ROOT).contains("IF NOT EXISTS"))
            ddl=ddl.replaceFirst("(?i)CREATE\\s+TABLE\\s+","CREATE TABLE IF NOT EXISTS ");
        return ddl+" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    }

    private static void verifyColumns(Connection sqlite,Connection mysql,String table) throws SQLException {
        List<String> src=columns(sqlite,table);
        List<String> dest=columns(mysql,table);
        if (!src.equals(dest))
            throw new SQLException("MySQL schema mismatch for "+table+
                    ": source columns "+src+" vs destination columns "+dest);
    }

    private static List<String> columns(Connection connection,String table) throws SQLException {
        List<String> cols=new ArrayList<String>();
        try (Statement st=connection.createStatement();
             ResultSet rs=st.executeQuery("SELECT * FROM `"+table+"` WHERE 1=0")) {
            ResultSetMetaData meta=rs.getMetaData();
            for (int i=1;i<=meta.getColumnCount();i++) {
                String col=meta.getColumnName(i);
                if (!col.matches("[A-Za-z0-9_]+")) throw new SQLException("Unsafe SQL column: "+col);
                cols.add(col.toLowerCase(java.util.Locale.ROOT));
            }
        }
        return cols;
    }

    private static long count(Connection conn,String table) throws SQLException {
        try (Statement st=conn.createStatement();
             ResultSet rs=st.executeQuery("SELECT COUNT(*) FROM `"+table+"`")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static long copyTable(Connection sqlite,Connection mysql,String table) throws SQLException {
        StringBuilder columns=new StringBuilder();
        StringBuilder placeholders=new StringBuilder();
        List<String> fields=columns(sqlite,table);
        for (int i=0;i<fields.size();i++) {
            if (i>0) { columns.append(','); placeholders.append(','); }
            columns.append('`').append(fields.get(i)).append('`');
            placeholders.append('?');
        }
        String query="INSERT INTO `"+table+"` ("+columns+") VALUES ("+placeholders+")";
        long rows=0;
        try (Statement st=sqlite.createStatement();
             ResultSet rs=st.executeQuery("SELECT * FROM `"+table+"`");
             PreparedStatement ins=mysql.prepareStatement(query)) {
            while(rs.next()) {
                for (int i=1;i<=fields.size();i++) {
                    Object value=rs.getObject(i);
                    if (value==null) ins.setNull(i,java.sql.Types.NULL);
                    else if (value instanceof byte[]) ins.setBytes(i,(byte[])value);
                    else ins.setObject(i,value);
                }
                ins.addBatch();
                rows++;
                if (rows%500==0) ins.executeBatch();
            }
            ins.executeBatch();
        }
        return rows;
    }

    private static void ensureMySQLIndexes(Connection c,List<String> tables) throws SQLException {
        String[][] indexes = {
                {"tdc_discord_jobs","idx_ready","status,available_at,lease_until"},
                {"tdc_member_intents","idx_member_claim","desired,status,priority,available_at,lease_until"},
                {"tdc_towny_identity","idx_identity_name","guild_id,kind,last_name"}
        };
        for (String[] index:indexes) {
            if (!tables.contains(index[0])) continue;
            boolean exists=false;
            try (ResultSet info=c.getMetaData().getIndexInfo(c.getCatalog(),null,index[0],false,false)) {
                while (info.next())
                    if (index[1].equalsIgnoreCase(info.getString("INDEX_NAME"))) exists=true;
            }
            if (!exists) {
                try (Statement statement=c.createStatement()) {
                    statement.executeUpdate("CREATE INDEX "+index[1]+" ON "+index[0]
                            +"("+index[2]+")");
                }
            }
        }
    }
}
