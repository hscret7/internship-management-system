-- ========================================================
-- MIGRATION SPRINT 2: BE2 - DUYỆT TÀI LIỆU (US05)
-- Thêm các trường reviewed_by, reviewed_at, reject_reason vào bảng documents
-- ========================================================

USE internship_management;

SET NAMES 'utf8mb4' COLLATE 'utf8mb4_unicode_ci';

ALTER TABLE documents
    ADD COLUMN reviewed_by BIGINT NULL AFTER status,
    ADD COLUMN reviewed_at TIMESTAMP NULL AFTER reviewed_by,
    ADD COLUMN reject_reason TEXT NULL AFTER reviewed_at;

ALTER TABLE documents
    ADD CONSTRAINT fk_documents_reviewed_by
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id) ON DELETE SET NULL;
