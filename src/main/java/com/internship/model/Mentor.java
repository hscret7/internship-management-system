package com.internship.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Mentor: Người hướng dẫn thực tập sinh (BE3).
 * Liên kết với users có role MENTOR.
 */
public class Mentor implements Serializable {
    private static final long serialVersionUID = 1L;

    private int mentorId;
    private long userId;           // FK → users.user_id
    private Integer departmentId;  // FK → departments.department_id (nullable)
    private String specialization;
    private int maxMentees;
    private String status;         // ACTIVE, INACTIVE
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Transient display fields
    private String fullName;
    private String email;
    private String departmentName;
    private int currentMenteeCount;

    public Mentor() {
    }

    public int getMentorId() {
        return mentorId;
    }

    public void setMentorId(int mentorId) {
        this.mentorId = mentorId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public int getMaxMentees() {
        return maxMentees;
    }

    public void setMaxMentees(int maxMentees) {
        this.maxMentees = maxMentees;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public int getCurrentMenteeCount() {
        return currentMenteeCount;
    }

    public void setCurrentMenteeCount(int currentMenteeCount) {
        this.currentMenteeCount = currentMenteeCount;
    }

    @Override
    public String toString() {
        return "Mentor{" +
                "mentorId=" + mentorId +
                ", userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
