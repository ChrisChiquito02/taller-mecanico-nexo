-- En bases existentes, verificar el contrato V1 antes de establecer baseline 1.
ALTER TABLE app_user ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE account_token (
 id BINARY(16) PRIMARY KEY, user_id BINARY(16) NOT NULL,
 token_hash CHAR(64) NOT NULL UNIQUE, kind VARCHAR(16) NOT NULL,
 expires_at DATETIME(6) NOT NULL, consumed_at DATETIME(6),
 FOREIGN KEY(user_id) REFERENCES app_user(id), INDEX ix_token_user(user_id,kind),
 INDEX ix_token_expiry(expires_at)
) ENGINE=InnoDB;
INSERT INTO role(id,code,name,description,active) VALUES
 (UNHEX('01900000000070008000000000000001'),'USER','Usuario','Acceso propio',true),
 (UNHEX('01900000000070008000000000000002'),'ADMIN','Administrador','Administración de identidad',true)
 ON DUPLICATE KEY UPDATE code=code;
INSERT INTO permission(id,code,module_code,description) VALUES
 (UNHEX('01900000000070008000000000000011'),'profile:read','identity','Consultar perfil propio'),
 (UNHEX('01900000000070008000000000000012'),'users:read','identity','Consultar usuarios'),
 (UNHEX('01900000000070008000000000000013'),'users:write','identity','Administrar roles y estado')
 ON DUPLICATE KEY UPDATE code=code;
INSERT IGNORE INTO role_permission(role_id,permission_id)
 SELECT r.id,p.id FROM role r CROSS JOIN permission p
 WHERE (r.code='USER' AND p.code='profile:read') OR (r.code='ADMIN' AND p.code IN ('profile:read','users:read','users:write'));
