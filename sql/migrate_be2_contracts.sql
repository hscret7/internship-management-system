-- ========================================================
-- MIGRATION SPRINT 2: BE2 - HỢP ĐỒNG THỰC TẬP (US09, US10)
-- Đảm bảo cấu trúc bảng contracts và các cột confirmed_at, status cho QA
-- Tương thích idempotent: Chạy lại nhiều lần không sinh lỗi.
-- ========================================================

USE internship_management;

SET NAMES 'utf8mb4' COLLATE 'utf8mb4_unicode_ci';

-- 1. Tạo bảng contracts nếu chưa tồn tại
CREATE TABLE IF NOT EXISTS contracts (
    contract_id INT AUTO_INCREMENT PRIMARY KEY,
    application_id INT NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    uploaded_by BIGINT NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) DEFAULT 'PENDING_CONFIRMATION',
    confirmed_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS migrate_be2_contracts_proc;
DELIMITER $$
CREATE PROCEDURE migrate_be2_contracts_proc()
BEGIN
    -- 2. Thêm cột confirmed_at nếu chưa có
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND COLUMN_NAME = 'confirmed_at'
    ) THEN
        ALTER TABLE contracts ADD COLUMN confirmed_at TIMESTAMP NULL AFTER status;
    END IF;

    -- 3. Thêm cột status nếu chưa có
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND COLUMN_NAME = 'status'
    ) THEN
        ALTER TABLE contracts ADD COLUMN status VARCHAR(30) DEFAULT 'PENDING_CONFIRMATION' AFTER uploaded_at;
    END IF;

    -- 4. Thêm khóa ngoại nếu có thể
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND CONSTRAINT_NAME = 'fk_contracts_application'
    ) THEN
        IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'applications') THEN
            ALTER TABLE contracts
                ADD CONSTRAINT fk_contracts_application
                FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE;
        END IF;
    END IF;
END$$
DELIMITER ;

CALL migrate_be2_contracts_proc();
DROP PROCEDURE IF EXISTS migrate_be2_contracts_proc;
