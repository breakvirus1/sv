-- Add default_width_mm and default_height_mm columns to the materials reference table.
-- These values are used by the frontend to pre-fill position dimensions when a material is selected.

ALTER TABLE svtables.materials
    ADD COLUMN IF NOT EXISTS default_width_mm NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS default_height_mm NUMERIC(10, 2) NOT NULL DEFAULT 0;
