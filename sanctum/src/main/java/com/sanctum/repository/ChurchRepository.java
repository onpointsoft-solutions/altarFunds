package com.sanctum.repository;

import com.sanctum.model.Church;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository class for Church entity
 */
public class ChurchRepository {
    private static final Logger logger = Logger.getLogger(ChurchRepository.class.getName());
    
    public Church create(Church church) throws SQLException {
        String sql = "INSERT INTO churches (name, address, phone, email) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, church.getName());
            pstmt.setString(2, church.getAddress());
            pstmt.setString(3, church.getPhone());
            pstmt.setString(4, church.getEmail());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating church failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    church.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating church failed, no ID obtained.");
                }
            }
            
            conn.commit();
            logger.info("Church created successfully: " + church.getName());
            return church;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating church", e);
            throw e;
        }
    }
    
    public Church findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM churches WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToChurch(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding church by ID: " + id, e);
            throw e;
        }
    }
    
    public List<Church> findAll() throws SQLException {
        String sql = "SELECT * FROM churches ORDER BY name";
        List<Church> churches = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                churches.add(mapResultSetToChurch(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding all churches", e);
            throw e;
        }
        
        return churches;
    }
    
    public Church update(Church church) throws SQLException {
        String sql = "UPDATE churches SET name = ?, address = ?, phone = ?, email = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, church.getName());
            pstmt.setString(2, church.getAddress());
            pstmt.setString(3, church.getPhone());
            pstmt.setString(4, church.getEmail());
            pstmt.setInt(5, church.getId());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating church failed, no rows affected.");
            }
            
            conn.commit();
            logger.info("Church updated successfully: " + church.getName());
            return church;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating church", e);
            throw e;
        }
    }
    
    public boolean delete(Integer id) throws SQLException {
        String sql = "DELETE FROM churches WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            int affectedRows = pstmt.executeUpdate();
            conn.commit();
            
            boolean success = affectedRows > 0;
            if (success) {
                logger.info("Church deleted successfully: ID " + id);
            }
            return success;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting church ID: " + id, e);
            throw e;
        }
    }
    
    public Church findByName(String name) throws SQLException {
        String sql = "SELECT * FROM churches WHERE name = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToChurch(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding church by name: " + name, e);
            throw e;
        }
    }
    
    public boolean exists(Integer id) throws SQLException {
        return DatabaseManager.exists("churches", "id", id);
    }
    
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM churches";
        return DatabaseManager.executeSingleValueQuery(sql, Integer.class);
    }
    
    private Church mapResultSetToChurch(ResultSet rs) throws SQLException {
        Church church = new Church();
        church.setId(rs.getInt("id"));
        church.setName(rs.getString("name"));
        church.setAddress(rs.getString("address"));
        church.setPhone(rs.getString("phone"));
        church.setEmail(rs.getString("email"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            church.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            church.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return church;
    }
}
