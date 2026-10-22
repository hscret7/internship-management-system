-- Apply explicitly to the selected application database; no automatic startup DDL.
CREATE TABLE IF NOT EXISTS audit_logs (
    audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NULL,
    action VARCHAR(64) NOT NULL,
    object_type VARCHAR(64) NOT NULL,
    object_id BIGINT NULL,
    occurred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    result ENUM('SUCCESS', 'FAILURE') NOT NULL,
    INDEX idx_audit_time (occurred_at, audit_id),
    INDEX idx_audit_actor_time (actor_user_id, occurred_at),
    INDEX idx_audit_action_time (action, occurred_at)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- No FK: history remains readable after actor/object deletion.
