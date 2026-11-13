CREATE TABLE nexo_request_quota (
    owner_id BINARY(16) PRIMARY KEY,
    expires_at DATETIME(6) NOT NULL,
    requests INT NOT NULL,
    INDEX ix_nexo_quota_expiry (expires_at)
) ENGINE=InnoDB;
