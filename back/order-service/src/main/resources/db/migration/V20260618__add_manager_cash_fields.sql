-- Add manager_cash_percent to employees table
ALTER TABLE employees ADD COLUMN manager_cash_percent NUMERIC(5,2);

-- Add cash_from_priceplus to orders table
ALTER TABLE orders ADD COLUMN cash_from_priceplus NUMERIC(12,2);
