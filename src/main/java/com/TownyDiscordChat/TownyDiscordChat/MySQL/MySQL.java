package com.TownyDiscordChat.TownyDiscordChat.MySQL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL {

    private final String host = "BLANK";
    private final String port = "BLANK";
    private final String database = "BLANK";
    private final String username = "BLANK";
    private final String password = "BLANK";

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
