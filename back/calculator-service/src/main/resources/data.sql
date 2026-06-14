-- Sample materials (BANNER and PLENKA)
INSERT INTO calculator.calculator_materials (name, price_per_square_meter, waste_coefficient, material_type, created_at) VALUES
('Баннер Frontlit 440 г/м²', 250.00, 1.10, 'BANNER', NOW()),
('Баннер Blockout 510 г/м²', 280.00, 1.10, 'BANNER', NOW()),
('Плёнка monomer 80 мкм', 200.00, 1.05, 'PLENKA', NOW()),
('Плёнка cast 100 мкм', 600.00, 1.05, 'PLENKA', NOW());

-- Sample eyelets
INSERT INTO calculator.calculator_eyelets (name, price_per_piece, diameter_mm, created_at) VALUES
('Люверс 8 мм', 12.00, 8, NOW()),
('Люверс 10 мм', 15.00, 10, NOW());

-- Sample operations
-- Printing operations (applicable to both)
INSERT INTO calculator.calculator_operations (name, price, unit_type, applicable_to, is_default, created_at) VALUES
('Печать 360 dpi', 180.00, 'SQUARE_METER', 'BOTH', TRUE, NOW()),
('Печать 720 dpi', 220.00, 'SQUARE_METER', 'BOTH', TRUE, NOW()),
('Печать 1440 dpi', 280.00, 'SQUARE_METER', 'BOTH', FALSE, NOW()),
('Порезка по периметру', 35.00, 'LINEAR_METER', 'BOTH', TRUE, NOW());

-- Banner-specific operations
INSERT INTO calculator.calculator_operations (name, price, unit_type, applicable_to, is_default, created_at) VALUES
('Подворот', 45.00, 'LINEAR_METER', 'BANNER', TRUE, NOW()),
('Установка люверсов', 8.00, 'PIECE', 'BANNER', TRUE, NOW()),
('Проклейка / Сварка', 50.00, 'LINEAR_METER', 'BANNER', TRUE, NOW());

-- Film-specific operations
INSERT INTO calculator.calculator_operations (name, price, unit_type, applicable_to, is_default, created_at) VALUES
('Ламинация глянцевая', 220.00, 'SQUARE_METER', 'PLENKA', TRUE, NOW()),
('Ламинация матовая', 200.00, 'SQUARE_METER', 'PLENKA', FALSE, NOW()),
('Контурная резка', 45.00, 'LINEAR_METER', 'PLENKA', TRUE, NOW()),
('Поклейка', 150.00, 'SQUARE_METER', 'PLENKA', TRUE, NOW());
