-- ============================================================
-- V20260507_2: Add order_material_operations table
-- Хранит операции, выполненные над материалом в заказе
-- ============================================================

CREATE TABLE IF NOT EXISTS ordschema.order_material_operations (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_material_id BIGINT NOT NULL,
    material_operation_id BIGINT,
    name VARCHAR(255) NOT NULL,
    operation_type VARCHAR(30),
    base_price NUMERIC(12,2),
    unit VARCHAR(20) DEFAULT 'шт',
    waste_coefficient NUMERIC(5,3) DEFAULT 1.0,
    quantity NUMERIC(12,4) DEFAULT 1,
    cost NUMERIC(12,2) DEFAULT 0,
    parameters JSONB,
    additional_materials JSONB,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_order_material_operations_order FOREIGN KEY (order_id) REFERENCES ordschema.orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_material_operations_material FOREIGN KEY (order_material_id) REFERENCES ordschema.order_materials (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_material_operations_template FOREIGN KEY (material_operation_id) REFERENCES ordschema.material_operations (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_order_material_operations_order ON ordschema.order_material_operations(order_id);
CREATE INDEX IF NOT EXISTS idx_order_material_operations_material ON ordschema.order_material_operations(order_material_id);
CREATE INDEX IF NOT EXISTS idx_order_material_operations_template ON ordschema.order_material_operations(material_operation_id);
