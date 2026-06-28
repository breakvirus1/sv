ALTER TABLE IF EXISTS svschema.employees ADD COLUMN IF NOT EXISTS manager_cash_percent NUMERIC(5,2);

ALTER TABLE IF EXISTS svschema.orders ADD COLUMN IF NOT EXISTS cash_from_priceplus NUMERIC(12,2);
