-- Add eyelet_quantity column to order_materials
ALTER TABLE svschema.order_materials ADD COLUMN IF NOT EXISTS eyelet_quantity NUMERIC(12,2) DEFAULT 0;

UPDATE svschema.order_materials SET eyelet_quantity = 0 WHERE eyelet_quantity IS NULL;
