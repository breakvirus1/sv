-- Flyway migration V5: Create shared materials table (order service)
-- Materials are seeded per-company in client-service CompanyService.createCompany

CREATE TABLE IF NOT EXISTS svschema.materials (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(20),
    price NUMERIC(12,2) DEFAULT 0,
    waste_coefficient NUMERIC(5,3) DEFAULT 1.0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false
);
