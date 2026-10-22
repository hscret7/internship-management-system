-- Pause attendance writers and back up first. No row is changed or deleted.
-- Check duplicates below. Have QA/HR reconcile any duplicates explicitly first.
-- UNIQUE ALTER fails safely with duplicate-key error when duplicates remain.
USE internship_management;
SELECT enrollment_id, work_date, COUNT(*) AS duplicate_count
FROM attendances GROUP BY enrollment_id, work_date HAVING COUNT(*) > 1;
SET @attendance_sql = IF(EXISTS(
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='attendances' AND index_name='uq_attendance_day'),
    'SELECT 1', 'ALTER TABLE attendances ADD UNIQUE KEY uq_attendance_day (enrollment_id, work_date)');
PREPARE attendance_statement FROM @attendance_sql;
EXECUTE attendance_statement;
DEALLOCATE PREPARE attendance_statement;
