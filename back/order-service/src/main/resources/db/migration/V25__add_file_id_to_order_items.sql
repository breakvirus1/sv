-- V15: Add file_id column to order_items for bidirectional @OneToOne with files table
ALTER TABLE svtables.order_items ADD COLUMN IF NOT EXISTS file_id BIGINT;
ALTER TABLE svtables.order_items ADD CONSTRAINT fk_order_items_file FOREIGN KEY (file_id) REFERENCES svtables.files (id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_order_items_file ON svtables.order_items(file_id);
