CREATE TABLE notification_logs (
    id                 VARCHAR(36)  NOT NULL,
    source_event_id    VARCHAR(64)  NOT NULL,
    recipient_user_id  VARCHAR(36)  NOT NULL,
    recipient_email    VARCHAR(128),
    type               VARCHAR(24)  NOT NULL,
    subject            VARCHAR(160) NOT NULL,
    body               TEXT NOT NULL,
    status             VARCHAR(16)  NOT NULL,
    error_message      VARCHAR(255),
    created_at         TIMESTAMP NOT NULL,
    sent_at            TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_notification_source_event UNIQUE (source_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_notification_recipient ON notification_logs (recipient_user_id);
