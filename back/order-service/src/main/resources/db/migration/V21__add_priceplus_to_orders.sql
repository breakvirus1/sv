-- V12: add priceplus column to orders table
ALTER TABLE svtables.orders ADD COLUMN IF NOT EXISTS priceplus DECIMAL(10, 2);
