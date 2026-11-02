-- V102: Create roles and employee_roles tables for Keycloak role synchronization
CREATE TABLE IF NOT EXISTS svschema.roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS svschema.employee_roles (
    employee_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL REFERENCES svschema.roles(id) ON DELETE CASCADE,
    PRIMARY KEY (employee_id, role_id)
);

-- Insert default roles (idempotent)
INSERT INTO svschema.roles (name)
SELECT 'ROLE_ADMIN' WHERE NOT EXISTS (SELECT 1 FROM svschema.roles WHERE name = 'ROLE_ADMIN');

INSERT INTO svschema.roles (name)
SELECT 'ROLE_MANAGER' WHERE NOT EXISTS (SELECT 1 FROM svschema.roles WHERE name = 'ROLE_MANAGER');

INSERT INTO svschema.roles (name)
SELECT 'ROLE_PRODUCTION' WHERE NOT EXISTS (SELECT 1 FROM svschema.roles WHERE name = 'ROLE_PRODUCTION');

INSERT INTO svschema.roles (name)
SELECT 'ROLE_ACCOUNTANT' WHERE NOT EXISTS (SELECT 1 FROM svschema.roles WHERE name = 'ROLE_ACCOUNTANT');

INSERT INTO svschema.roles (name)
SELECT 'ROLE_USER' WHERE NOT EXISTS (SELECT 1 FROM svschema.roles WHERE name = 'ROLE_USER');
