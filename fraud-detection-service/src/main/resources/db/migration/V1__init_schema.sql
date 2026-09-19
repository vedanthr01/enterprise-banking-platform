CREATE TABLE transfer_history (
    id                VARCHAR(36)    NOT NULL,
    source_event_id   VARCHAR(64)    NOT NULL,
    transaction_id    VARCHAR(36)    NOT NULL,
    from_account_id   VARCHAR(36)    NOT NULL,
    to_account_id     VARCHAR(36)    NOT NULL,
    amount            DECIMAL(19,4)  NOT NULL,
    occurred_at       TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_fraud_history_source_event UNIQUE (source_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_fraud_history_from_account ON transfer_history (from_account_id);
CREATE INDEX idx_fraud_history_from_to ON transfer_history (from_account_id, to_account_id);
CREATE INDEX idx_fraud_history_occurred_at ON transfer_history (occurred_at);
