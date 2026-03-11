package com.sanctum.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Donation model representing financial donations
 */
public class Donation implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum PaymentMethod {
        CASH, CHECK, BANK_TRANSFER, MOBILE_MONEY
    }
    
    public enum SyncStatus {
        PENDING, SYNCED
    }
    
    private Integer id;
    private Integer churchId;
    private Integer memberId;
    private Integer fundId;
    private BigDecimal amount;
    private LocalDate donationDate;
    private String receiptNumber;
    private PaymentMethod paymentMethod;
    private String notes;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private SyncStatus syncStatus;
    
    // Transient fields for joins
    private transient String memberName;
    private transient String fundName;
    
    public Donation() {
        this.paymentMethod = PaymentMethod.CASH;
        this.syncStatus = SyncStatus.SYNCED;
    }
    
    public Donation(BigDecimal amount, Integer fundId, LocalDate donationDate) {
        this();
        this.amount = amount;
        this.fundId = fundId;
        this.donationDate = donationDate;
    }
    
    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getChurchId() { return churchId; }
    public void setChurchId(Integer churchId) { this.churchId = churchId; }
    
    public Integer getMemberId() { return memberId; }
    public void setMemberId(Integer memberId) { this.memberId = memberId; }
    
    public Integer getFundId() { return fundId; }
    public void setFundId(Integer fundId) { this.fundId = fundId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public LocalDate getDonationDate() { return donationDate; }
    public void setDonationDate(LocalDate donationDate) { this.donationDate = donationDate; }
    
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public SyncStatus getSyncStatus() { return syncStatus; }
    public void setSyncStatus(SyncStatus syncStatus) { this.syncStatus = syncStatus; }
    
    // Transient field getters/setters
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    
    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }
    
    // Utility methods
    public String getPaymentMethodDisplay() {
        if (paymentMethod == null) return "Unknown";
        return paymentMethod.toString().replace("_", " ");
    }
    
    public String getFormattedAmount() {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
    
    public boolean isPendingSync() {
        return syncStatus == SyncStatus.PENDING;
    }
    
    public boolean hasMember() {
        return memberId != null && memberId > 0;
    }
    
    @Override
    public String toString() {
        return "Donation{" +
                "id=" + id +
                ", amount=" + amount +
                ", donationDate=" + donationDate +
                ", receiptNumber='" + receiptNumber + '\'' +
                ", fundName='" + fundName + '\'' +
                ", memberName='" + memberName + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Donation donation = (Donation) o;
        return id != null && id.equals(donation.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
