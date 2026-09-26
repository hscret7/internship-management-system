-- Run ONCE only if an older sql/schema_be3.sql was already applied.
-- Back up the database first. Do not run this after a fresh installation of
-- the current schema_be3.sql; the generated columns already exist there.
USE internship_management;

-- Inspect these results first. Resolve duplicate ACTIVE rows before ALTER.
SELECT intern_profile_id, COUNT(*) AS active_count
FROM program_enrollments WHERE status = 'ACTIVE'
GROUP BY intern_profile_id HAVING COUNT(*) > 1;
SELECT enrollment_id, COUNT(*) AS current_mentor_count
FROM mentor_assignments WHERE ended_at IS NULL
GROUP BY enrollment_id HAVING COUNT(*) > 1;
SELECT program_id, max_interns FROM internship_programs
WHERE max_interns IS NULL OR max_interns < 0;
SELECT mentor_id, max_mentees FROM mentors
WHERE max_mentees IS NULL OR max_mentees <= 0;

-- Align legacy status names with the agreed vocabulary.
UPDATE internship_programs SET status = 'CANCELLED' WHERE status = 'INACTIVE';
UPDATE program_enrollments SET status = 'CANCELLED' WHERE status = 'WITHDRAWN';

ALTER TABLE internship_programs
    MODIFY max_interns INT NOT NULL DEFAULT 0,
    MODIFY status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    ADD CONSTRAINT chk_program_capacity CHECK (max_interns >= 0),
    ADD CONSTRAINT chk_program_status
        CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED'));

ALTER TABLE mentors
    MODIFY max_mentees INT NOT NULL DEFAULT 5,
    ADD CONSTRAINT chk_mentor_capacity CHECK (max_mentees > 0);

ALTER TABLE program_enrollments
    MODIFY status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN active_intern_profile_id INT GENERATED ALWAYS AS
        (CASE WHEN status = 'ACTIVE' THEN intern_profile_id ELSE NULL END) STORED,
    ADD UNIQUE KEY uq_one_active_enrollment_per_intern (active_intern_profile_id),
    ADD CONSTRAINT chk_enrollment_status
        CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'));

ALTER TABLE mentor_assignments
    ADD COLUMN active_enrollment_id INT GENERATED ALWAYS AS
        (CASE WHEN ended_at IS NULL THEN enrollment_id ELSE NULL END) STORED,
    ADD UNIQUE KEY uq_one_current_mentor_per_enrollment (active_enrollment_id);
