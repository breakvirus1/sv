-- Flyway migration V3: Seed initial data for calculator-service
-- Note: Materials are now in svschema.materials (shared table), seed references those IDs

-- Sample eyelets
INSERT INTO svschema.calculator_eyelets (name, price_per_piece, diameter_mm, created_at) VALUES
('Люверс 8 мм', 12.00, 8, NOW()),
('Люверс 10 мм', 15.00, 10, NOW()),
('Люверс 12 мм', 18.00, 12, NOW())
ON CONFLICT DO NOTHING;
