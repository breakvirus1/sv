ALTER TABLE svschema.order_materials ADD COLUMN IF NOT EXISTS eyelet_cost NUMERIC(12,2) DEFAULT 0;
