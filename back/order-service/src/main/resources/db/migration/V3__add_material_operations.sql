-- ============================================================
-- PrintSV Database Schema - V3
-- Adds material_operations table for material-associated operation templates
-- ============================================================

-- ----------------------------
-- 13. Material Operations table (reference operations for materials)
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.material_operations (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    price_per_unit NUMERIC(12,2) DEFAULT 0,
    norm_seconds BIGINT DEFAULT NULL,
    unit VARCHAR(20) DEFAULT 'шт',
    sort_order INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_material_operations_material FOREIGN KEY (material_id)
        REFERENCES ordschema.materials (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_material_operations_material ON ordschema.material_operations(material_id);
CREATE INDEX IF NOT EXISTS idx_material_operations_active ON ordschema.material_operations(active);
