-- ============================================================
-- PrintSV Database Schema - Order Operations Table (V3)
-- Adds order_item_operations table for order service
-- ============================================================

-- ----------------------------
-- 13. Order Operations table
-- ----------------------------
CREATE TABLE IF NOT EXISTS svschema.order_item_operations (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    operation_id BIGINT NOT NULL,
    operation_name VARCHAR(255) NOT NULL,
    price_per_unit NUMERIC(12,2),
    calculated_quantity NUMERIC(12,4),
    subtotal NUMERIC(12,2) DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_order_operations_order_item FOREIGN KEY (order_item_id)
        REFERENCES svschema.order_items (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_operations_order_item ON svschema.order_item_operations(order_item_id);
CREATE INDEX IF NOT EXISTS idx_order_operations_operation_id ON svschema.order_item_operations(operation_id);
