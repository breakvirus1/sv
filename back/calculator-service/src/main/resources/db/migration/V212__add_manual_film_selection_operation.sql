-- Add manual film selection operation
INSERT INTO svschema.calculator_operations (name, price, unit_type, is_default, created_at)
VALUES ('Ручная выборка пленки', 100.00, 'SQUARE_METER', TRUE, NOW())
ON CONFLICT DO NOTHING;
