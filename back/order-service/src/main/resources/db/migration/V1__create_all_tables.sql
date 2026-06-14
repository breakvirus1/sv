-- ============================================================
-- PrintSV Database Schema - Complete Migration (V1)
-- All tables for microservices shared database
-- ============================================================

-- Create schema first
CREATE SCHEMA IF NOT EXISTS svtables;

-- ----------------------------
-- 1. Clients table
-- ----------------------------
CREATE TABLE IF NOT EXISTS svtables.clients (
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

CREATE INDEX IF NOT EXISTS idx_clients_name ON svtables.clients(name);

-- ----------------------------
-- 2. Employees table
-- ----------------------------
CREATE TABLE IF NOT EXISTS svtables.employees (
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

CREATE INDEX IF NOT EXISTS idx_employees_username ON svtables.employees(username);

-- ----------------------------
-- 3. Workshops table
-- ----------------------------
CREATE TABLE IF NOT EXISTS svtables.workshops (
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
CREATE TABLE IF NOT EXISTS svtables.materials (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(20),
    price NUMERIC(12,2) DEFAULT 0,
    waste_coefficient NUMERIC(5,3) DEFAULT 1.0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false
);

-- ----------------------------
-- 5. Roles table
-- ----------------------------
CREATE TABLE IF NOT EXISTS svtables.roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL
);

-- ----------------------------
-- 6. Orders table
-- ----------------------------
CREATE TABLE IF NOT EXISTS svtables.orders (
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
    launched_at TIMESTAMP,
    ready_at TIMESTAMP,
    accepted_at TIMESTAMP,
    closed_at TIMESTAMP,
    has_documents BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_orders_client FOREIGN KEY (client_id) REFERENCES svtables.clients (id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_manager FOREIGN KEY (manager_id) REFERENCES svtables.employees (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_orders_status ON svtables.orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_due_date ON svtables.orders(due_date);
CREATE INDEX IF NOT EXISTS idx_orders_client ON svtables.orders(client_id);
CREATE INDEX IF NOT EXISTS idx_orders_manager ON svtables.orders(manager_id);

-- ----------------------------
-- 7. Order Items table
-- ----------------------------
CREATE TABLE IF NOT EXISTS svtables.order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(12,2) DEFAULT 0,
    quantity INTEGER DEFAULT 1,
    cost NUMERIC(12,2) DEFAULT 0,
    ready_date DATE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES svtables.orders (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_items_order ON svtables.order_items(order_id);

-- ----------------------------
-- 8. Order Stages table
-- ----------------------------
CREATE TABLE IF NOT EXISTS svtables.order_stages (
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
    CONSTRAINT fk_order_stages_order FOREIGN KEY (order_id) REFERENCES svtables.orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_stages_workshop FOREIGN KEY (workshop_id) REFERENCES svtables.workshops (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_order_stages_order ON svtables.order_stages(order_id);
CREATE INDEX IF NOT EXISTS idx_order_stages_workshop ON svtables.order_stages(workshop_id);

-- ----------------------------
-- 9. Order Materials table
-- ----------------------------
CREATE TABLE IF NOT EXISTS svtables.order_materials (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT,
    order_item_id BIGINT,
    material_id BIGINT NOT NULL,
    quantity NUMERIC(12,2) DEFAULT 0,
    waste_coefficient NUMERIC(5,3) DEFAULT 1.0,
    cost NUMERIC(12,2) DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_order_materials_order FOREIGN KEY (order_id) REFERENCES svtables.orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_materials_item FOREIGN KEY (order_item_id) REFERENCES svtables.order_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_materials_material FOREIGN KEY (material_id) REFERENCES svtables.materials (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_order_materials_order ON svtables.order_materials(order_id);
CREATE INDEX IF NOT EXISTS idx_order_materials_item ON svtables.order_materials(order_item_id);
CREATE INDEX IF NOT EXISTS idx_order_materials_material ON svtables.order_materials(material_id);

-- ----------------------------
-- 10. Payments table
-- ----------------------------
CREATE TABLE IF NOT EXISTS svtables.payments (
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
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES svtables.orders (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_payments_order ON svtables.payments(order_id);

-- ----------------------------
-- 11. Order Comments table
-- ----------------------------
CREATE TABLE IF NOT EXISTS svtables.order_comments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    author_id BIGINT,
    message TEXT,
    is_internal BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_order_comments_order FOREIGN KEY (order_id) REFERENCES svtables.orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_comments_author FOREIGN KEY (author_id) REFERENCES svtables.employees (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_order_comments_order ON svtables.order_comments(order_id);

-- ----------------------------
-- 12. Files table
-- ----------------------------
CREATE TABLE IF NOT EXISTS svtables.files (
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
    CONSTRAINT fk_files_order FOREIGN KEY (order_id) REFERENCES svtables.orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_files_order_item FOREIGN KEY (order_item_id) REFERENCES svtables.order_items (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_files_order ON svtables.files(order_id);
CREATE INDEX IF NOT EXISTS idx_files_order_item ON svtables.files(order_item_id);
