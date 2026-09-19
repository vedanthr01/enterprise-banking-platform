CREATE TABLE credentials (
    id            VARCHAR(36)  NOT NULL,
    username      VARCHAR(64)  NOT NULL,
    email         VARCHAR(128) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_credentials_username UNIQUE (username),
    CONSTRAINT uq_credentials_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE credential_roles (
    credential_id VARCHAR(36) NOT NULL,
    role          VARCHAR(32) NOT NULL,
    PRIMARY KEY (credential_id, role),
    CONSTRAINT fk_credential_roles_credential FOREIGN KEY (credential_id)
        REFERENCES credentials (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refresh_tokens (
    id         BIGINT AUTO_INCREMENT,
    user_id    VARCHAR(36)  NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
