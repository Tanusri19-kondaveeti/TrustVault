
CREATE TABLE users (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       name VARCHAR(100) NOT NULL,
                       email VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL DEFAULT 'USER',
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       PRIMARY KEY (id),
                       CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE documents (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           owner_id BIGINT NOT NULL,
                           filename VARCHAR(255) NOT NULL,
                           storage_key VARCHAR(500) NOT NULL,
                           content_type VARCHAR(150),
                           file_size BIGINT NOT NULL,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP,
                           PRIMARY KEY (id),
                           CONSTRAINT fk_documents_owner
                               FOREIGN KEY (owner_id) REFERENCES users(id),
                           CONSTRAINT uk_documents_storage_key UNIQUE (storage_key)
);

CREATE TABLE document_grants (
                                 id BIGINT NOT NULL AUTO_INCREMENT,
                                 document_id BIGINT NOT NULL,
                                 granted_to_user_id BIGINT NOT NULL,
                                 permission VARCHAR(10) NOT NULL,
                                 granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY (id),
                                 CONSTRAINT fk_grants_document
                                     FOREIGN KEY (document_id) REFERENCES documents(id),
                                 CONSTRAINT fk_grants_user
                                     FOREIGN KEY (granted_to_user_id) REFERENCES users(id),
                                 CONSTRAINT uk_document_grantee
                                     UNIQUE (document_id, granted_to_user_id),
                                 CONSTRAINT chk_grant_permission
                                     CHECK (permission IN ('READ', 'EDIT'))
);

CREATE TABLE audit_logs (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            actor_user_id BIGINT NULL,
                            document_id BIGINT NULL,
                            action VARCHAR(100) NOT NULL,
                            outcome VARCHAR(20) NOT NULL,
                            details VARCHAR(1000),
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            CONSTRAINT fk_audit_actor
                                FOREIGN KEY (actor_user_id) REFERENCES users(id)
                                    ON DELETE SET NULL,
                            CONSTRAINT fk_audit_document
                                FOREIGN KEY (document_id) REFERENCES documents(id)
                                    ON DELETE SET NULL
);

CREATE INDEX idx_documents_owner ON documents(owner_id);
CREATE INDEX idx_grants_user ON document_grants(granted_to_user_id);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at);
