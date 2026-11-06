package com.TownyDiscordChat.TownyDiscordChat.Storage;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

/**
 * Single-server JDBC connection factory. The historical plugin.SQL field now
 * points here; legacy mapping tables and the durable worker use the SAME
 * configured backend. No automatic transfer of old data is performed.
 */
public final class Database {
    public enum Backend { SQLITE, MYSQL }
    private final Backend backend;
    private final File directory;
    private final File sqliteFile;
    private final String mysqlUrl;
    private final String username;
    private final String password;
    private Connection legacyConnection;

    public Database(FileConfiguration config, File pluginDataFolder) {
        this.directory=pluginDataFolder;
        String type=config.getString("storage.type","auto").toLowerCase(Locale.ROOT);
        boolean anyMysqlConfig = meaningful(config.getString("SQL.database"))
                || meaningful(config.getString("SQL.username"))
                || meaningful(config.getString("SQL.password"));
        if ("auto".equals(type)) type=anyMysqlConfig?"mysql":"sqlite";
        if ("mysql".equals(type)) backend=Backend.MYSQL;
        else if ("sqlite".equals(type)) backend=Backend.SQLITE;
        else throw new IllegalArgumentException("storage.type must be auto, sqlite, or mysql");
        String path=config.getString("storage.sqlite.file","townydiscordchat.db");
        if (!path.matches("[A-Za-z0-9_-]+\\.db"))
            throw new IllegalArgumentException("storage.sqlite.file must be a .db filename in the plugin directory");
        sqliteFile=new File(pluginDataFolder,path);
        String host=config.getString("SQL.host","localhost");
        String port=config.getString("SQL.port","3306");
        String name=config.getString("SQL.database","");
        mysqlUrl="jdbc:mysql://"+host+":"+port+"/"+name
                +"?useUnicode=true&characterEncoding=UTF-8&connectTimeout=5000&socketTimeout=30000";
        username=config.getString("SQL.username","");
        password=config.getString("SQL.password","");
    }

    public static boolean meaningful(String value) {
        return value!=null && !value.trim().isEmpty()
                && !value.equalsIgnoreCase("blank") && !value.equalsIgnoreCase("change-me");
    }

    public Backend getBackend() { return backend; }
    public File getSqliteFile() { return sqliteFile; }
    public String getMysqlUrl() { return mysqlUrl; }

    public boolean isConnected() throws SQLException {
        return legacyConnection != null && !legacyConnection.isClosed();
    }

    public void connect() throws ClassNotFoundException, SQLException {
        if (!isConnected()) legacyConnection=openConnection();
    }

    public Connection getConnection() { return legacyConnection; }

    public Connection openConnection() throws SQLException {
        if (backend==Backend.MYSQL) return openMySQL(mysqlUrl,username,password);
        return openSQLite(sqliteFile);
    }

    public static Connection openMySQL(String url,String username,String password) throws SQLException {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) { throw new SQLException("MySQL JDBC driver missing",e); }
        return DriverManager.getConnection(url,username,password);
    }

    public static Connection openSQLite(File file) throws SQLException {
        try { Class.forName("org.sqlite.JDBC"); }
        catch (ClassNotFoundException e) { throw new SQLException("SQLite JDBC driver missing",e); }
        File folder=file.getParentFile();
        if (folder!=null && !folder.isDirectory() && !folder.mkdirs())
            throw new SQLException("Cannot create SQLite data directory: "+folder);
        Connection raw=DriverManager.getConnection("jdbc:sqlite:"+file.getAbsolutePath());
        try (Statement statement=raw.createStatement()) {
            statement.execute("PRAGMA busy_timeout=15000");
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA synchronous=NORMAL");
        } catch (SQLException e) { raw.close(); throw e; }
        return wrapSQLite(raw);
    }

    /**
     * Converts SQL at the JDBC boundary so the existing 2023 mapping table
     * helpers and the durable stores cannot accidentally use different
     * databases or inconsistent dialects. MySQL connections are unwrapped.
     */
    private static Connection wrapSQLite(Connection real) {
        return (Connection)Proxy.newProxyInstance(Database.class.getClassLoader(),
                new Class<?>[]{Connection.class},(proxy,method,args)->{
                    try {
                        if ("prepareStatement".equals(method.getName())
                                && args!=null && args.length>0 && args[0] instanceof String) {
                            Object[] params=args.clone();
                            params[0]=SqlDialect.sqlite((String)params[0]);
                            return method.invoke(real,params);
                        }
                        if ("createStatement".equals(method.getName()))
                            return wrapStatement((Statement)method.invoke(real,args));
                        if ("unwrap".equals(method.getName()) && args!=null
                                && args.length==1 && args[0]==Connection.class)
                            return real;
                        return method.invoke(real,args);
                    } catch (InvocationTargetException error) { throw error.getCause(); }
                });
    }

    private static Statement wrapStatement(Statement original) {
        return (Statement)Proxy.newProxyInstance(Database.class.getClassLoader(),
                new Class<?>[]{Statement.class},(proxy,method,args)->{
                    try {
                        if (args!=null && args.length>0 && args[0] instanceof String
                                && ("execute".equals(method.getName())
                                || "executeUpdate".equals(method.getName())
                                || "executeQuery".equals(method.getName())
                                || "addBatch".equals(method.getName()))) {
                            Object[] params=args.clone();
                            params[0]=SqlDialect.sqlite((String)params[0]);
                            return method.invoke(original,params);
                        }
                        return method.invoke(original,args);
                    } catch (InvocationTargetException error) { throw error.getCause(); }
                });
    }

    /** Install SQLite indexes removed from MySQL's inline CREATE TABLE syntax. */
    public void ensureIndexes() throws SQLException {
        if (backend!=Backend.SQLITE) return;
        try (Connection connection=openConnection(); Statement statement=connection.createStatement()) {
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ready ON tdc_discord_jobs(status,available_at,lease_until)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_member_claim ON tdc_member_intents(desired,status,priority,available_at,lease_until)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_identity_name ON tdc_towny_identity(guild_id,kind,last_name)");
        }
    }

    /**
     * Older table helpers swallow SQL exceptions for backward compatibility.
     * This second validation prevents enabling Discord jobs against a
     * partially initialized schema.
     */
    public void verifyRequiredTables() throws SQLException {
        String[] required={
            "tdc_log_category","tdc_log_text_channel","tdc_players",
            "tdc_town_roles","tdc_nation_roles","tdc_town_categories",
            "tdc_nation_categories","tdc_town_text_channels",
            "tdc_nation_text_channels","tdc_town_voice_channels",
            "tdc_nation_voice_channels","tdc_discord_jobs",
            "tdc_schema_migrations","tdc_member_intents",
            "tdc_managed_member_roles","tdc_towny_identity","tdc_audit_runs"
        };
        try (Connection c=openConnection()) {
            for (String table:required) {
                try (java.sql.ResultSet rs=c.getMetaData().getTables(
                        c.getCatalog(),null,table,new String[]{"TABLE"})) {
                    if (!rs.next()) throw new SQLException(
                            "Required storage table missing: "+table
                            +". Check SQLite/MySQL schema errors and restart.");
                }
            }
        }
    }

    public void disconnect() throws SQLException {
        if (isConnected()) legacyConnection.close();
        legacyConnection=null;
    }
}
