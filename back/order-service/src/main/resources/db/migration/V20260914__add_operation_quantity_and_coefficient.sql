ALTER TABLE svschema.product_operations ADD COLUMN IF NOT EXISTS quantity NUMERIC(12,4) DEFAULT 1;
ALTER TABLE svschema.product_operations ADD COLUMN IF NOT EXISTS coefficient NUMERIC(5,3) DEFAULT 1.000;
