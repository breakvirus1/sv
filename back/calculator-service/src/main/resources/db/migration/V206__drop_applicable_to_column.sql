-- Drop applicable_to column from calculator_operations

ALTER TABLE svschema.calculator_operations DROP COLUMN IF EXISTS applicable_to;
