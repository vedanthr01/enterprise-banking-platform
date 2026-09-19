CREATE TABLE transactions (
    id                   VARCHAR(36)   NOT NULL,
    idempotency_key      VARCHAR(128)  NOT NULL,
    from_account_id      VARCHAR(36)   NOT NULL,
    to_account_id        VARCHAR(36)   NOT NULL,
    amount               DECIMAL(19,4) NOT NULL,
    currency             VARCHAR(3)    NOT NULL,
    status               VARCHAR(16)   NOT NULL,
    initiated_by_user_id VARCHAR(36)   NOT NULL,
    failure_reason       TEXT,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_transactions_idempotency_key UNIQUE (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_transactions_from_account ON transactions (from_account_id);
CREATE INDEX idx_transactions_to_account ON transactions (to_account_id);
CREATE INDEX idx_transactions_initiated_by ON transactions (initiated_by_user_id);

CREATE TABLE outbox_events (
    id           BIGINT AUTO_INCREMENT,
    aggregate_id VARCHAR(36) NOT NULL,
    event_type   VARCHAR(32) NOT NULL,
    topic        VARCHAR(64) NOT NULL,
    payload      TEXT NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts     INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL,
    published_at TIMESTAMP NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_outbox_status ON outbox_events (status);
