-- Fix unique constraint on operation_groups to exclude soft-deleted records
-- This allows reusing names of deleted operation groups

-- Drop the old unique constraint (PostgreSQL creates both constraint and index for UNIQUE)
ALTER TABLE svschema.operation_groups DROP CONSTRAINT IF EXISTS operation_groups_name_key;

-- Create a new partial unique index that only includes non-deleted records
CREATE UNIQUE INDEX operation_groups_name_key
    ON svschema.operation_groups (name)
    WHERE deleted = false;
