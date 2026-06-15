-- Добавление поля ready_date в order_materials
ALTER TABLE ordschema.order_materials
ADD COLUMN IF NOT EXISTS ready_date DATE;
