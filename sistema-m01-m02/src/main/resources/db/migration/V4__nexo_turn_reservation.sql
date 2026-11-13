CREATE TABLE nexo_turn_reservation (
    owner_id BINARY(16) PRIMARY KEY,
    conversation_id CHAR(36) NOT NULL,
    turn_id CHAR(36) NOT NULL,
    cancelled BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at DATETIME(6) NOT NULL,
    INDEX ix_nexo_reservation_expiry (expires_at)
) ENGINE=InnoDB;
