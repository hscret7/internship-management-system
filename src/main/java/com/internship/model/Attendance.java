package com.internship.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Attendance {
    private int attendanceId;
    private int enrollmentId;
    private LocalDate workDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String notes;
    private LocalDateTime createdAt;
    
    // Virtual fields for report
    private String internName;
    private String programName;

    // getters, setters, constructors
    public Attendance() {}

    public Attendance(int attendanceId, int enrollmentId, LocalDate workDate, LocalDateTime checkInTime, LocalDateTime checkOutTime, String notes, LocalDateTime createdAt) {
        this.attendanceId = attendanceId;
        this.enrollmentId = enrollmentId;
        this.workDate = workDate;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public int getAttendanceId() { return attendanceId; }
    public void setAttendanceId(int attendanceId) { this.attendanceId = attendanceId; }
    public int getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(int enrollmentId) { this.enrollmentId = enrollmentId; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getInternName() { return internName; }
    public void setInternName(String internName) { this.internName = internName; }
    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }
}
