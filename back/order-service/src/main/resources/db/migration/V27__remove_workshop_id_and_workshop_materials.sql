-- V17: Remove workshop_id from orders, remove workshop_materials table
-- Orders no longer have a direct workshop reference — workshop is determined by operations in order items

ALTER TABLE IF EXISTS svtables.orders DROP COLUMN IF EXISTS workshop_id;

DROP TABLE IF EXISTS svtables.workshop_materials;
