-- US36: apply manually after the meetings schema, before enabling the feature.
CREATE TABLE IF NOT EXISTS inapp_notifications (
  notification_id BIGINT NOT NULL AUTO_INCREMENT,
  recipient_user_id BIGINT NOT NULL,
  event_key VARCHAR(160) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  kind VARCHAR(30) NOT NULL,
  source_type VARCHAR(20) NOT NULL,
  source_id BIGINT NOT NULL,
  source_revision INT NOT NULL,
  title VARCHAR(200) NOT NULL,
  body TEXT NOT NULL,
  available_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  read_at DATETIME(6) NULL,
  superseded_at DATETIME(6) NULL,
  PRIMARY KEY (notification_id),
  UNIQUE KEY uq_inapp_event_recipient (event_key,recipient_user_id),
  KEY idx_inapp_recipient (recipient_user_id,superseded_at,available_at,notification_id),
  KEY idx_inapp_source (source_type,source_id,kind,source_revision),
  CONSTRAINT fk_inapp_recipient FOREIGN KEY (recipient_user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
