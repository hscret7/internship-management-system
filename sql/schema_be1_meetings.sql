-- BE1 Task 35.1. Apply manually after core/BE2/BE3 schemas; never run at startup.
-- Existing tables/data are not modified. Review compatibility before rerunning.
CREATE TABLE IF NOT EXISTS meetings (
    meeting_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    program_id INT NOT NULL,
    organizer_user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    location VARCHAR(500) NOT NULL,
    starts_at TIMESTAMP(6) NOT NULL,
    ends_at TIMESTAMP(6) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    revision INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_be1_meeting_program FOREIGN KEY (program_id) REFERENCES internship_programs(program_id),
    CONSTRAINT fk_be1_meeting_organizer FOREIGN KEY (organizer_user_id) REFERENCES users(user_id),
    CONSTRAINT chk_be1_meeting_time CHECK (ends_at > starts_at),
    CONSTRAINT chk_be1_meeting_status CHECK (status IN ('SCHEDULED', 'CANCELLED')),
    CONSTRAINT chk_be1_meeting_revision CHECK (revision > 0),
    INDEX idx_be1_meeting_program_time (program_id, starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS meeting_participants (
    meeting_id BIGINT NOT NULL,
    enrollment_id INT NOT NULL,
    PRIMARY KEY (meeting_id, enrollment_id),
    CONSTRAINT fk_be1_participant_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(meeting_id),
    CONSTRAINT fk_be1_participant_enrollment FOREIGN KEY (enrollment_id) REFERENCES program_enrollments(enrollment_id),
    INDEX idx_be1_participant_enrollment (enrollment_id, meeting_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- No cascading deletion: retain cancelled meetings and historical participant links.
