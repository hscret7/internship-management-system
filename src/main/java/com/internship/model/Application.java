package com.internship.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Application: Đơn ứng tuyển thực tập (BE3).
 * Bảng applications đã được BE2 tạo trong schema_be2.sql.
 */
public class Application implements Serializable {
    private static final long serialVersionUID = 1L;

    private int applicationId;
    private int internId;         // FK → intern_profiles.profile_id
    private String status;        // PENDING, APPROVED, REJECTED
    private Timestamp submittedAt;
    private Long reviewedBy;      // FK → users.user_id (nullable)
    private Timestamp reviewedAt;
    private String rejectReason;

    // Transient fields for display purposes (not persisted directly)
    private String internName;
    private String internEmail;
    private String reviewerName;

    public Application() {
    }

    // --- Getters & Setters ---

    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public int getInternId() {
        return internId;
    }

    public void setInternId(int internId) {
        this.internId = internId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getSubmittedAt() {
        return submittedAt;
    }

    /** UTC ISO-8601 instant for transport/HTML datetime; never a timezone-less string. */
    public String getSubmittedAtIso() {
        return submittedAt == null ? null : submittedAt.toInstant().toString();
    }

    public void setSubmittedAt(Timestamp submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    /** Null while unreviewed; otherwise UTC ISO-8601 with a trailing Z. */
    public String getReviewedAtIso() {
        return reviewedAt == null ? null : reviewedAt.toInstant().toString();
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getInternName() {
        return internName;
    }

    public void setInternName(String internName) {
        this.internName = internName;
    }

    public String getInternEmail() {
        return internEmail;
    }

    public void setInternEmail(String internEmail) {
        this.internEmail = internEmail;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    @Override
    public String toString() {
        return "Application{" +
                "applicationId=" + applicationId +
                ", internId=" + internId +
                ", status='" + status + '\'' +
                ", submittedAt=" + getSubmittedAtIso() +
                ", reviewedBy=" + reviewedBy +
                '}';
    }
}
