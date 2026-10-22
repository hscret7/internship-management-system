-- BE3 Sprint 3 / US23-US24. Additive migration; no existing data is removed.
-- Run after database/schema.sql, sql/schema_be2.sql, sql/schema_be3.sql.
USE internship_management;

CREATE TABLE IF NOT EXISTS work_schedules (
    schedule_id INT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id INT NOT NULL,
    work_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    note VARCHAR(500) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ws_enrollment FOREIGN KEY (enrollment_id) REFERENCES program_enrollments(enrollment_id),
    CONSTRAINT fk_ws_creator FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_ws_updater FOREIGN KEY (updated_by) REFERENCES users(user_id),
    CONSTRAINT chk_ws_times CHECK (start_time >= '00:00:00' AND end_time <= '23:59:59' AND start_time < end_time),
    CONSTRAINT chk_ws_status CHECK (status IN ('ACTIVE', 'CANCELLED')),
    INDEX idx_ws_overlap (enrollment_id, work_date, status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS leave_requests (
    leave_id INT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id INT NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    reject_reason VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lr_enrollment FOREIGN KEY (enrollment_id) REFERENCES program_enrollments(enrollment_id),
    CONSTRAINT fk_lr_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(user_id),
    CONSTRAINT chk_lr_dates CHECK (from_date <= to_date),
    CONSTRAINT chk_lr_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0),
    CONSTRAINT chk_lr_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_lr_decision CHECK (
        (status = 'PENDING' AND reviewed_by IS NULL AND reviewed_at IS NULL AND reject_reason IS NULL)
        OR (status = 'APPROVED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND reject_reason IS NULL)
        OR (status = 'REJECTED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL
            AND reject_reason IS NOT NULL AND CHAR_LENGTH(TRIM(reject_reason)) > 0)
    ),
    INDEX idx_lr_overlap (enrollment_id, status, from_date, to_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
