package com.TownyDiscordChat.TownyDiscordChat.MySQL;

import com.TownyDiscordChat.TownyDiscordChat.Main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL {

    private final String host = Main.plugin.config.getString("SQL.host");
    private final String port = Main.plugin.config.getString("SQL.port");
    private final String database = Main.plugin.config.getString("SQL.database");
    private final String username = Main.plugin.config.getString("SQL.username");
    private final String password = Main.plugin.config.getString("SQL.password");

    private Connection connection;

    public boolean isConnected() throws SQLException {
        return (connection != null && !connection.isClosed());
    }

    public void connect() throws ClassNotFoundException, SQLException {
        if (!isConnected()) {
            connection = DriverManager.getConnection("jdbc:mysql://" +
                            host + ":" + port + "/" + database, //+ "?useSSL=true",
                    username, password);
        }
    }

    public void disconnect() throws SQLException {
        if (isConnected()) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void checkSQLConnection() throws SQLException, ClassNotFoundException {
        if (!isConnected()){
            connect();
        }
    }
}
