-- Flyway migration V5: Seed initial materials for order service

CREATE TABLE IF NOT EXISTS svschema.materials (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(20),
    price NUMERIC(12,2) DEFAULT 0,
    waste_coefficient NUMERIC(5,3) DEFAULT 1.0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false
);

INSERT INTO svschema.materials (id, name, unit, price, waste_coefficient, created_at, updated_at, deleted) VALUES
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

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = 'svschema' AND sequencename = 'materials_id_seq') THEN
        PERFORM setval('svschema.materials_id_seq', COALESCE((SELECT MAX(id) FROM svschema.materials), 1));
    END IF;
END $$;
