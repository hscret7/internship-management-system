package com.internship.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Allowance {
    private int allowanceId;
    private int enrollmentId;
    private String allowanceType;
    private String period;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paidAt;
    private long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String internName;

    public Allowance() {
    }

    public Allowance(int allowanceId, int enrollmentId, String allowanceType, String period, BigDecimal amount, String status, LocalDateTime paidAt, long createdBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.allowanceId = allowanceId;
        this.enrollmentId = enrollmentId;
        this.allowanceType = allowanceType;
        this.period = period;
        this.amount = amount;
        this.status = status;
        this.paidAt = paidAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getAllowanceId() { return allowanceId; }
    public void setAllowanceId(int allowanceId) { this.allowanceId = allowanceId; }

    public int getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(int enrollmentId) { this.enrollmentId = enrollmentId; }

    public String getAllowanceType() { return allowanceType; }
    public void setAllowanceType(String allowanceType) { this.allowanceType = allowanceType; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public long getCreatedBy() { return createdBy; }
    public void setCreatedBy(long createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getInternName() { return internName; }
    public void setInternName(String internName) { this.internName = internName; }
}
