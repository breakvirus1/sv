-- V15: Add file_id column to order_items for bidirectional @OneToOne with files table
ALTER TABLE order_items ADD COLUMN file_id BIGINT;
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_file FOREIGN KEY (file_id) REFERENCES files (id) ON DELETE SET NULL;
CREATE INDEX idx_order_items_file ON order_items(file_id);
