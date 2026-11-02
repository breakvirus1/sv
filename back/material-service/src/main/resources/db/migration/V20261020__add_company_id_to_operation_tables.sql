ALTER TABLE svschema.material_operations ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE svschema.operation_parameters ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE svschema.operation_additional_materials ADD COLUMN IF NOT EXISTS company_id BIGINT;
