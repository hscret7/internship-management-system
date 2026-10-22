-- ========================================================
-- DATABASE SCHEMA: BE2 - HỒ SƠ, TÀI LIỆU VÀ HỢP ĐỒNG
-- Dự án: Internship Management System
-- Database: MySQL 8.0+
-- ========================================================

CREATE DATABASE IF NOT EXISTS internship_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE internship_management;

SET NAMES 'utf8mb4' COLLATE 'utf8mb4_unicode_ci';
SET CHARACTER SET utf8mb4;

-- 1. Bảng roles (Đã được tạo bởi BE1)
-- 2. Bảng users (Đã được tạo bởi BE1, user_id là BIGINT)

-- 3. Bảng intern_profiles (BE2 Quản lý: Hồ sơ thực tập sinh)
CREATE TABLE IF NOT EXISTS intern_profiles (
    profile_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    dob DATE NULL,
    gender VARCHAR(10) NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    university VARCHAR(150) NOT NULL,
    major VARCHAR(100) NOT NULL,
    gpa DECIMAL(3,2) NULL,
    skills TEXT NULL,
    status VARCHAR(30) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_full_name (full_name),
    INDEX idx_university (university),
    INDEX idx_major (major)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Bảng applications (Tham chiếu cho Hợp đồng và Đơn ứng tuyển)
CREATE TABLE IF NOT EXISTS applications (
    application_id INT AUTO_INCREMENT PRIMARY KEY,
    intern_id INT NOT NULL, -- Tham chiếu tới intern_profiles(profile_id) hoặc users(user_id)
    status VARCHAR(30) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    reject_reason TEXT NULL,
    FOREIGN KEY (intern_id) REFERENCES intern_profiles(profile_id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Bảng documents (BE2 Quản lý: CV và Đơn xin thực tập)
CREATE TABLE IF NOT EXISTS documents (
    document_id INT AUTO_INCREMENT PRIMARY KEY,
    profile_id INT NOT NULL,
    document_type VARCHAR(50) NOT NULL, -- 'CV', 'APPLICATION_LETTER', 'OTHER'
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    reject_reason TEXT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (profile_id) REFERENCES intern_profiles(profile_id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_profile_doc (profile_id, document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Bảng contracts (BE2 Quản lý: Hợp đồng thực tập)
CREATE TABLE IF NOT EXISTS contracts (
    contract_id INT AUTO_INCREMENT PRIMARY KEY,
    application_id INT NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    uploaded_by BIGINT NOT NULL, -- user_id của HR upload
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) DEFAULT 'PENDING_CONFIRMATION', -- PENDING_CONFIRMATION, CONFIRMED
    confirmed_at TIMESTAMP NULL,
    FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
