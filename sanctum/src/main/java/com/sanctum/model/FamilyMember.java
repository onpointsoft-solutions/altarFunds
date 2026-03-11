package com.sanctum.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FamilyMember model representing family members of church members
 */
public class FamilyMember implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer id;
    private Integer memberId;
    private String relationship;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String phone;
    private boolean isDependent;
    private LocalDateTime createdAt;
    
    // Transient fields for joins
    private transient String memberFullName;
    
    public FamilyMember() {
        this.isDependent = true;
    }
    
    public FamilyMember(Integer memberId, String relationship, String firstName, String lastName) {
        this();
        this.memberId = memberId;
        this.relationship = relationship;
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getMemberId() { return memberId; }
    public void setMemberId(Integer memberId) { this.memberId = memberId; }
    
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public boolean isDependent() { return isDependent; }
    public void setDependent(boolean dependent) { isDependent = dependent; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Transient field getters/setters
    public String getMemberFullName() { return memberFullName; }
    public void setMemberFullName(String memberFullName) { this.memberFullName = memberFullName; }
    
    // Utility methods
    public String getFullName() {
        return (firstName != null ? firstName : "") + 
               (lastName != null ? " " + lastName : "");
    }
    
    public String getDisplayName() {
        String name = getFullName().trim();
        return name.isEmpty() ? "Unknown Family Member" : name;
    }
    
    public int getAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }
    
    public boolean hasContactInfo() {
        return phone != null && !phone.trim().isEmpty();
    }
    
    public boolean isChild() {
        return getAge() < 18;
    }
    
    public boolean isAdult() {
        return getAge() >= 18;
    }
    
    @Override
    public String toString() {
        return "FamilyMember{" +
                "id=" + id +
                ", memberId=" + memberId +
                ", relationship='" + relationship + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", isDependent=" + isDependent +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        FamilyMember that = (FamilyMember) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
