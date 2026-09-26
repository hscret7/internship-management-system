CREATE DATABASE IF NOT EXISTS internship_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE internship_management;

CREATE TABLE IF NOT EXISTS roles (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role_id BIGINT NOT NULL,

    status ENUM(
        'ACTIVE',
        'INACTIVE',
        'LOCKED'
    ) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id)
        REFERENCES roles(role_id)
);

INSERT INTO roles (role_name, description)
VALUES
    ('ADMIN', 'Quản trị hệ thống'),
    ('HR', 'Nhân sự'),
    ('MENTOR', 'Người hướng dẫn thực tập sinh'),
    ('INTERN', 'Thực tập sinh')
ON DUPLICATE KEY UPDATE
    description = VALUES(description);