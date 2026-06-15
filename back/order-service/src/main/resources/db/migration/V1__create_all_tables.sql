-- ============================================================
-- PrintSV Database Schema - Complete Migration (V1)
-- All tables for microservices shared database
-- ============================================================

CREATE SCHEMA IF NOT EXISTS ordschema;

-- ----------------------------
-- 1. Clients table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50),
    contact_person VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(255),
    inn VARCHAR(100),
    address VARCHAR(255),
    priceplus DECIMAL(10, 2),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_clients_name ON ordschema.clients(name);

-- ----------------------------
-- 2. Employees table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.employees (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL UNIQUE,
    position VARCHAR(100),
    phone VARCHAR(50),
    email VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_employees_username ON ordschema.employees(username);

-- ----------------------------
-- 3. Workshops table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.workshops (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false
);

-- ----------------------------
-- 4. Materials table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.materials (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20),
    unit VARCHAR(20),
    price NUMERIC(12,2) DEFAULT 0,
    waste_coefficient NUMERIC(5,3) DEFAULT 1.0,
    default_width_m DECIMAL(10,4) DEFAULT 0,
    default_height_m DECIMAL(10,4) DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false
);

-- ----------------------------
-- 5. Roles table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL
);

-- ----------------------------
-- 6. Orders table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    client_id BIGINT NOT NULL,
    description TEXT,
    total_amount NUMERIC(12,2) DEFAULT 0,
    paid_amount NUMERIC(12,2) DEFAULT 0,
    debt_amount NUMERIC(12,2) DEFAULT 0,
    status VARCHAR(30),
    production_stage VARCHAR(50),
    order_date DATE,
    due_date DATE,
    manager_id BIGINT,
    workshop_id BIGINT,
    launched_at TIMESTAMP,
    ready_at TIMESTAMP,
    accepted_at TIMESTAMP,
    closed_at TIMESTAMP,
    has_documents BOOLEAN DEFAULT false,
    priceplus DECIMAL(10,2) DEFAULT 0,
    total_with_priceplus NUMERIC(12,2) DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_orders_client FOREIGN KEY (client_id) REFERENCES ordschema.clients (id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_manager FOREIGN KEY (manager_id) REFERENCES ordschema.employees (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_orders_status ON ordschema.orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_due_date ON ordschema.orders(due_date);
CREATE INDEX IF NOT EXISTS idx_orders_client ON ordschema.orders(client_id);
CREATE INDEX IF NOT EXISTS idx_orders_manager ON ordschema.orders(manager_id);

-- ----------------------------
-- 7. Order Items table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(12,2) DEFAULT 0,
    quantity INTEGER DEFAULT 1,
    cost NUMERIC(12,2) DEFAULT 0,
    ready_date DATE,
    file_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES ordschema.orders (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_items_order ON ordschema.order_items(order_id);

-- ----------------------------
-- 8. Order Stages table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.order_stages (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    workshop_id BIGINT NOT NULL,
    wait_previous BOOLEAN DEFAULT true,
    due_date DATE,
    note VARCHAR(500),
    status VARCHAR(50) DEFAULT 'TODO',
    source_files TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_order_stages_order FOREIGN KEY (order_id) REFERENCES ordschema.orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_stages_workshop FOREIGN KEY (workshop_id) REFERENCES ordschema.workshops (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_order_stages_order ON ordschema.order_stages(order_id);
CREATE INDEX IF NOT EXISTS idx_order_stages_workshop ON ordschema.order_stages(workshop_id);

-- ----------------------------
-- 9. Order Materials table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.order_materials (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT,
    order_item_id BIGINT,
    material_id BIGINT NOT NULL,
    quantity NUMERIC(12,2) DEFAULT 0,
    waste_coefficient NUMERIC(5,3) DEFAULT 1.0,
    cost NUMERIC(12,2) DEFAULT 0,
    cost_priceplus NUMERIC(12,2) DEFAULT 0,
    eyelet_cost NUMERIC(12,2) DEFAULT 0,
    width_m DECIMAL(10,4),
    height_m DECIMAL(10,4),
    ready_date DATE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_order_materials_order FOREIGN KEY (order_id) REFERENCES ordschema.orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_materials_item FOREIGN KEY (order_item_id) REFERENCES ordschema.order_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_materials_material FOREIGN KEY (material_id) REFERENCES ordschema.materials (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_order_materials_order ON ordschema.order_materials(order_id);
CREATE INDEX IF NOT EXISTS idx_order_materials_item ON ordschema.order_materials(order_item_id);
CREATE INDEX IF NOT EXISTS idx_order_materials_material ON ordschema.order_materials(material_id);

-- ----------------------------
-- 10. Order Item Operations table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.order_item_operations (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    operation_id BIGINT NOT NULL,
    operation_name VARCHAR(255) NOT NULL,
    price_per_unit NUMERIC(12,2) DEFAULT 0,
    calculated_quantity NUMERIC(12,4) DEFAULT 0,
    subtotal NUMERIC(12,2) DEFAULT 0,
    width_m DECIMAL(10,4),
    height_m DECIMAL(10,4),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_order_item_operations_item FOREIGN KEY (order_item_id) REFERENCES ordschema.order_items (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_item_operations_item ON ordschema.order_item_operations(order_item_id);

-- ----------------------------
-- 11. Payments table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    payment_date DATE NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    payment_type VARCHAR(50),
    details VARCHAR(100),
    is_partial BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES ordschema.orders (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_payments_order ON ordschema.payments(order_id);

-- ----------------------------
-- 12. Order Comments table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.order_comments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    author_id BIGINT,
    message TEXT,
    is_internal BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_order_comments_order FOREIGN KEY (order_id) REFERENCES ordschema.orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_comments_author FOREIGN KEY (author_id) REFERENCES ordschema.employees (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_order_comments_order ON ordschema.order_comments(order_id);

-- ----------------------------
-- 13. Files table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.files (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255),
    file_path VARCHAR(500) NOT NULL,
    file_url VARCHAR(500),
    mime_type VARCHAR(100),
    file_size BIGINT,
    order_id BIGINT,
    order_item_id BIGINT,
    uploaded_by VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_files_order FOREIGN KEY (order_id) REFERENCES ordschema.orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_files_order_item FOREIGN KEY (order_item_id) REFERENCES ordschema.order_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_files_order_item_file FOREIGN KEY (order_item_id) REFERENCES ordschema.order_items (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_files_order ON ordschema.files(order_id);
CREATE INDEX IF NOT EXISTS idx_files_order_item ON ordschema.files(order_item_id);

-- ----------------------------
-- 14. Material Operations table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.material_operations (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    operation_type VARCHAR(30) NOT NULL,
    quantity_formula VARCHAR(500),
    base_price NUMERIC(12,2) DEFAULT 0,
    unit VARCHAR(20) DEFAULT 'шт',
    waste_coefficient NUMERIC(5,3) DEFAULT 1.0,
    requires_dimensions BOOLEAN DEFAULT false,
    allows_additional_materials BOOLEAN DEFAULT false,
    sort_order INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_material_operations_material FOREIGN KEY (material_id) REFERENCES ordschema.materials (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_material_operations_material ON ordschema.material_operations(material_id);

-- ----------------------------
-- 15. Operation Parameters table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.operation_parameters (
    id BIGSERIAL PRIMARY KEY,
    operation_id BIGINT NOT NULL,
    param_key VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    type VARCHAR(20) DEFAULT 'NUMBER',
    unit VARCHAR(20) DEFAULT '',
    default_value VARCHAR(255) DEFAULT '',
    required BOOLEAN DEFAULT false,
    sort_order INTEGER DEFAULT 0,
    description VARCHAR(500) DEFAULT '',
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_operation_parameters_operation FOREIGN KEY (operation_id) REFERENCES ordschema.material_operations (id) ON DELETE CASCADE
);

-- ----------------------------
-- 16. Operation Additional Materials table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.operation_additional_materials (
    id BIGSERIAL PRIMARY KEY,
    operation_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    default_quantity NUMERIC(12,4) DEFAULT 1,
    unit VARCHAR(20) DEFAULT 'шт',
    price_per_unit NUMERIC(12,2),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_oam_operation FOREIGN KEY (operation_id) REFERENCES ordschema.material_operations (id) ON DELETE CASCADE,
    CONSTRAINT fk_oam_material FOREIGN KEY (material_id) REFERENCES ordschema.materials (id) ON DELETE RESTRICT
);

-- ----------------------------
-- 17. Workshop Operations table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.workshop_operations (
    id BIGSERIAL PRIMARY KEY,
    workshop_id BIGINT NOT NULL,
    operation_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_workshop_operations_workshop FOREIGN KEY (workshop_id) REFERENCES ordschema.workshops (id) ON DELETE CASCADE
);

-- ----------------------------
-- 18. Workshop Materials table
-- ----------------------------
CREATE TABLE IF NOT EXISTS ordschema.workshop_materials (
    id BIGSERIAL PRIMARY KEY,
    workshop_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_workshop_materials_workshop FOREIGN KEY (workshop_id) REFERENCES ordschema.workshops (id) ON DELETE CASCADE
);
