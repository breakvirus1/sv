-- Flyway migration V5: Align material reference to shared svtables.materials
-- This migration updates the foreign key in calculator_calculations to reference the shared materials table.

-- First, clear existing calculation data to avoid FK violations (dev environment)
DELETE FROM calculator.calculator_calculation_operations;
DELETE FROM calculator.calculator_calculations;

-- Drop the old foreign key constraint (auto-generated name)
ALTER TABLE calculator.calculator_calculations DROP CONSTRAINT IF EXISTS calculator_calculations_material_id_fkey;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'svtables' AND table_name = 'materials') THEN
        ALTER TABLE calculator.calculator_calculations
            DROP CONSTRAINT IF EXISTS fk_calculator_calculations_material;
        ALTER TABLE calculator.calculator_calculations
            ADD CONSTRAINT fk_calculator_calculations_material
            FOREIGN KEY (material_id) REFERENCES svtables.materials (id) ON DELETE RESTRICT;
    END IF;
END $$;
