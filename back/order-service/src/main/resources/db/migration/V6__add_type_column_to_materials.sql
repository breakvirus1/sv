-- Flyway migration V5: Add type column to materials table for material-service

ALTER TABLE ordschema.materials
ADD COLUMN IF NOT EXISTS type VARCHAR(20);

-- Update existing materials to have default type 'MATERIAL'
UPDATE ordschema.materials
SET type = 'MATERIAL'
WHERE type IS NULL;
