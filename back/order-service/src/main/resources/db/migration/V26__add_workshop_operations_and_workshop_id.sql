-- ============================================================
-- PrintSV Database Schema - Workshop Operations & Workshop Visibility (V16)
-- Adds workshop_operations table, workshop_materials table, workshop_id to employees and orders
-- ============================================================

-- Workshop operations (list of operation IDs per workshop)
CREATE TABLE IF NOT EXISTS ordschema.workshop_operations (
    workshop_id BIGINT NOT NULL,
    operation_id BIGINT NOT NULL,
    CONSTRAINT fk_workshop_operations_workshop FOREIGN KEY (workshop_id)
        REFERENCES ordschema.workshops (id) ON DELETE CASCADE
);

CREATE INDEX idx_workshop_operations_workshop ON ordschema.workshop_operations(workshop_id);

CREATE TABLE IF NOT EXISTS ordschema.workshop_materials (
    workshop_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    CONSTRAINT fk_workshop_materials_workshop FOREIGN KEY (workshop_id)
        REFERENCES ordschema.workshops (id) ON DELETE CASCADE
);

CREATE INDEX idx_workshop_materials_workshop ON ordschema.workshop_materials(workshop_id);

ALTER TABLE ordschema.employees ADD COLUMN IF NOT EXISTS workshop_id BIGINT;

ALTER TABLE ordschema.orders ADD COLUMN IF NOT EXISTS workshop_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_orders_workshop ON ordschema.orders(workshop_id);
