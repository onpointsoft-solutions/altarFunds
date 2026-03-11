package com.sanctum.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Member model representing church members
 */
public class Member implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MemberStatus {
        ACTIVE, INACTIVE, DECEASED
    }
    
    public enum SyncStatus {
        PENDING, SYNCED
    }
    
    private Integer id;
    private Integer churchId;
    private String memberNumber;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String phone;
    private String email;
    private String address;
    private LocalDate membershipDate;
    private MemberStatus status;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private SyncStatus syncStatus;
    
    public Member() {
        this.status = MemberStatus.ACTIVE;
        this.isDeleted = false;
        this.syncStatus = SyncStatus.SYNCED;
    }
    
    public Member(String firstName, String lastName, String phone, String email) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
    }
    
    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getChurchId() { return churchId; }
    public void setChurchId(Integer churchId) { this.churchId = churchId; }
    
    public String getMemberNumber() { return memberNumber; }
    public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public LocalDate getMembershipDate() { return membershipDate; }
    public void setMembershipDate(LocalDate membershipDate) { this.membershipDate = membershipDate; }
    
    public MemberStatus getStatus() { return status; }
    public void setStatus(MemberStatus status) { this.status = status; }
    
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public SyncStatus getSyncStatus() { return syncStatus; }
    public void setSyncStatus(SyncStatus syncStatus) { this.syncStatus = syncStatus; }
    
    // Utility methods
    public String getFullName() {
        return (firstName != null ? firstName : "") + 
               (lastName != null ? " " + lastName : "");
    }
    
    public String getDisplayName() {
        String name = getFullName().trim();
        return name.isEmpty() ? "Unknown Member" : name;
    }
    
    public boolean isActive() {
        return status == MemberStatus.ACTIVE && !isDeleted;
    }
    
    public int getAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }
    
    public boolean hasContactInfo() {
        return (phone != null && !phone.trim().isEmpty()) || 
               (email != null && !email.trim().isEmpty());
    }
    
    public long getYearsAsMember() {
        if (membershipDate == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.YEARS.between(membershipDate, LocalDate.now());
    }
    
    @Override
    public String toString() {
        return "Member{" +
                "id=" + id +
                ", memberNumber='" + memberNumber + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", status=" + status +
                ", isActive=" + isActive() +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Member member = (Member) o;
        return id != null && id.equals(member.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
