-- =============================================
-- V20250506: Add fields for dynamic calculator
-- =============================================

-- 1. Add formula_json, category, is_active to products table
ALTER TABLE svschema.products
ADD COLUMN IF NOT EXISTS formula_json JSONB,
ADD COLUMN IF NOT EXISTS category VARCHAR(100),
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- 2. Add quantity_formula to product_materials table
ALTER TABLE svschema.product_materials
ADD COLUMN IF NOT EXISTS quantity_formula VARCHAR(500);

-- 3. Add params JSONB, width, height to order_items table
ALTER TABLE svschema.order_items
ADD COLUMN IF NOT EXISTS params JSONB,
ADD COLUMN IF NOT EXISTS width NUMERIC(10,3),
ADD COLUMN IF NOT EXISTS height NUMERIC(10,3);

-- 4. Add stock fields to materials table
ALTER TABLE svschema.materials
ADD COLUMN IF NOT EXISTS article VARCHAR(100) UNIQUE,
ADD COLUMN IF NOT EXISTS supplier VARCHAR(150),
ADD COLUMN IF NOT EXISTS current_stock NUMERIC(12,4) DEFAULT 0,
ADD COLUMN IF NOT EXISTS min_stock NUMERIC(12,4);

-- 5. Add cost_price and margin_percent to orders table
ALTER TABLE svschema.orders
ADD COLUMN IF NOT EXISTS cost_price NUMERIC(15,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS margin_percent NUMERIC(8,2);
