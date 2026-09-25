package com.internship.model;

import java.io.Serializable;

/**
 * User: Thực thể người dùng trong hệ thống (Đồng bộ với BE1).
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private int roleId;
    private String roleName; // ADMIN, HR, MENTOR, INTERN
    private String username;
    private String email;
    private String status;

    public User() {
    }

    public User(int userId, int roleId, String roleName, String username, String email, String status) {
        this.userId = userId;
        this.roleId = roleId;
        this.roleName = roleName;
        this.username = username;
        this.email = email;
        this.status = status;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isHR() {
        return "HR".equalsIgnoreCase(roleName) || "ADMIN".equalsIgnoreCase(roleName);
    }

    public boolean isIntern() {
        return "INTERN".equalsIgnoreCase(roleName);
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(roleName);
    }
}
