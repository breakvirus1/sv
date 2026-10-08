ALTER TABLE svschema.statistic_orders ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE svschema.statistic_order_items ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE svschema.statistic_material_consumptions ADD COLUMN IF NOT EXISTS company_id BIGINT;
