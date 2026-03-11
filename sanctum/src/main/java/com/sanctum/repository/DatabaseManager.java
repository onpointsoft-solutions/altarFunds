package com.sanctum.repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database connection manager for SQLite
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_URL = "jdbc:sqlite:data/sanctum.db";
    private static Connection connection;
    
    static {
        try {
            // Ensure data directory exists
            Path dataDir = Paths.get("data");
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            
            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException | IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database driver", e);
        }
    }
    
    /**
     * Get database connection
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(false);
            enableForeignKeys(connection);
        }
        return connection;
    }
    
    /**
     * Close database connection
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error closing database connection", e);
        }
    }
    
    /**
     * Enable foreign key constraints
     */
    private static void enableForeignKeys(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
    }
    
    /**
     * Execute update with prepared statement
     */
    public static int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            
            return pstmt.executeUpdate();
        }
    }
    
    /**
     * Execute query with prepared statement
     */
    public static ResultSet executeQuery(String sql, Object... params) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        
        for (int i = 0; i < params.length; i++) {
            pstmt.setObject(i + 1, params[i]);
        }
        
        return pstmt.executeQuery();
    }
    
    /**
     * Execute single value query
     */
    public static <T> T executeSingleValueQuery(String sql, Class<T> type, Object... params) throws SQLException {
        try (ResultSet rs = executeQuery(sql, params)) {
            if (rs.next()) {
                if (type == Integer.class) {
                    return type.cast(rs.getInt(1));
                } else if (type == Long.class) {
                    return type.cast(rs.getLong(1));
                } else if (type == String.class) {
                    return type.cast(rs.getString(1));
                } else if (type == Double.class) {
                    return type.cast(rs.getDouble(1));
                } else if (type == Boolean.class) {
                    return type.cast(rs.getBoolean(1));
                }
            }
            return null;
        }
    }
    
    /**
     * Check if record exists
     */
    public static boolean exists(String tableName, String idColumn, Object id) throws SQLException {
        String sql = "SELECT 1 FROM " + tableName + " WHERE " + idColumn + " = ?";
        try (ResultSet rs = executeQuery(sql, id)) {
            return rs.next();
        }
    }
    
    /**
     * Get next ID from sequence
     */
    public static int getNextId(String tableName) throws SQLException {
        String sql = "SELECT MAX(id) FROM " + tableName;
        Integer maxId = executeSingleValueQuery(sql, Integer.class);
        return (maxId == null) ? 1 : maxId + 1;
    }
    
    /**
     * Backup database
     */
    public static boolean backupDatabase(String backupPath) {
        try {
            File sourceFile = new File("data/sanctum.db");
            File backupFile = new File(backupPath);
            
            if (!sourceFile.exists()) {
                LOGGER.warning("Source database file does not exist");
                return false;
            }
            
            Files.copy(sourceFile.toPath(), backupFile.toPath());
            LOGGER.info("Database backed up to: " + backupPath);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to backup database", e);
            return false;
        }
    }
    
    /**
     * Restore database
     */
    public static boolean restoreDatabase(String backupPath) {
        try {
            File backupFile = new File(backupPath);
            File targetFile = new File("data/sanctum.db");
            
            if (!backupFile.exists()) {
                LOGGER.warning("Backup file does not exist: " + backupPath);
                return false;
            }
            
            closeConnection();
            Files.copy(backupFile.toPath(), targetFile.toPath());
            LOGGER.info("Database restored from: " + backupPath);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to restore database", e);
            return false;
        }
    }
    
    /**
     * Get database file size
     */
    public static long getDatabaseSize() {
        File dbFile = new File("data/sanctum.db");
        return dbFile.exists() ? dbFile.length() : 0;
    }
    
    /**
     * Vacuum database to optimize size
     */
    public static void vacuumDatabase() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("VACUUM");
            LOGGER.info("Database vacuum completed");
        }
    }
}
