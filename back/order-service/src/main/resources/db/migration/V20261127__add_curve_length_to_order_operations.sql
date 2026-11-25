-- Длина кривой для операций резки (заменяет расчёт по периметру)
ALTER TABLE svschema.order_item_operations ADD COLUMN IF NOT EXISTS curve_length_m DECIMAL(10,4);