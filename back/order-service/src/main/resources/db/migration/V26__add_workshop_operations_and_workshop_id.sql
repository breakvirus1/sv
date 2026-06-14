-- ============================================================
-- PrintSV Database Schema - Workshop Operations & Workshop Visibility (V16)
-- Adds workshop_operations table, workshop_materials table, workshop_id to employees and orders
-- ============================================================

-- Workshop operations (list of operation IDs per workshop)
CREATE TABLE IF NOT EXISTS svtables.workshop_operations (
    workshop_id BIGINT NOT NULL,
    operation_id BIGINT NOT NULL,
    CONSTRAINT fk_workshop_operations_workshop FOREIGN KEY (workshop_id)
        REFERENCES svtables.workshops (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_workshop_operations_workshop ON svtables.workshop_operations(workshop_id);

CREATE TABLE IF NOT EXISTS svtables.workshop_materials (
    workshop_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    CONSTRAINT fk_workshop_materials_workshop FOREIGN KEY (workshop_id)
        REFERENCES svtables.workshops (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_workshop_materials_workshop ON svtables.workshop_materials(workshop_id);

ALTER TABLE svtables.employees ADD COLUMN IF NOT EXISTS workshop_id BIGINT;

ALTER TABLE svtables.orders ADD COLUMN IF NOT EXISTS workshop_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_orders_workshop ON svtables.orders(workshop_id);
