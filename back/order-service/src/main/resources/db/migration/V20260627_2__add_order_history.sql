-- Add history column to orders table for human-readable change log

ALTER TABLE svschema.orders ADD COLUMN IF NOT EXISTS history TEXT;
