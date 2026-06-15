-- Flyway migration V4: Seed initial materials for order service

-- Insert materials (same as calculator materials)
INSERT INTO ordschema.materials (id, name, unit, price, waste_coefficient, created_at, updated_at, deleted) VALUES
(1, 'Баннер Frontlit 440 г/м²', 'м2', 250.00, 1.100, NOW(), NOW(), false),
(2, 'Баннер Blockout 510 г/м²', 'м2', 280.00, 1.100, NOW(), NOW(), false),
(3, 'Плёнка monomer 80 мкм', 'м2', 200.00, 1.050, NOW(), NOW(), false),
(4, 'Плёнка cast 100 мкм', 'м2', 600.00, 1.050, NOW(), NOW(), false)
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  unit = EXCLUDED.unit,
  price = EXCLUDED.price,
  waste_coefficient = EXCLUDED.waste_coefficient,
  updated_at = EXCLUDED.updated_at,
  deleted = EXCLUDED.deleted;

-- Reset sequence
SELECT setval('ordschema.materials_id_seq', (SELECT MAX(id) FROM ordschema.materials));
