-- Create material_operation_groups linking table
-- Links materials to operations through operation groups

CREATE TABLE IF NOT EXISTS svschema.material_operation_groups (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL,
    operation_group_id BIGINT NOT NULL,
    operation_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_mog_material FOREIGN KEY (material_id)
        REFERENCES svschema.materials (id) ON DELETE CASCADE,
    CONSTRAINT fk_mog_group FOREIGN KEY (operation_group_id)
        REFERENCES svschema.operation_groups (id) ON DELETE CASCADE,
    CONSTRAINT fk_mog_operation FOREIGN KEY (operation_id)
        REFERENCES svschema.calculator_operations (id) ON DELETE CASCADE,
    CONSTRAINT uq_material_group_operation UNIQUE (material_id, operation_group_id, operation_id)
);

CREATE INDEX IF NOT EXISTS idx_mog_material ON svschema.material_operation_groups(material_id);
CREATE INDEX IF NOT EXISTS idx_mog_group ON svschema.material_operation_groups(operation_group_id);
CREATE INDEX IF NOT EXISTS idx_mog_operation ON svschema.material_operation_groups(operation_id);
CREATE INDEX IF NOT EXISTS idx_mog_material_group ON svschema.material_operation_groups(material_id, operation_group_id);
