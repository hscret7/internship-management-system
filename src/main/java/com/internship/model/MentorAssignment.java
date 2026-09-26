package com.internship.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * MentorAssignment: Phân mentor cho enrollment (BE3).
 * Đổi mentor = kết thúc bản ghi cũ (ended_at) + tạo bản ghi mới.
 * Một Enrollment tối đa 1 MentorAssignment chưa kết thúc.
 */
public class MentorAssignment implements Serializable {
    private static final long serialVersionUID = 1L;

    private int assignmentId;
    private int enrollmentId;
    private int mentorId;
    private Timestamp assignedAt;
    private long assignedBy;       // user_id của HR thực hiện gán
    private Timestamp endedAt;     // NULL = đang hiệu lực
    private String endReason;
    private Timestamp createdAt;

    // Transient display fields
    private String mentorName;
    private String assignedByName;

    public MentorAssignment() {
    }

    public int getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(int assignmentId) {
        this.assignmentId = assignmentId;
    }

    public int getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(int enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public int getMentorId() {
        return mentorId;
    }

    public void setMentorId(int mentorId) {
        this.mentorId = mentorId;
    }

    public Timestamp getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Timestamp assignedAt) {
        this.assignedAt = assignedAt;
    }

    public long getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(long assignedBy) {
        this.assignedBy = assignedBy;
    }

    public Timestamp getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Timestamp endedAt) {
        this.endedAt = endedAt;
    }

    public String getEndReason() {
        return endReason;
    }

    public void setEndReason(String endReason) {
        this.endReason = endReason;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getMentorName() {
        return mentorName;
    }

    public void setMentorName(String mentorName) {
        this.mentorName = mentorName;
    }

    public String getAssignedByName() {
        return assignedByName;
    }

    public void setAssignedByName(String assignedByName) {
        this.assignedByName = assignedByName;
    }

    public boolean isActive() {
        return endedAt == null;
    }

    @Override
    public String toString() {
        return "MentorAssignment{" +
                "assignmentId=" + assignmentId +
                ", enrollmentId=" + enrollmentId +
                ", mentorId=" + mentorId +
                ", active=" + isActive() +
                '}';
    }
}
