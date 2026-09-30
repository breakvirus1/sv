-- Add ready flag to order_items
ALTER TABLE svschema.order_items ADD COLUMN IF NOT EXISTS ready BOOLEAN DEFAULT FALSE;
UPDATE svschema.order_items SET ready = FALSE WHERE ready IS NULL;
