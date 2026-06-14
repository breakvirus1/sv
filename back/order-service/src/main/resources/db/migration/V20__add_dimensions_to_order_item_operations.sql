-- ============================================================
-- PrintSV Database Schema - Order Operations Dimensions (V4)
-- Adds width_mm and height_mm columns to order_item_operations
-- ============================================================

ALTER TABLE svtables.order_item_operations
    ADD COLUMN IF NOT EXISTS width_mm NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS height_mm NUMERIC(12,2);
