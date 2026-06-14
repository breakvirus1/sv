-- Добавление поля ready_date в order_materials
ALTER TABLE svtables.order_materials
ADD COLUMN IF NOT EXISTS ready_date DATE;
