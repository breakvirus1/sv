-- Flyway migration V4: Add missing columns (deleted, updated_at) to calculations and calculation_operations tables

ALTER TABLE calculator.calculator_calculations
ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE calculator.calculator_calculation_operations
ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
