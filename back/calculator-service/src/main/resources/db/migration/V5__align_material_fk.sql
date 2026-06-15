-- Flyway migration V5: Align material reference to shared ordschema.materials
-- This migration updates the foreign key in calculator_calculations to reference the shared materials table.

-- First, clear existing calculation data to avoid FK violations (dev environment)
DELETE FROM calculator_calculation_operations;
DELETE FROM calculator_calculations;

-- Drop the old foreign key constraint (auto-generated name)
ALTER TABLE calculator_calculations DROP CONSTRAINT IF EXISTS calculator_calculations_material_id_fkey;

-- Add new foreign key referencing ordschema.materials
ALTER TABLE calculator_calculations
    ADD CONSTRAINT fk_calculator_calculations_material
    FOREIGN KEY (material_id) REFERENCES ordschema.materials (id) ON DELETE RESTRICT;
