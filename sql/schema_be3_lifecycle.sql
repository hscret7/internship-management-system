-- Additive Sprint 3 migration. Run after schema_be3.sql; no history is deleted.
USE internship_management;
CREATE TABLE IF NOT EXISTS program_lifecycle_events (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    program_id INT NOT NULL,
    from_status VARCHAR(20) NOT NULL,
    to_status VARCHAR(20) NOT NULL,
    actor_user_id BIGINT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (program_id) REFERENCES internship_programs(program_id),
    FOREIGN KEY (actor_user_id) REFERENCES users(user_id)
);
CREATE TABLE IF NOT EXISTS enrollment_lifecycle_events (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id INT NOT NULL,
    from_status VARCHAR(20) NOT NULL,
    to_status VARCHAR(20) NOT NULL,
    actor_user_id BIGINT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (enrollment_id) REFERENCES program_enrollments(enrollment_id),
    FOREIGN KEY (actor_user_id) REFERENCES users(user_id)
);
