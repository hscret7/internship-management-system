package com.internship.model;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * ProgramEnrollment: Ghi danh TTS vào chương trình thực tập (BE3).
 * enrollment_id là khóa mà BE4 sẽ dùng cho task/report/evaluation.
 */
public class ProgramEnrollment implements Serializable {
    private static final long serialVersionUID = 1L;

    private int enrollmentId;
    private int applicationId;     // UNIQUE – mỗi Application tối đa 1 Enrollment
    private int programId;
    private int internProfileId;
    private String status;         // ACTIVE, COMPLETED, CANCELLED
    private Timestamp enrolledAt;
    private Timestamp completedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Transient display fields
    private String internName;
    private String programName;
    private String currentMentorName;
    private Integer currentMentorId;
    private String departmentName;
    private Date startDate;
    private Date endDate;

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String value) { departmentName = value; }
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date value) { startDate = value; }
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date value) { endDate = value; }

    public ProgramEnrollment() {
    }

    public int getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(int enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public int getProgramId() {
        return programId;
    }

    public void setProgramId(int programId) {
        this.programId = programId;
    }

    public int getInternProfileId() {
        return internProfileId;
    }

    public void setInternProfileId(int internProfileId) {
        this.internProfileId = internProfileId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(Timestamp enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
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

    public String getInternName() {
        return internName;
    }

    public void setInternName(String internName) {
        this.internName = internName;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getCurrentMentorName() {
        return currentMentorName;
    }

    public void setCurrentMentorName(String currentMentorName) {
        this.currentMentorName = currentMentorName;
    }

    public Integer getCurrentMentorId() {
        return currentMentorId;
    }

    public void setCurrentMentorId(Integer currentMentorId) {
        this.currentMentorId = currentMentorId;
    }

    @Override
    public String toString() {
        return "ProgramEnrollment{" +
                "enrollmentId=" + enrollmentId +
                ", applicationId=" + applicationId +
                ", programId=" + programId +
                ", internProfileId=" + internProfileId +
                ", status='" + status + '\'' +
                '}';
    }
}
