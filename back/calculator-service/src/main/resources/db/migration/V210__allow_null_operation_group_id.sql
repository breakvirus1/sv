-- Allow operation_group_id to be NULL in material_operation_groups
-- This enables operations without a group to be stored

ALTER TABLE svschema.material_operation_groups
    DROP CONSTRAINT IF EXISTS fk_mog_group;

ALTER TABLE svschema.material_operation_groups
    ALTER COLUMN operation_group_id DROP NOT NULL;

ALTER TABLE svschema.material_operation_groups
    ADD CONSTRAINT fk_mog_group FOREIGN KEY (operation_group_id)
        REFERENCES svschema.operation_groups (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_mog_group_nullable ON svschema.material_operation_groups(operation_group_id) WHERE operation_group_id IS NOT NULL;
