-- Solo se aplica en un esquema vacío. No usa DROP ni modifica bases ajenas.
CREATE TABLE app_user (
 id BINARY(16) PRIMARY KEY,
 email_normalized VARCHAR(254) NOT NULL,
 password_hash VARCHAR(255) NOT NULL,
 display_name VARCHAR(120) NOT NULL,
 status VARCHAR(20) NOT NULL,
 email_verified BOOLEAN NOT NULL,
 failed_login_count INT NOT NULL,
 locked_until DATETIME(6), password_changed_at DATETIME(6) NOT NULL,
 last_login_at DATETIME(6), created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL, deactivated_at DATETIME(6),
 CONSTRAINT uq_app_user_email UNIQUE(email_normalized)
) ENGINE=InnoDB;
CREATE TABLE role (
 id BINARY(16) PRIMARY KEY, code VARCHAR(50) NOT NULL UNIQUE,
 name VARCHAR(100) NOT NULL, description VARCHAR(255), active BOOLEAN NOT NULL
) ENGINE=InnoDB;
CREATE TABLE permission (
 id BINARY(16) PRIMARY KEY, code VARCHAR(80) NOT NULL UNIQUE,
 module_code VARCHAR(30) NOT NULL, description VARCHAR(255) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE user_role (
 user_id BINARY(16) NOT NULL, role_id BINARY(16) NOT NULL,
 PRIMARY KEY(user_id,role_id), FOREIGN KEY(user_id) REFERENCES app_user(id),
 FOREIGN KEY(role_id) REFERENCES role(id)
) ENGINE=InnoDB;
CREATE TABLE role_permission (
 role_id BINARY(16) NOT NULL, permission_id BINARY(16) NOT NULL,
 PRIMARY KEY(role_id,permission_id), FOREIGN KEY(role_id) REFERENCES role(id),
 FOREIGN KEY(permission_id) REFERENCES permission(id)
) ENGINE=InnoDB;
