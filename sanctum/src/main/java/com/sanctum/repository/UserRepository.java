package com.sanctum.repository;

import com.sanctum.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository class for User entity
 */
public class UserRepository {
    private static final Logger logger = Logger.getLogger(UserRepository.class.getName());
    
    public User create(User user) throws SQLException {
        String sql = "INSERT INTO users (church_id, username, password_hash, email, full_name, role, is_active)\n" +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, user.getChurchId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getFullName());
            pstmt.setString(6, user.getRole());
            pstmt.setBoolean(7, user.isActive());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
            
            conn.commit();
            logger.info("User created successfully: " + user.getUsername());
            return user;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating user", e);
            throw e;
        }
    }
    
    public User findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding user by ID: " + id, e);
            throw e;
        }
    }
    
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding user by username: " + username, e);
            throw e;
        }
    }
    
    public List<User> findByChurchId(Integer churchId) throws SQLException {
        String sql = "SELECT * FROM users WHERE church_id = ? ORDER BY full_name";
        List<User> users = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, churchId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding users by church ID: " + churchId, e);
            throw e;
        }
        
        return users;
    }
    
    public List<User> findByRole(String role) throws SQLException {
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY full_name";
        List<User> users = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, role);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding users by role: " + role, e);
            throw e;
        }
        
        return users;
    }
    
    public User update(User user) throws SQLException {
        String sql = "UPDATE users SET \n" +
            "    email = ?, \n" +
            "    full_name = ?, \n" +
            "    role = ?, \n" +
            "    is_active = ?, \n" +
            "    updated_at = CURRENT_TIMESTAMP\n" +
            "WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getFullName());
            pstmt.setString(3, user.getRole());
            pstmt.setBoolean(4, user.isActive());
            pstmt.setInt(5, user.getId());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating user failed, no rows affected.");
            }
            
            conn.commit();
            logger.info("User updated successfully: " + user.getUsername());
            return user;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating user", e);
            throw e;
        }
    }
    
    public boolean updatePassword(Integer userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, passwordHash);
            pstmt.setInt(2, userId);
            
            int affectedRows = pstmt.executeUpdate();
            conn.commit();
            
            boolean success = affectedRows > 0;
            if (success) {
                logger.info("Password updated successfully for user ID: " + userId);
            }
            return success;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating password for user ID: " + userId, e);
            throw e;
        }
    }
    
    public boolean incrementFailedAttempts(Integer userId) throws SQLException {
        String sql = "UPDATE users SET \n" +
            "    failed_login_attempts = failed_login_attempts + 1,\n" +
            "    updated_at = CURRENT_TIMESTAMP\n" +
            "WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            int affectedRows = pstmt.executeUpdate();
            conn.commit();
            
            return affectedRows > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error incrementing failed attempts for user ID: " + userId, e);
            throw e;
        }
    }
    
    public boolean resetFailedAttempts(Integer userId) throws SQLException {
        String sql = "UPDATE users SET \n" +
            "    failed_login_attempts = 0,\n" +
            "    locked_until = NULL,\n" +
            "    updated_at = CURRENT_TIMESTAMP\n" +
            "WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            int affectedRows = pstmt.executeUpdate();
            conn.commit();
            
            boolean success = affectedRows > 0;
            if (success) {
                logger.info("Failed attempts reset for user ID: " + userId);
            }
            return success;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error resetting failed attempts for user ID: " + userId, e);
            throw e;
        }
    }
    
    public boolean lockAccount(Integer userId, LocalDateTime lockUntil) throws SQLException {
        String sql = "UPDATE users SET \n" +
            "    locked_until = ?,\n" +
            "    updated_at = CURRENT_TIMESTAMP\n" +
            "WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, Timestamp.valueOf(lockUntil));
            pstmt.setInt(2, userId);
            
            int affectedRows = pstmt.executeUpdate();
            conn.commit();
            
            boolean success = affectedRows > 0;
            if (success) {
                logger.info("Account locked for user ID: " + userId + " until: " + lockUntil);
            }
            return success;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error locking account for user ID: " + userId, e);
            throw e;
        }
    }
    
    public boolean delete(Integer id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            int affectedRows = pstmt.executeUpdate();
            conn.commit();
            
            boolean success = affectedRows > 0;
            if (success) {
                logger.info("User deleted successfully: ID " + id);
            }
            return success;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting user ID: " + id, e);
            throw e;
        }
    }
    
    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY full_name";
        List<User> users = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding all users", e);
            throw e;
        }
        
        return users;
    }
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setChurchId(rs.getInt("church_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(rs.getString("role"));
        user.setActive(rs.getBoolean("is_active"));
        user.setFailedAttempts(rs.getInt("failed_login_attempts"));
        
        Timestamp lockedUntil = rs.getTimestamp("locked_until");
        if (lockedUntil != null) {
            user.setLockedUntil(lockedUntil.toLocalDateTime());
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return user;
    }
}
