-- =============================================
-- Добавление поля quantity_formula в таблицу product_operations
-- Позволяет хранить формулу для расчёта количества операции (например, для люверсов)
-- =============================================
ALTER TABLE product_operations ADD COLUMN IF NOT EXISTS quantity_formula VARCHAR(500);