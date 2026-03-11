package com.sanctum.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Event model representing church events
 */
public class Event implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum RecurrencePattern {
        NONE, DAILY, WEEKLY, MONTHLY, YEARLY
    }
    
    private Integer id;
    private Integer churchId;
    private String title;
    private String description;
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private boolean isRecurring;
    private RecurrencePattern recurrencePattern;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Transient fields for joins
    private transient String createdByName;
    private transient Integer attendeeCount;
    
    public Event() {
        this.isRecurring = false;
        this.recurrencePattern = RecurrencePattern.NONE;
    }
    
    public Event(String title, String description, LocalDate eventDate, LocalTime startTime) {
        this();
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.startTime = startTime;
    }
    
    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getChurchId() { return churchId; }
    public void setChurchId(Integer churchId) { this.churchId = churchId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public boolean isRecurring() { return isRecurring; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }
    
    public RecurrencePattern getRecurrencePattern() { return recurrencePattern; }
    public void setRecurrencePattern(RecurrencePattern recurrencePattern) { this.recurrencePattern = recurrencePattern; }
    
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Transient field getters/setters
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    
    public Integer getAttendeeCount() { return attendeeCount; }
    public void setAttendeeCount(Integer attendeeCount) { this.attendeeCount = attendeeCount; }
    
    // Utility methods
    public String getDisplayName() {
        return title != null ? title : "Untitled Event";
    }
    
    public boolean hasTimeRange() {
        return startTime != null && endTime != null;
    }
    
    public String getTimeRange() {
        if (startTime == null) return "No time set";
        if (endTime == null) return startTime.toString();
        return startTime + " - " + endTime;
    }
    
    public boolean isToday() {
        return eventDate != null && eventDate.equals(LocalDate.now());
    }
    
    public boolean isUpcoming() {
        return eventDate != null && eventDate.isAfter(LocalDate.now());
    }
    
    public boolean isPast() {
        return eventDate != null && eventDate.isBefore(LocalDate.now());
    }
    
    public boolean hasLocation() {
        return location != null && !location.trim().isEmpty();
    }
    
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", eventDate=" + eventDate +
                ", startTime=" + startTime +
                ", location='" + location + '\'' +
                ", isRecurring=" + isRecurring +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Event event = (Event) o;
        return id != null && id.equals(event.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
