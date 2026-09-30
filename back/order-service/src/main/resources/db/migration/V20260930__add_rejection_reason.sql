-- Add rejection_reason to orders
ALTER TABLE svschema.orders ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
