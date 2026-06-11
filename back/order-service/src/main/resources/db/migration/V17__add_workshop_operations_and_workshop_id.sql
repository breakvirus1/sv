-- ============================================================
-- PrintSV Database Schema - Workshop Operations & Workshop Visibility (V16)
-- Adds workshop_operations table, workshop_materials table, workshop_id to employees and orders
-- ============================================================

-- Workshop operations (list of operation IDs per workshop)
CREATE TABLE IF NOT EXISTS workshop_operations (
    workshop_id BIGINT NOT NULL,
    operation_id BIGINT NOT NULL,
    CONSTRAINT fk_workshop_operations_workshop FOREIGN KEY (workshop_id)
        REFERENCES workshops (id) ON DELETE CASCADE
);

CREATE INDEX idx_workshop_operations_workshop ON workshop_operations(workshop_id);

-- Workshop materials (list of material IDs per workshop)
CREATE TABLE IF NOT EXISTS workshop_materials (
    workshop_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    CONSTRAINT fk_workshop_materials_workshop FOREIGN KEY (workshop_id)
        REFERENCES workshops (id) ON DELETE CASCADE
);

CREATE INDEX idx_workshop_materials_workshop ON workshop_materials(workshop_id);

-- Add workshop_id to employees
ALTER TABLE employees ADD COLUMN IF NOT EXISTS workshop_id BIGINT;

-- Add workshop_id to orders
ALTER TABLE orders ADD COLUMN IF NOT EXISTS workshop_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_orders_workshop ON orders(workshop_id);
