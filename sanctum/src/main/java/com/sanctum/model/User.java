package com.sanctum.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * User model representing system users
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer id;
    private Integer churchId;
    private String username;
    private String passwordHash;
    private String email;
    private String fullName;
    private String role;
    private boolean isActive;
    private int failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public User() {}
    
    public User(String username, String email, String fullName, String role) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.isActive = true;
        this.failedLoginAttempts = 0;
    }
    
    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getChurchId() { return churchId; }
    public void setChurchId(Integer churchId) { this.churchId = churchId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public int getFailedAttempts() { return failedLoginAttempts; }
    public void setFailedAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Utility methods
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }
    
    public boolean canAttemptLogin() {
        return isActive && !isLocked();
    }
    
    public boolean isAdmin() {
        return "CHURCH_ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }
    
    public boolean isPastor() {
        return "PASTOR".equals(role);
    }
    
    public boolean isTreasurer() {
        return "TREASURER".equals(role);
    }
    
    public boolean isSecretary() {
        return "SECRETARY".equals(role);
    }
    
    public boolean isMember() {
        return "MEMBER".equals(role);
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                ", isActive=" + isActive +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        User user = (User) o;
        return id != null && id.equals(user.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
