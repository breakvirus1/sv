-- Add created_at and updated_at columns to product_materials
ALTER TABLE svschema.product_materials ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE svschema.product_materials ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE svschema.product_materials ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;

-- Add created_at and updated_at columns to product_operations
ALTER TABLE svschema.product_operations ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE svschema.product_operations ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE svschema.product_operations ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;
