-- Flyway migration V4: Add missing columns (deleted, updated_at) to calculations and calculation_operations tables

ALTER TABLE svschema.calculator_calculations
ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE svschema.calculator_calculation_operations
ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
