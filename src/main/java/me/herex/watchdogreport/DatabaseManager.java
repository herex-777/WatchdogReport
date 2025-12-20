package me.herex.watchdogreport;

import java.sql.*;
import java.util.logging.Level;

public class DatabaseManager {

    private final WatchdogReport plugin;
    private Connection connection;
    private final String databaseType;

    public DatabaseManager(WatchdogReport plugin) {
        this.plugin = plugin;
        this.databaseType = plugin.getConfigManager().getDatabaseType().toLowerCase();
    }

    public boolean connect() {
        try {
            if (databaseType.equals("mysql")) {
                return connectMySQL();
            } else {
                return connectSQLite();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Database connection failed", e);
            return false;
        }
    }

    private boolean connectMySQL() throws SQLException, ClassNotFoundException {
        String host = plugin.getConfigManager().getMySQLHost();
        int port = plugin.getConfigManager().getMySQLPort();
        String database = plugin.getConfigManager().getMySQLDatabase();
        String username = plugin.getConfigManager().getMySQLUsername();
        String password = plugin.getConfigManager().getMySQLPassword();
        boolean useSSL = plugin.getConfigManager().getMySQLUseSSL();
        boolean allowPublicKey = plugin.getConfigManager().getMySQLAllowPublicKey();
        String timezone = plugin.getConfigManager().getMySQLTimezone();

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=" + useSSL +
                "&allowPublicKeyRetrieval=" + allowPublicKey +
                "&serverTimezone=" + timezone;

        plugin.getLogger().info("Connecting to MySQL: " + host + ":" + port + "/" + database);

        try {
            // Try MySQL 8 driver first, then fall back to older driver
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e1) {
                // Try older driver
                Class.forName("com.mysql.jdbc.Driver");
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("MySQL Driver not found! Add mysql-connector-java to your dependencies.");
            throw e;
        }

        connection = DriverManager.getConnection(url, username, password);

        if (connection != null && !connection.isClosed()) {
            plugin.getLogger().info("Successfully connected to MySQL database!");
            createTables();
            return true;
        }

        return false;
    }

    private boolean connectSQLite() throws SQLException, ClassNotFoundException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite Driver not found! Add sqlite-jdbc to your dependencies.");
            throw e;
        }

        String filename = plugin.getConfigManager().getSQLiteFilename();
        String path = plugin.getDataFolder().getAbsolutePath() + "/" + filename;

        // Create data folder if it doesn't exist
        plugin.getDataFolder().mkdirs();

        plugin.getLogger().info("SQLite database path: " + path);

        String url = "jdbc:sqlite:" + path;
        connection = DriverManager.getConnection(url);

        if (connection != null && !connection.isClosed()) {
            plugin.getLogger().info("Successfully connected to SQLite database!");
            createTables();
            return true;
        }

        return false;
    }

    private void createTables() {
        String autoIncrement = databaseType.equals("mysql") ? "AUTO_INCREMENT" : "AUTOINCREMENT";

        try (Statement stmt = connection.createStatement()) {
            // Create reports table
            String reportsTable = "CREATE TABLE IF NOT EXISTS reports (" +
                    "id INTEGER PRIMARY KEY " + autoIncrement + ", " +
                    "reporter VARCHAR(16) NOT NULL, " +
                    "reported VARCHAR(16) NOT NULL, " +
                    "reason TEXT NOT NULL, " +
                    "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "status VARCHAR(20) DEFAULT 'pending'" +
                    ")";
            stmt.execute(reportsTable);
            plugin.getLogger().info("Reports table created/verified.");

            // Create punishments table
            String punishmentsTable = "CREATE TABLE IF NOT EXISTS punishments (" +
                    "id INTEGER PRIMARY KEY " + autoIncrement + ", " +
                    "player_name VARCHAR(16) NOT NULL, " +
                    "player_uuid VARCHAR(36), " +
                    "player_ip VARCHAR(45), " +
                    "punishment_type VARCHAR(20) NOT NULL, " +
                    "reason TEXT NOT NULL, " +
                    "operator VARCHAR(16) NOT NULL, " +
                    "duration BIGINT, " +
                    "start_time BIGINT NOT NULL, " +
                    "end_time BIGINT, " +
                    "active BOOLEAN DEFAULT TRUE, " +
                    "silent BOOLEAN DEFAULT FALSE" +
                    ")";
            stmt.execute(punishmentsTable);
            plugin.getLogger().info("Punishments table created/verified.");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create database tables", e);
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error closing database connection", e);
            }
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public String getDatabaseType() {
        return databaseType;
    }
}