-- ========================================================
-- DATABASE SCHEMA: BE3 - APPLICATION REVIEW, ENROLLMENT, MENTOR
-- Dự án: Internship Management System
-- Database: MySQL 8.0+
-- Chạy database/schema.sql → sql/schema_be2.sql trước file này.
-- File này KHÔNG chỉnh sửa bảng applications (thuộc BE2).
-- ========================================================

USE internship_management;

-- 1. Bảng departments (Phòng ban)
CREATE TABLE IF NOT EXISTS departments (
    department_id INT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(150) NOT NULL UNIQUE,
    description TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bảng internship_programs (Chương trình thực tập)
CREATE TABLE IF NOT EXISTS internship_programs (
    program_id INT AUTO_INCREMENT PRIMARY KEY,
    program_name VARCHAR(200) NOT NULL,
    department_id INT NOT NULL,
    description TEXT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    max_interns INT NOT NULL DEFAULT 0,     -- 0 = không giới hạn
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',  -- PLANNED, ACTIVE, COMPLETED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(department_id),
    INDEX idx_program_status (status),
    INDEX idx_program_dept (department_id),
    CONSTRAINT chk_program_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_program_capacity CHECK (max_interns >= 0),
    CONSTRAINT chk_program_status CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bảng mentors (Mentor - liên kết user có role MENTOR)
CREATE TABLE IF NOT EXISTS mentors (
    mentor_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    department_id INT NULL,
    specialization VARCHAR(200) NULL,
    max_mentees INT NOT NULL DEFAULT 5,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (department_id) REFERENCES departments(department_id),
    INDEX idx_mentor_status (status),
    CONSTRAINT chk_mentor_capacity CHECK (max_mentees > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Bảng program_enrollments (Ghi danh TTS vào chương trình)
--    Mỗi Application tối đa 1 Enrollment: UNIQUE(application_id)
--    Mỗi TTS tối đa 1 Enrollment ACTIVE: unique generated column + transaction
CREATE TABLE IF NOT EXISTS program_enrollments (
    enrollment_id INT AUTO_INCREMENT PRIMARY KEY,
    application_id INT NOT NULL UNIQUE,
    program_id INT NOT NULL,
    intern_profile_id INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, COMPLETED, CANCELLED
    active_intern_profile_id INT GENERATED ALWAYS AS
        (CASE WHEN status = 'ACTIVE' THEN intern_profile_id ELSE NULL END) STORED,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES applications(application_id),
    FOREIGN KEY (program_id) REFERENCES internship_programs(program_id),
    FOREIGN KEY (intern_profile_id) REFERENCES intern_profiles(profile_id),
    INDEX idx_enrollment_status (status),
    INDEX idx_enrollment_intern (intern_profile_id),
    INDEX idx_enrollment_program (program_id),
    UNIQUE KEY uq_one_active_enrollment_per_intern (active_intern_profile_id),
    CONSTRAINT chk_enrollment_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Bảng mentor_assignments (Phân mentor cho enrollment)
--    Một Enrollment tối đa 1 MentorAssignment chưa kết thúc (ended_at IS NULL):
--    unique generated column + transaction trước khi insert.
CREATE TABLE IF NOT EXISTS mentor_assignments (
    assignment_id INT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id INT NOT NULL,
    mentor_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT NOT NULL,   -- user_id của HR thực hiện gán
    ended_at TIMESTAMP NULL,       -- NULL = đang hiệu lực
    active_enrollment_id INT GENERATED ALWAYS AS
        (CASE WHEN ended_at IS NULL THEN enrollment_id ELSE NULL END) STORED,
    end_reason VARCHAR(200) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (enrollment_id) REFERENCES program_enrollments(enrollment_id),
    FOREIGN KEY (mentor_id) REFERENCES mentors(mentor_id),
    FOREIGN KEY (assigned_by) REFERENCES users(user_id),
    INDEX idx_ma_enrollment (enrollment_id),
    INDEX idx_ma_mentor (mentor_id),
    INDEX idx_ma_active (enrollment_id, ended_at),
    UNIQUE KEY uq_one_current_mentor_per_enrollment (active_enrollment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
