-- Additive US34 export evidence only. Requires schema_be4_sprint3.sql first.
-- Back up before applying MySQL DDL. Does not change BE2 attendance rows or program cohort filters.
USE internship_management;
SET @be4_sql = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='report_export_events' AND column_name='report_from'), 'SELECT 1', 'ALTER TABLE report_export_events ADD COLUMN report_from DATE NULL');
PREPARE be4_statement FROM @be4_sql;
EXECUTE be4_statement;
DEALLOCATE PREPARE be4_statement;
SET @be4_sql = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='report_export_events' AND column_name='report_to'), 'SELECT 1', 'ALTER TABLE report_export_events ADD COLUMN report_to DATE NULL');
PREPARE be4_statement FROM @be4_sql;
EXECUTE be4_statement;
DEALLOCATE PREPARE be4_statement;
