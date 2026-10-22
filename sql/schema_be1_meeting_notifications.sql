-- Task 35.2. Manual migration AFTER schema_be1_meetings.sql. No runtime DDL.
CREATE TABLE IF NOT EXISTS meeting_recipient_snapshots (
 meeting_id BIGINT NOT NULL,
 revision INT NOT NULL,
 recipient_user_id BIGINT NOT NULL,
 recipient_email VARCHAR(255) NOT NULL,
 PRIMARY KEY(meeting_id,revision,recipient_user_id),
 CONSTRAINT fk_be1_snapshot_meeting FOREIGN KEY(meeting_id) REFERENCES meetings(meeting_id),
 CONSTRAINT fk_be1_snapshot_user FOREIGN KEY(recipient_user_id) REFERENCES users(user_id),
 CONSTRAINT chk_be1_snapshot_revision CHECK(revision>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS meeting_notification_outbox (
 notification_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 meeting_id BIGINT NOT NULL,
 revision INT NOT NULL,
 event_type VARCHAR(20) NOT NULL,
 recipient_user_id BIGINT NOT NULL,
 recipient_email VARCHAR(255) NOT NULL,
 subject VARCHAR(255) NOT NULL,
 body TEXT NOT NULL,
 due_at DATETIME(6) NOT NULL,
 state VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 attempts INT NOT NULL DEFAULT 0,
 claim_token VARCHAR(36) NULL,
 lease_until DATETIME(6) NULL,
 error_code VARCHAR(40) NULL,
 sent_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uq_be1_notification(meeting_id,revision,event_type,recipient_user_id),
 INDEX idx_be1_due(state,due_at,notification_id),
 INDEX idx_be1_delivery_order(meeting_id,recipient_user_id,notification_id,state),
 CONSTRAINT fk_be1_notification_meeting FOREIGN KEY(meeting_id) REFERENCES meetings(meeting_id),
 CONSTRAINT fk_be1_notification_user FOREIGN KEY(recipient_user_id) REFERENCES users(user_id),
 CONSTRAINT chk_be1_notification_event CHECK(event_type IN ('CREATE','CHANGE','CANCEL','REMINDER')),
 CONSTRAINT chk_be1_notification_state CHECK(state IN ('PENDING','IN_FLIGHT','SENT','FAILED','SUPERSEDED')),
 CONSTRAINT chk_be1_notification_revision CHECK(revision>0),
 CONSTRAINT chk_be1_notification_attempts CHECK(attempts>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
