ALTER TABLE svschema.calculator_operations ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE svschema.operation_groups ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE svschema.calculator_calculations ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE svschema.calculator_calculation_operations ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE svschema.calculator_eyelets ADD COLUMN IF NOT EXISTS company_id BIGINT;
