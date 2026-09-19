CREATE TABLE loan_applications (
    id                       VARCHAR(36)   NOT NULL,
    applicant_user_id        VARCHAR(36)   NOT NULL,
    loan_type                VARCHAR(16)   NOT NULL,
    principal_amount         DECIMAL(19,4) NOT NULL,
    interest_rate_annual     DECIMAL(5,2)  NOT NULL,
    tenure_months            INT           NOT NULL,
    purpose                  VARCHAR(255),
    disbursement_account_id  VARCHAR(36)   NOT NULL,
    status                   VARCHAR(24)   NOT NULL,
    rejection_reason         VARCHAR(255),
    created_at               TIMESTAMP NOT NULL,
    updated_at               TIMESTAMP NOT NULL,
    decided_at               TIMESTAMP NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_loan_app_applicant ON loan_applications (applicant_user_id);

CREATE TABLE loans (
    id                       VARCHAR(36)   NOT NULL,
    application_id           VARCHAR(36)   NOT NULL,
    borrower_user_id         VARCHAR(36)   NOT NULL,
    loan_type                VARCHAR(16)   NOT NULL,
    disbursement_account_id  VARCHAR(36)   NOT NULL,
    principal_amount         DECIMAL(19,4) NOT NULL,
    interest_rate_annual     DECIMAL(5,2)  NOT NULL,
    tenure_months            INT           NOT NULL,
    emi_amount               DECIMAL(19,4) NOT NULL,
    outstanding_principal    DECIMAL(19,4) NOT NULL,
    status                   VARCHAR(16)   NOT NULL,
    start_date               DATE          NOT NULL,
    created_at               TIMESTAMP NOT NULL,
    updated_at               TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_loan_application UNIQUE (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_loan_borrower ON loans (borrower_user_id);

CREATE TABLE emi_schedules (
    id                     VARCHAR(36)   NOT NULL,
    loan_id                VARCHAR(36)   NOT NULL,
    installment_number     INT           NOT NULL,
    due_date               DATE          NOT NULL,
    emi_amount             DECIMAL(19,4) NOT NULL,
    principal_component    DECIMAL(19,4) NOT NULL,
    interest_component     DECIMAL(19,4) NOT NULL,
    status                 VARCHAR(16)   NOT NULL,
    paid_at                TIMESTAMP NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_emi_loan ON emi_schedules (loan_id);
CREATE INDEX idx_emi_due_status ON emi_schedules (due_date, status);

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

CREATE INDEX idx_loan_outbox_status ON outbox_events (status);
