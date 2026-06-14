-- ============================================================
-- PrintSV Database Schema - V3
-- Adds material_operations table for material-associated operation templates
-- ============================================================

-- ----------------------------
-- 13. Material Operations table (reference operations for materials)
-- ----------------------------
CREATE TABLE IF NOT EXISTS svtables.material_operations (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    price_per_unit NUMERIC(12,2) DEFAULT 0,
    norm_seconds BIGINT DEFAULT NULL,
    unit VARCHAR(20) DEFAULT 'шт',
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_material_operations_material FOREIGN KEY (material_id)
        REFERENCES svtables.materials (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_material_operations_material ON svtables.material_operations(material_id);

-- Ensure active column exists (for tables created before this migration was updated)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'svtables' AND table_name = 'material_operations' AND column_name = 'active'
    ) THEN
        ALTER TABLE svtables.material_operations ADD COLUMN active BOOLEAN DEFAULT true;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_material_operations_active ON svtables.material_operations(active);
