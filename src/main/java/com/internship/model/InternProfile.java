package com.internship.model;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * InternProfile: Đại diện cho hồ sơ thực tập sinh (BE2).
 */
public class InternProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private int profileId;
    private int userId;
    private String fullName;
    private Date dob;
    private String gender;
    private String phone;
    private String email;
    private String university;
    private String major;
    private Double gpa;
    private String skills;
    private String status; // ACTIVE, INACTIVE
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public InternProfile() {
    }

    public InternProfile(int profileId, int userId, String fullName, Date dob, String gender, 
                         String phone, String email, String university, String major, 
                         Double gpa, String skills, String status, Timestamp createdAt, Timestamp updatedAt) {
        this.profileId = profileId;
        this.userId = userId;
        this.fullName = fullName;
        this.dob = dob;
        this.gender = gender;
        this.phone = phone;
        this.email = email;
        this.university = university;
        this.major = major;
        this.gpa = gpa;
        this.skills = skills;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public Double getGpa() {
        return gpa;
    }

    public void setGpa(Double gpa) {
        this.gpa = gpa;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
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

    @Override
    public String toString() {
        return "InternProfile{" +
                "profileId=" + profileId +
                ", userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", university='" + university + '\'' +
                ", major='" + major + '\'' +
                ", gpa=" + gpa +
                ", status='" + status + '\'' +
                '}';
    }
}
