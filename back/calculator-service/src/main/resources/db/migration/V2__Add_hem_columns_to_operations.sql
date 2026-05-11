-- Flyway migration V2: Add hem parameters to operations table

ALTER TABLE calculator_operations
ADD COLUMN hem_width_mm INTEGER,
ADD COLUMN hem_count INTEGER;
