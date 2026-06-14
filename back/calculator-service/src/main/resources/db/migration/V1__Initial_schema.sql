-- Flyway migration V1: Create tables for calculator service

CREATE SCHEMA IF NOT EXISTS calculator;

-- Materials table (SINGLE_TABLE inheritance)
CREATE TABLE IF NOT EXISTS calculator.calculator_materials (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_per_square_meter DECIMAL(12,2),
    waste_coefficient DECIMAL(5,3) DEFAULT 1.0,
    material_type VARCHAR(31) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- Eyelets table
CREATE TABLE IF NOT EXISTS calculator.calculator_eyelets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_per_piece DECIMAL(12,2) NOT NULL,
    diameter_mm INTEGER,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- Operations table
CREATE TABLE IF NOT EXISTS calculator.calculator_operations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    unit_type VARCHAR(20) NOT NULL,
    applicable_to VARCHAR(20) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- Calculations table
CREATE TABLE IF NOT EXISTS calculator.calculator_calculations (
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
    FOREIGN KEY (material_id) REFERENCES calculator.calculator_materials (id),
    FOREIGN KEY (eyelet_id) REFERENCES calculator.calculator_eyelets (id)
);

-- Calculation operations join table
CREATE TABLE IF NOT EXISTS calculator.calculator_calculation_operations (
    id BIGSERIAL PRIMARY KEY,
    calculation_id BIGINT NOT NULL,
    operation_id BIGINT NOT NULL,
    quantity DECIMAL(12,4) NOT NULL,
    price_per_unit DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (calculation_id) REFERENCES calculator.calculator_calculations (id) ON DELETE CASCADE,
    FOREIGN KEY (operation_id) REFERENCES calculator.calculator_operations (id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_calc_calculations_material ON calculator.calculator_calculations(material_id);
CREATE INDEX IF NOT EXISTS idx_calc_calculations_eyelet ON calculator.calculator_calculations(eyelet_id);
CREATE INDEX IF NOT EXISTS idx_calcops_calculation ON calculator.calculator_calculation_operations(calculation_id);
CREATE INDEX IF NOT EXISTS idx_calcops_operation ON calculator.calculator_calculation_operations(operation_id);
