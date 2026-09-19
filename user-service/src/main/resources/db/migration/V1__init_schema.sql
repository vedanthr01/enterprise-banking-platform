CREATE TABLE user_profiles (
    user_id       VARCHAR(36)  NOT NULL,
    username      VARCHAR(64)  NOT NULL,
    email         VARCHAR(128) NOT NULL,
    first_name    VARCHAR(64)  NOT NULL,
    last_name     VARCHAR(64),
    phone         VARCHAR(20),
    address_line  VARCHAR(255),
    city          VARCHAR(64),
    state         VARCHAR(64),
    postal_code   VARCHAR(16),
    country       VARCHAR(64),
    date_of_birth DATE,
    kyc_status    VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
