package com.internship.model;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * InternshipProgram: Chương trình thực tập (BE3).
 */
public class InternshipProgram implements Serializable {
    private static final long serialVersionUID = 1L;

    private int programId;
    private String programName;
    private int departmentId;
    private String description;
    private Date startDate;
    private Date endDate;
    private int maxInterns;
    private String status;         // PLANNED, ACTIVE, COMPLETED, CANCELLED
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Transient display field
    private String departmentName;

    public InternshipProgram() {
    }

    public int getProgramId() {
        return programId;
    }

    public void setProgramId(int programId) {
        this.programId = programId;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public int getMaxInterns() {
        return maxInterns;
    }

    public void setMaxInterns(int maxInterns) {
        this.maxInterns = maxInterns;
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

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    @Override
    public String toString() {
        return "InternshipProgram{" +
                "programId=" + programId +
                ", programName='" + programName + '\'' +
                ", departmentId=" + departmentId +
                ", status='" + status + '\'' +
                '}';
    }
}
