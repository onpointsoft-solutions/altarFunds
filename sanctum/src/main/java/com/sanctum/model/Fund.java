package com.sanctum.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Fund model representing donation funds/categories
 */
public class Fund implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer id;
    private Integer churchId;
    private String name;
    private String description;
    private boolean isActive;
    private LocalDateTime createdAt;
    
    public Fund() {
        this.isActive = true;
    }
    
    public Fund(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }
    
    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getChurchId() { return churchId; }
    public void setChurchId(Integer churchId) { this.churchId = churchId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Utility methods
    public String getDisplayName() {
        return name != null ? name : "Unnamed Fund";
    }
    
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "Fund{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", isActive=" + isActive +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Fund fund = (Fund) o;
        return id != null && id.equals(fund.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
