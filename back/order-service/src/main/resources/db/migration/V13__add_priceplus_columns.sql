-- V13: add cost_priceplus to order_materials and total_with_priceplus to orders
ALTER TABLE order_materials ADD COLUMN IF NOT EXISTS cost_priceplus DECIMAL(12, 2) DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS total_with_priceplus DECIMAL(12, 2) DEFAULT 0;