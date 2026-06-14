-- Add width_mm and height_mm columns to order_materials table

ALTER TABLE svtables.order_materials
    ADD COLUMN IF NOT EXISTS width_mm NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS height_mm NUMERIC(10, 2) NOT NULL DEFAULT 0;
