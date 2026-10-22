-- BE2 Sprint 3: Attendance, Allowance, Support Requests

USE internship_management;

-- 1. Attendances (US21, US22)
CREATE TABLE IF NOT EXISTS attendances (
    attendance_id INT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id INT NOT NULL,
    work_date DATE NOT NULL,
    check_in_time DATETIME NOT NULL,
    check_out_time DATETIME NULL,
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_attendance_day (enrollment_id, work_date),
    FOREIGN KEY (enrollment_id) REFERENCES program_enrollments(enrollment_id) ON DELETE CASCADE,
    INDEX idx_attendance_enrollment (enrollment_id, work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Allowances (US25, US26)
CREATE TABLE IF NOT EXISTS allowances (
    allowance_id INT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id INT NOT NULL,
    allowance_type VARCHAR(50) NOT NULL,
    period VARCHAR(20) NOT NULL, 
    amount DECIMAL(15, 2) NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',
    paid_at DATETIME NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (enrollment_id) REFERENCES program_enrollments(enrollment_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(user_id),
    UNIQUE KEY uk_allowance (enrollment_id, allowance_type, period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Support Requests (US27, US28)
CREATE TABLE IF NOT EXISTS support_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    profile_id INT NOT NULL,
    request_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',
    response TEXT NULL,
    handled_by BIGINT NULL,
    handled_at DATETIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (profile_id) REFERENCES intern_profiles(profile_id) ON DELETE CASCADE,
    FOREIGN KEY (handled_by) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
