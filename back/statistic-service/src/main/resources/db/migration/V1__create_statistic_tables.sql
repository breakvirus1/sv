CREATE SCHEMA IF NOT EXISTS svschema;

CREATE TABLE IF NOT EXISTS svschema.statistic_orders (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_number VARCHAR(50) NOT NULL,
    order_date DATE,
    client_id BIGINT,
    client_name VARCHAR(255),
    total_amount NUMERIC(12,2) DEFAULT 0,
    priceplus NUMERIC(10,2) DEFAULT 0,
    total_with_priceplus NUMERIC(12,2) DEFAULT 0,
    status VARCHAR(30),
    production_stage VARCHAR(50),
    manager_id BIGINT,
    manager_name VARCHAR(255),
    synced_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_statistic_orders_order_id ON svschema.statistic_orders(order_id);
CREATE INDEX IF NOT EXISTS idx_statistic_orders_order_date ON svschema.statistic_orders(order_date);

CREATE TABLE IF NOT EXISTS svschema.statistic_order_items (
    id BIGSERIAL PRIMARY KEY,
    statistic_order_id BIGINT NOT NULL,
    order_item_id BIGINT,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(12,2) DEFAULT 0,
    quantity INTEGER DEFAULT 1,
    cost NUMERIC(12,2) DEFAULT 0,
    ready_date DATE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_statistic_order_items_order FOREIGN KEY (statistic_order_id) REFERENCES svschema.statistic_orders (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_statistic_order_items_order ON svschema.statistic_order_items(statistic_order_id);
CREATE INDEX IF NOT EXISTS idx_statistic_order_items_order_item_id ON svschema.statistic_order_items(order_item_id);

CREATE TABLE IF NOT EXISTS svschema.statistic_material_consumptions (
    id BIGSERIAL PRIMARY KEY,
    statistic_order_item_id BIGINT NOT NULL,
    order_material_id BIGINT,
    material_id BIGINT NOT NULL,
    material_name VARCHAR(255) NOT NULL,
    material_unit VARCHAR(20),
    material_price NUMERIC(12,2) DEFAULT 0,
    waste_coefficient NUMERIC(5,3) DEFAULT 1.0,
    quantity NUMERIC(12,2) DEFAULT 0,
    width_m DECIMAL(10,4),
    height_m DECIMAL(10,4),
    net_quantity NUMERIC(12,2) DEFAULT 0,
    cost NUMERIC(12,2) DEFAULT 0,
    cost_priceplus NUMERIC(12,2) DEFAULT 0,
    eyelet_cost NUMERIC(12,2) DEFAULT 0,
    order_client_priceplus NUMERIC(10,2) DEFAULT 0,
    consumption_with_priceplus NUMERIC(12,2) DEFAULT 0,
    synced_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_statistic_material_consumptions_item FOREIGN KEY (statistic_order_item_id) REFERENCES svschema.statistic_order_items (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_statistic_material_consumptions_item ON svschema.statistic_material_consumptions(statistic_order_item_id);
CREATE INDEX IF NOT EXISTS idx_statistic_material_consumptions_material ON svschema.statistic_material_consumptions(material_id);
CREATE INDEX IF NOT EXISTS idx_statistic_material_consumptions_order_material ON svschema.statistic_material_consumptions(order_material_id);
