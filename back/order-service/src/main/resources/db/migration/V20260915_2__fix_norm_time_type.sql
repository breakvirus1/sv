ALTER TABLE svschema.product_operations ALTER COLUMN norm_time TYPE VARCHAR(50) USING norm_time::text;
