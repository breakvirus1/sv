-- Привязка операций изделия к позиции материала.
-- Позиция изделия = материал + список операций, применяемых к этому материалу.
ALTER TABLE svschema.product_operations
    ADD COLUMN IF NOT EXISTS product_material_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_product_operations_product_material'
    ) THEN
        ALTER TABLE svschema.product_operations
            ADD CONSTRAINT fk_product_operations_product_material
            FOREIGN KEY (product_material_id)
            REFERENCES svschema.product_materials(id)
            ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_product_operations_product_material
    ON svschema.product_operations(product_material_id);
