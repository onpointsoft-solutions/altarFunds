package com.sanctum.config;

import com.sanctum.repository.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database configuration and initialization
 */
public class DatabaseConfig {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());
    
    public static void initializeDatabase() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create all tables
            createChurchTable(stmt);
            createUserTable(stmt);
            createMemberTable(stmt);
            createFamilyMemberTable(stmt);
            createFundTable(stmt);
            createDonationTable(stmt);
            createExpenseTable(stmt);
            createEventTable(stmt);
            createAttendanceTable(stmt);
            
            LOGGER.info("All database tables created/verified successfully");
        }
    }
    
    private static void createChurchTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS churches (\n" +
            "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "    name TEXT NOT NULL,\n" +
            "    address TEXT,\n" +
            "    phone TEXT,\n" +
            "    email TEXT,\n" +
            "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
            "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
            ")";
        stmt.execute(sql);
    }
    
    private static void createUserTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users (\n" +
            "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "    church_id INTEGER NOT NULL,\n" +
            "    username TEXT UNIQUE NOT NULL,\n" +
            "    password_hash TEXT NOT NULL,\n" +
            "    email TEXT,\n" +
            "    full_name TEXT NOT NULL,\n" +
            "    role TEXT NOT NULL CHECK (role IN ('SUPER_ADMIN', 'CHURCH_ADMIN', 'PASTOR', 'TREASURER', 'SECRETARY', 'MEMBER')),\n" +
            "    is_active BOOLEAN DEFAULT 1,\n" +
            "    failed_login_attempts INTEGER DEFAULT 0,\n" +
            "    locked_until TIMESTAMP,\n" +
            "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
            "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
            "    FOREIGN KEY (church_id) REFERENCES churches(id)\n" +
            ")";
        stmt.execute(sql);
    }
    
    private static void createMemberTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS members (\n" +
            "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "    church_id INTEGER NOT NULL,\n" +
            "    member_number TEXT UNIQUE,\n" +
            "    first_name TEXT NOT NULL,\n" +
            "    last_name TEXT NOT NULL,\n" +
            "    date_of_birth DATE,\n" +
            "    phone TEXT,\n" +
            "    email TEXT,\n" +
            "    address TEXT,\n" +
            "    membership_date DATE,\n" +
            "    status TEXT DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'DECEASED')),\n" +
            "    is_deleted BOOLEAN DEFAULT 0,\n" +
            "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
            "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
            "    sync_status TEXT DEFAULT 'SYNCED' CHECK (sync_status IN ('PENDING', 'SYNCED')),\n" +
            "    FOREIGN KEY (church_id) REFERENCES churches(id)\n" +
            ")";
        stmt.execute(sql);
    }
    
    private static void createFamilyMemberTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS family_members (\n" +
            "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "    member_id INTEGER NOT NULL,\n" +
            "    relationship TEXT NOT NULL,\n" +
            "    first_name TEXT NOT NULL,\n" +
            "    last_name TEXT NOT NULL,\n" +
            "    date_of_birth DATE,\n" +
            "    phone TEXT,\n" +
            "    is_dependent BOOLEAN DEFAULT 1,\n" +
            "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
            "    FOREIGN KEY (member_id) REFERENCES members(id)\n" +
            ")";
        stmt.execute(sql);
    }
    
    private static void createFundTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS funds (\n" +
            "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "    church_id INTEGER NOT NULL,\n" +
            "    name TEXT NOT NULL,\n" +
            "    description TEXT,\n" +
            "    is_active BOOLEAN DEFAULT 1,\n" +
            "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
            "    FOREIGN KEY (church_id) REFERENCES churches(id)\n" +
            ")";
        stmt.execute(sql);
    }
    
    private static void createDonationTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS donations (\n" +
            "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "    church_id INTEGER NOT NULL,\n" +
            "    member_id INTEGER,\n" +
            "    fund_id INTEGER NOT NULL,\n" +
            "    amount DECIMAL(10,2) NOT NULL,\n" +
            "    donation_date DATE NOT NULL,\n" +
            "    receipt_number TEXT UNIQUE,\n" +
            "    payment_method TEXT DEFAULT 'CASH' CHECK (payment_method IN ('CASH', 'CHECK', 'BANK_TRANSFER', 'MOBILE_MONEY')),\n" +
            "    notes TEXT,\n" +
            "    created_by INTEGER,\n" +
            "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
            "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
            "    sync_status TEXT DEFAULT 'SYNCED' CHECK (sync_status IN ('PENDING', 'SYNCED')),\n" +
            "    FOREIGN KEY (church_id) REFERENCES churches(id),\n" +
            "    FOREIGN KEY (member_id) REFERENCES members(id),\n" +
            "    FOREIGN KEY (fund_id) REFERENCES funds(id),\n" +
            "    FOREIGN KEY (created_by) REFERENCES users(id)\n" +
            ")";
        stmt.execute(sql);
    }
    
    private static void createExpenseTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS expenses (\n" +
            "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "    church_id INTEGER NOT NULL,\n" +
            "    category TEXT NOT NULL,\n" +
            "    description TEXT NOT NULL,\n" +
            "    amount DECIMAL(10,2) NOT NULL,\n" +
            "    expense_date DATE NOT NULL,\n" +
            "    receipt_path TEXT,\n" +
            "    status TEXT DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),\n" +
            "    approved_by INTEGER,\n" +
            "    approved_at TIMESTAMP,\n" +
            "    created_by INTEGER,\n" +
            "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
            "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
            "    sync_status TEXT DEFAULT 'SYNCED' CHECK (sync_status IN ('PENDING', 'SYNCED')),\n" +
            "    FOREIGN KEY (church_id) REFERENCES churches(id),\n" +
            "    FOREIGN KEY (approved_by) REFERENCES users(id),\n" +
            "    FOREIGN KEY (created_by) REFERENCES users(id)\n" +
            ")";
        stmt.execute(sql);
    }
    
    private static void createEventTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS events (\n" +
            "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "    church_id INTEGER NOT NULL,\n" +
            "    title TEXT NOT NULL,\n" +
            "    description TEXT,\n" +
            "    event_date DATE NOT NULL,\n" +
            "    start_time TIME,\n" +
            "    end_time TIME,\n" +
            "    location TEXT,\n" +
            "    is_recurring BOOLEAN DEFAULT 0,\n" +
            "    recurrence_pattern TEXT,\n" +
            "    created_by INTEGER,\n" +
            "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
            "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
            "    FOREIGN KEY (church_id) REFERENCES churches(id),\n" +
            "    FOREIGN KEY (created_by) REFERENCES users(id)\n" +
            ")";
        stmt.execute(sql);
    }
    
    private static void createAttendanceTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS attendance (\n" +
            "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "    event_id INTEGER NOT NULL,\n" +
            "    member_id INTEGER NOT NULL,\n" +
            "    attended BOOLEAN DEFAULT 0,\n" +
            "    notes TEXT,\n" +
            "    recorded_by INTEGER,\n" +
            "    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
            "    FOREIGN KEY (event_id) REFERENCES events(id),\n" +
            "    FOREIGN KEY (member_id) REFERENCES members(id),\n" +
            "    FOREIGN KEY (recorded_by) REFERENCES users(id),\n" +
            "    UNIQUE(event_id, member_id)\n" +
            ")";
        stmt.execute(sql);
    }
}
