-- Add missing BaseEntity columns to order_item_operations
ALTER TABLE ordschema.order_item_operations ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE ordschema.order_item_operations ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE ordschema.order_item_operations ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false;

-- Update existing rows with default values
UPDATE ordschema.order_item_operations SET created_at = NOW() WHERE created_at IS NULL;
UPDATE ordschema.order_item_operations SET updated_at = NOW() WHERE updated_at IS NULL;
UPDATE ordschema.order_item_operations SET deleted = false WHERE deleted IS NULL;
