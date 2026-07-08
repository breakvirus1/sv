CREATE TABLE IF NOT EXISTS svschema.calculator_material_operations (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL,
    operation_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_calc_mat_op_material FOREIGN KEY (material_id) REFERENCES svschema.materials (id),
    CONSTRAINT fk_calc_mat_op_operation FOREIGN KEY (operation_id) REFERENCES svschema.calculator_operations (id)
);

CREATE INDEX IF NOT EXISTS idx_calc_mat_op_material ON svschema.calculator_material_operations(material_id);
CREATE INDEX IF NOT EXISTS idx_calc_mat_op_operation ON svschema.calculator_material_operations(operation_id);
