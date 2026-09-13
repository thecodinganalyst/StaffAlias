CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE person (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_person_id_tenant UNIQUE (id, tenant_id)
);

CREATE INDEX idx_person_tenant_id ON person(tenant_id);

CREATE TABLE employment (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    person_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_employment_dates CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT uk_employment_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_employment_person_tenant FOREIGN KEY (person_id, tenant_id)
        REFERENCES person(id, tenant_id)
);

CREATE INDEX idx_employment_tenant_person ON employment(tenant_id, person_id);

CREATE TABLE employment_identifier (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    employment_id UUID NOT NULL,
    identifier_type VARCHAR(32) NOT NULL,
    employee_id VARCHAR(100) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_employment_identifier_dates CHECK (effective_to IS NULL OR effective_to >= effective_from),
    CONSTRAINT fk_identifier_employment_tenant FOREIGN KEY (employment_id, tenant_id)
        REFERENCES employment(id, tenant_id)
);

CREATE INDEX idx_employment_identifier_tenant_employment
    ON employment_identifier(tenant_id, employment_id);

ALTER TABLE employment_identifier
    ADD CONSTRAINT ex_employee_id_no_overlap
    EXCLUDE USING gist (
        tenant_id WITH =,
        employee_id WITH =,
        daterange(effective_from, COALESCE(effective_to, 'infinity'::date), '[]') WITH &&
    )
    WHERE (identifier_type = 'EMPLOYEE_ID');
