-- Flyway migration V2: Add hem parameters to operations table

ALTER TABLE svschema.calculator_operations
ADD COLUMN IF NOT EXISTS hem_width_mm INTEGER,
ADD COLUMN IF NOT EXISTS hem_count INTEGER;
