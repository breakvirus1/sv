-- Добавление поля ready_date в order_materials
ALTER TABLE order_materials
ADD COLUMN ready_date DATE;
