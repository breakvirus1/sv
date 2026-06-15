-- =============================================
-- Продукты (шаблоны изделий)
-- =============================================
CREATE TABLE IF NOT EXISTS ordschema.products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    article VARCHAR(100),
    description TEXT,
    width NUMERIC(10,3),
    height NUMERIC(10,3),
    unit VARCHAR(20) DEFAULT 'шт',
    base_price NUMERIC(12,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- Материалы в шаблоне изделия
CREATE TABLE IF NOT EXISTS ordschema.product_materials (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    quantity NUMERIC(12,4) NOT NULL,
    waste_coefficient NUMERIC(5,3) DEFAULT 1.000,
    sort_order INT,
    FOREIGN KEY (product_id) REFERENCES ordschema.products(id) ON DELETE CASCADE,
    FOREIGN KEY (material_id) REFERENCES ordschema.materials(id) ON DELETE RESTRICT
);

-- Операции/работы в шаблоне изделия
CREATE TABLE IF NOT EXISTS ordschema.product_operations (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    price_per_unit NUMERIC(12,2) NOT NULL,
    norm_time INTERVAL,
    unit VARCHAR(50) DEFAULT 'шт',
    sort_order INT,
    FOREIGN KEY (product_id) REFERENCES ordschema.products(id) ON DELETE CASCADE
);

-- Материалы в конкретной позиции заказа (с возможностью переопределения)
CREATE TABLE IF NOT EXISTS ordschema.order_item_materials (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    quantity NUMERIC(12,4) NOT NULL,
    waste_coefficient NUMERIC(5,3) DEFAULT 1.000,
    cost NUMERIC(12,2),
    FOREIGN KEY (order_item_id) REFERENCES ordschema.order_items(id) ON DELETE CASCADE,
    FOREIGN KEY (material_id) REFERENCES ordschema.materials(id)
);

-- Операции в конкретной позиции заказа
CREATE TABLE IF NOT EXISTS ordschema.order_item_operations (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    price_per_unit NUMERIC(12,2) NOT NULL,
    norm_time INTERVAL,
    quantity NUMERIC(12,4) DEFAULT 1,
    cost NUMERIC(12,2),
    FOREIGN KEY (order_item_id) REFERENCES ordschema.order_items(id) ON DELETE CASCADE
);

-- Добавляем ссылку на продукт в позицию заказа
ALTER TABLE ordschema.order_items ADD COLUMN IF NOT EXISTS product_id BIGINT REFERENCES ordschema.products(id);
