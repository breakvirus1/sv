-- Flyway migration V3: Seed initial data for calculator-service
-- Note: Materials are now in svschema.materials (shared table), seed references those IDs

-- Sample eyelets
INSERT INTO svschema.calculator_eyelets (name, price_per_piece, diameter_mm, created_at) VALUES
('Люверс 8 мм', 12.00, 8, NOW()),
('Люверс 10 мм', 15.00, 10, NOW()),
('Люверс 12 мм', 18.00, 12, NOW())
ON CONFLICT DO NOTHING;

-- Sample operations
INSERT INTO svschema.calculator_operations (name, price, unit_type, applicable_to, is_default, created_at) VALUES
('Печать 360 dpi', 180.00, 'SQUARE_METER', 'BOTH', TRUE, NOW()),
('Печать 720 dpi', 220.00, 'SQUARE_METER', 'BOTH', TRUE, NOW()),
('Печать 1440 dpi', 280.00, 'SQUARE_METER', 'BOTH', FALSE, NOW()),
('Порезка по периметру', 35.00, 'LINEAR_METER', 'BOTH', TRUE, NOW()),
('Подворот', 45.00, 'LINEAR_METER', 'BANNER', TRUE, NOW()),
('Установка люверсов', 8.00, 'PIECE', 'BANNER', TRUE, NOW()),
('Проклейка / Сварка', 50.00, 'LINEAR_METER', 'BANNER', TRUE, NOW()),
('Ламинация глянцевая', 220.00, 'SQUARE_METER', 'PLENKA', TRUE, NOW()),
('Ламинация матовая', 200.00, 'SQUARE_METER', 'PLENKA', FALSE, NOW()),
('Контурная резка', 45.00, 'LINEAR_METER', 'PLENKA', TRUE, NOW()),
('Поклейка', 150.00, 'SQUARE_METER', 'PLENKA', TRUE, NOW())
ON CONFLICT DO NOTHING;
