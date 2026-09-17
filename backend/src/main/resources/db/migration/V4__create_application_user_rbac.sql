CREATE TABLE application_user (
    id UUID PRIMARY KEY,
    username VARCHAR(200) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(32) NOT NULL,
    tenant_id UUID NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_application_user_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT ck_application_user_role
        CHECK (role IN ('PLATFORM_ADMIN', 'TENANT_ADMIN')),
    CONSTRAINT ck_application_user_tenant_scope
        CHECK (
            (role = 'PLATFORM_ADMIN' AND tenant_id IS NULL)
            OR (role = 'TENANT_ADMIN' AND tenant_id IS NOT NULL)
        )
);

CREATE UNIQUE INDEX uk_application_user_username_ci
    ON application_user (LOWER(username));

CREATE INDEX idx_application_user_tenant_id
    ON application_user (tenant_id);
