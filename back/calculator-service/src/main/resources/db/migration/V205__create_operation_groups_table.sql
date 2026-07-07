-- Flyway migration V5: Create operation_groups table

CREATE TABLE IF NOT EXISTS svschema.operation_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_operation_groups_name ON svschema.operation_groups(name);
