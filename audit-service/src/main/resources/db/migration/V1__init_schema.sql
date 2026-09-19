CREATE TABLE audit_events (
    id                VARCHAR(36)  NOT NULL,
    source_event_id   VARCHAR(64)  NOT NULL,
    domain            VARCHAR(16)  NOT NULL,
    event_type        VARCHAR(40)  NOT NULL,
    aggregate_id      VARCHAR(36),
    actor_user_id     VARCHAR(36),
    severity          VARCHAR(16)  NOT NULL,
    description       VARCHAR(500) NOT NULL,
    metadata          TEXT NOT NULL,
    occurred_at       TIMESTAMP NOT NULL,
    recorded_at       TIMESTAMP NOT NULL,
    record_hash       VARCHAR(64)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_audit_source_event UNIQUE (source_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_audit_domain ON audit_events (domain);
CREATE INDEX idx_audit_event_type ON audit_events (event_type);
CREATE INDEX idx_audit_aggregate ON audit_events (aggregate_id);
CREATE INDEX idx_audit_actor ON audit_events (actor_user_id);
CREATE INDEX idx_audit_occurred_at ON audit_events (occurred_at);
