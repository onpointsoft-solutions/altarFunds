package com.sanctum.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Expense model representing church expenses
 */
public class Expense implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum ExpenseStatus {
        PENDING, APPROVED, REJECTED
    }
    
    public enum SyncStatus {
        PENDING, SYNCED
    }
    
    private Integer id;
    private Integer churchId;
    private String category;
    private String description;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String receiptPath;
    private ExpenseStatus status;
    private Integer approvedBy;
    private LocalDateTime approvedAt;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private SyncStatus syncStatus;
    
    // Transient fields for joins
    private transient String approvedByName;
    private transient String createdByName;
    
    public Expense() {
        this.status = ExpenseStatus.PENDING;
        this.syncStatus = SyncStatus.SYNCED;
    }
    
    public Expense(String category, String description, BigDecimal amount, LocalDate expenseDate) {
        this();
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.expenseDate = expenseDate;
    }
    
    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getChurchId() { return churchId; }
    public void setChurchId(Integer churchId) { this.churchId = churchId; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    
    public String getReceiptPath() { return receiptPath; }
    public void setReceiptPath(String receiptPath) { this.receiptPath = receiptPath; }
    
    public ExpenseStatus getStatus() { return status; }
    public void setStatus(ExpenseStatus status) { this.status = status; }
    
    public Integer getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Integer approvedBy) { this.approvedBy = approvedBy; }
    
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public SyncStatus getSyncStatus() { return syncStatus; }
    public void setSyncStatus(SyncStatus syncStatus) { this.syncStatus = syncStatus; }
    
    // Transient field getters/setters
    public String getApprovedByName() { return approvedByName; }
    public void setApprovedByName(String approvedByName) { this.approvedByName = approvedByName; }
    
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    
    // Utility methods
    public String getFormattedAmount() {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
    
    public boolean isPending() {
        return status == ExpenseStatus.PENDING;
    }
    
    public boolean isApproved() {
        return status == ExpenseStatus.APPROVED;
    }
    
    public boolean isRejected() {
        return status == ExpenseStatus.REJECTED;
    }
    
    public boolean hasReceipt() {
        return receiptPath != null && !receiptPath.trim().isEmpty();
    }
    
    public boolean canBeApproved() {
        return isPending();
    }
    
    public boolean canBeRejected() {
        return isPending();
    }
    
    public boolean isPendingSync() {
        return syncStatus == SyncStatus.PENDING;
    }
    
    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", category='" + category + '\'' +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", status=" + status +
                ", expenseDate=" + expenseDate +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Expense expense = (Expense) o;
        return id != null && id.equals(expense.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
