-- ========================================================
-- MIGRATION SPRINT 2: BE2 - DUYỆT TÀI LIỆU (US05)
-- Thêm an toàn các trường reviewed_by, reviewed_at, reject_reason vào bảng documents
-- Tương thích idempotent: Chạy lại nhiều lần không sinh lỗi.
-- ========================================================

USE internship_management;

SET NAMES 'utf8mb4' COLLATE 'utf8mb4_unicode_ci';

DROP PROCEDURE IF EXISTS migrate_be2_documents_proc;
DELIMITER $$
CREATE PROCEDURE migrate_be2_documents_proc()
BEGIN
    -- 1. Thêm cột reviewed_by nếu chưa có
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'documents' AND COLUMN_NAME = 'reviewed_by'
    ) THEN
        ALTER TABLE documents ADD COLUMN reviewed_by BIGINT NULL AFTER status;
    END IF;

    -- 2. Thêm cột reviewed_at nếu chưa có
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'documents' AND COLUMN_NAME = 'reviewed_at'
    ) THEN
        ALTER TABLE documents ADD COLUMN reviewed_at TIMESTAMP NULL AFTER reviewed_by;
    END IF;

    -- 3. Thêm cột reject_reason nếu chưa có
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'documents' AND COLUMN_NAME = 'reject_reason'
    ) THEN
        ALTER TABLE documents ADD COLUMN reject_reason TEXT NULL AFTER reviewed_at;
    END IF;

    -- 4. Thêm khóa ngoại fk_documents_reviewed_by nếu chưa có
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'documents' AND CONSTRAINT_NAME = 'fk_documents_reviewed_by'
    ) THEN
        ALTER TABLE documents
            ADD CONSTRAINT fk_documents_reviewed_by
            FOREIGN KEY (reviewed_by) REFERENCES users(user_id) ON DELETE SET NULL;
    END IF;
END$$
DELIMITER ;

CALL migrate_be2_documents_proc();
DROP PROCEDURE IF EXISTS migrate_be2_documents_proc;

