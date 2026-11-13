-- NEXO stores conversations separately; never changes IAM data or migrations V1/V2.
CREATE TABLE nexo_conversation (
 id CHAR(36) PRIMARY KEY,
 owner_id BINARY(16) NOT NULL,
 auth_scope CHAR(64) NOT NULL,
 title VARCHAR(80) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 FOREIGN KEY(owner_id) REFERENCES app_user(id),
 INDEX ix_nexo_owner(owner_id, expires_at)
) ENGINE=InnoDB;
CREATE TABLE nexo_message (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 conversation_id CHAR(36) NOT NULL,
 speaker VARCHAR(12) NOT NULL,
 content TEXT NOT NULL,
 metadata TEXT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 FOREIGN KEY(conversation_id) REFERENCES nexo_conversation(id) ON DELETE CASCADE,
 INDEX ix_nexo_messages(conversation_id,id)
) ENGINE=InnoDB;
CREATE TABLE nexo_audit (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 actor_fingerprint CHAR(64) NOT NULL,
 event_code VARCHAR(40) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 INDEX ix_nexo_audit_time(created_at)
) ENGINE=InnoDB;
