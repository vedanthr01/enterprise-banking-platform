CREATE TABLE accounts (
    id             VARCHAR(36)   NOT NULL,
    account_number VARCHAR(20)   NOT NULL,
    owner_user_id  VARCHAR(36)   NOT NULL,
    account_type   VARCHAR(20)   NOT NULL,
    balance        DECIMAL(19,4) NOT NULL DEFAULT 0,
    currency       VARCHAR(3)    NOT NULL DEFAULT 'INR',
    status         VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
    maturity_date  DATE,
    interest_rate  DECIMAL(5,2),
    tenure_months  INT,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_accounts_account_number UNIQUE (account_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_accounts_owner ON accounts (owner_user_id);

CREATE TABLE ledger_entries (
    id            BIGINT AUTO_INCREMENT,
    account_id    VARCHAR(36)   NOT NULL,
    reference_id  VARCHAR(64)   NOT NULL,
    entry_type    VARCHAR(20)   NOT NULL,
    amount        DECIMAL(19,4) NOT NULL,
    balance_after DECIMAL(19,4) NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_ledger_idempotency UNIQUE (account_id, reference_id, entry_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_ledger_account ON ledger_entries (account_id);

CREATE TABLE beneficiaries (
    id                         VARCHAR(36)  NOT NULL,
    owner_user_id              VARCHAR(36)  NOT NULL,
    beneficiary_account_number VARCHAR(20)  NOT NULL,
    beneficiary_name           VARCHAR(128) NOT NULL,
    nickname                   VARCHAR(64),
    created_at                 TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_beneficiaries_owner ON beneficiaries (owner_user_id);
