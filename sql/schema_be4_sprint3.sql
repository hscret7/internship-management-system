-- Extend evaluations without converting/deleting the original score 0-100 rows.
-- Run after sql/schema_be4.sql. Safe to run again; no existing rows deleted.
USE internship_management;
CREATE TABLE IF NOT EXISTS weekly_report_sections (
    report_id BIGINT PRIMARY KEY,
    results TEXT NOT NULL,
    difficulties TEXT NOT NULL,
    next_plan TEXT NOT NULL,
    FOREIGN KEY (report_id) REFERENCES weekly_reports(report_id)
);
CREATE TABLE IF NOT EXISTS task_progress_actors (
    progress_id BIGINT PRIMARY KEY,
    actor_user_id BIGINT NOT NULL,
    FOREIGN KEY (progress_id) REFERENCES task_progress(progress_id),
    FOREIGN KEY (actor_user_id) REFERENCES users(user_id)
);
-- DDL is not transactionally rolled back by MySQL. Back up first and pause writers.
-- Every guarded step can be rerun after an interrupted migration.
SET @be4_sql = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='evaluations' AND column_name='skill_score'), 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN skill_score TINYINT NULL, ADD COLUMN attitude_score TINYINT NULL');
PREPARE be4_statement FROM @be4_sql;
EXECUTE be4_statement;
DEALLOCATE PREPARE be4_statement;
ALTER TABLE evaluations MODIFY COLUMN score DECIMAL(5,2) NULL;
SET @be4_sql = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='evaluations' AND column_name='legacy_enrollment_id'), 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN legacy_enrollment_id INT GENERATED ALWAYS AS (CASE WHEN score IS NOT NULL THEN enrollment_id ELSE NULL END) STORED');
PREPARE be4_statement FROM @be4_sql;
EXECUTE be4_statement;
DEALLOCATE PREPARE be4_statement;
SET @be4_sql = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='evaluations' AND index_name='uq_eval_legacy_enrollment'), 'SELECT 1', 'ALTER TABLE evaluations ADD UNIQUE INDEX uq_eval_legacy_enrollment (legacy_enrollment_id)');
PREPARE be4_statement FROM @be4_sql;
EXECUTE be4_statement;
DEALLOCATE PREPARE be4_statement;
SET @be4_sql = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='evaluations' AND index_name='ix_eval_history'), 'SELECT 1', 'ALTER TABLE evaluations ADD INDEX ix_eval_history (enrollment_id, evaluation_id)');
PREPARE be4_statement FROM @be4_sql;
EXECUTE be4_statement;
DEALLOCATE PREPARE be4_statement;
SET @be4_sql = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='evaluations' AND index_name='uq_eval_enrollment'), 'ALTER TABLE evaluations DROP INDEX uq_eval_enrollment', 'SELECT 1');
PREPARE be4_statement FROM @be4_sql;
EXECUTE be4_statement;
DEALLOCATE PREPARE be4_statement;
SET @be4_sql = IF(EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='evaluations' AND constraint_name='ck_eval_record_shape'), 'SELECT 1', 'ALTER TABLE evaluations ADD CONSTRAINT ck_eval_record_shape CHECK ((score IS NOT NULL AND skill_score IS NULL AND attitude_score IS NULL) OR (score IS NULL AND skill_score IS NOT NULL AND attitude_score IS NOT NULL AND skill_score BETWEEN 1 AND 5 AND attitude_score BETWEEN 1 AND 5 AND comment IS NOT NULL AND CHAR_LENGTH(TRIM(comment)) > 0))');
PREPARE be4_statement FROM @be4_sql;
EXECUTE be4_statement;
DEALLOCATE PREPARE be4_statement;
CREATE TABLE IF NOT EXISTS report_export_events (
    export_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NOT NULL,
    program_id INT NULL,
    enrollment_id INT NULL,
    enrollment_status VARCHAR(20) NULL,
    format VARCHAR(10) NOT NULL,
    report_kind VARCHAR(30) NOT NULL DEFAULT 'END_TERM',
    cohort_from DATE NULL,
    cohort_to DATE NULL,
    row_count INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (actor_user_id) REFERENCES users(user_id)
);
