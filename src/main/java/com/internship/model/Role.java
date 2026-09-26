package com.internship.model;

import java.time.LocalDateTime;

public class Role {

    private long roleId;
    private String roleName;
    private String description;
    private LocalDateTime createdAt;

    public Role() {
    }

    public Role(long roleId, String roleName,
                String description, LocalDateTime createdAt) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.description = description;
        this.createdAt = createdAt;
    }

    public long getRoleId() {
        return roleId;
    }

    public void setRoleId(long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Role{" +
                "roleId=" + roleId +
                ", roleName='" + roleName + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}