-- Flyway migration V1: Create tables for calculator service
-- These tables are in the 'calculator' schema, separate from shared ordschema

CREATE SCHEMA IF NOT EXISTS calculator;

CREATE TABLE IF NOT EXISTS svschema.calculator_eyelets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_per_piece DECIMAL(12,2) NOT NULL,
    diameter_mm INTEGER,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS svschema.calculator_operations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    unit_type VARCHAR(20) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS svschema.calculator_calculations (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL,
    width_m DECIMAL(10,4) NOT NULL,
    height_m DECIMAL(10,4) NOT NULL,
    dpi INTEGER,
    podvorot_mm_horizontal DECIMAL(10,2),
    podvorot_mm_vertical DECIMAL(10,2),
    podvorot_count_per_side INTEGER DEFAULT 2,
    eyelet_id BIGINT,
    eyelet_step_cm INTEGER DEFAULT 40,
    total_price DECIMAL(12,2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS svschema.calculator_calculation_operations (
    id BIGSERIAL PRIMARY KEY,
    calculation_id BIGINT NOT NULL,
    operation_id BIGINT NOT NULL,
    quantity DECIMAL(12,4) NOT NULL,
    price_per_unit DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_calc_ops_calculation FOREIGN KEY (calculation_id) REFERENCES svschema.calculator_calculations (id) ON DELETE CASCADE,
    CONSTRAINT fk_calc_ops_operation FOREIGN KEY (operation_id) REFERENCES svschema.calculator_operations (id)
);

CREATE INDEX IF NOT EXISTS idx_calc_calculations_material ON svschema.calculator_calculations(material_id);
CREATE INDEX IF NOT EXISTS idx_calc_calculations_eyelet ON svschema.calculator_calculations(eyelet_id);
CREATE INDEX IF NOT EXISTS idx_calcops_calculation ON svschema.calculator_calculation_operations(calculation_id);
CREATE INDEX IF NOT EXISTS idx_calcops_operation ON svschema.calculator_calculation_operations(operation_id);
