-- ============================================================
-- PrintSV Database Schema - Complete Migration (V1)
-- All tables for microservices shared database
-- ============================================================

-- ----------------------------
-- 1. Clients table
-- ----------------------------
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50),
    contact_person VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(255),
    inn VARCHAR(100),
    address VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false
);

CREATE INDEX idx_clients_name ON clients(name);

-- ----------------------------
-- 2. Employees table
-- ----------------------------
CREATE TABLE employees (
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

CREATE INDEX idx_employees_username ON employees(username);

-- ----------------------------
-- 3. Workshops table
-- ----------------------------
CREATE TABLE workshops (
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
CREATE TABLE materials (
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
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL
);

-- ----------------------------
-- 6. Orders table
-- ----------------------------
CREATE TABLE orders (
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
    CONSTRAINT fk_orders_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_manager FOREIGN KEY (manager_id) REFERENCES employees (id) ON DELETE SET NULL
);

CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_due_date ON orders(due_date);
CREATE INDEX idx_orders_client ON orders(client_id);
CREATE INDEX idx_orders_manager ON orders(manager_id);

-- ----------------------------
-- 7. Order Items table
-- ----------------------------
CREATE TABLE order_items (
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
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

CREATE INDEX idx_order_items_order ON order_items(order_id);

-- ----------------------------
-- 8. Order Stages table
-- ----------------------------
CREATE TABLE order_stages (
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
    CONSTRAINT fk_order_stages_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_stages_workshop FOREIGN KEY (workshop_id) REFERENCES workshops (id) ON DELETE RESTRICT
);

CREATE INDEX idx_order_stages_order ON order_stages(order_id);
CREATE INDEX idx_order_stages_workshop ON order_stages(workshop_id);

-- ----------------------------
-- 9. Order Materials table
-- ----------------------------
CREATE TABLE order_materials (
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
    CONSTRAINT fk_order_materials_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_materials_item FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_materials_material FOREIGN KEY (material_id) REFERENCES materials (id) ON DELETE RESTRICT
);

CREATE INDEX idx_order_materials_order ON order_materials(order_id);
CREATE INDEX idx_order_materials_item ON order_materials(order_item_id);
CREATE INDEX idx_order_materials_material ON order_materials(material_id);

-- ----------------------------
-- 10. Payments table
-- ----------------------------
CREATE TABLE payments (
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
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

CREATE INDEX idx_payments_order ON payments(order_id);

-- ----------------------------
-- 11. Order Comments table
-- ----------------------------
CREATE TABLE order_comments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    author_id BIGINT,
    message TEXT,
    is_internal BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    CONSTRAINT fk_order_comments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_comments_author FOREIGN KEY (author_id) REFERENCES employees (id) ON DELETE SET NULL
);

CREATE INDEX idx_order_comments_order ON order_comments(order_id);

-- ----------------------------
-- 12. Files table
-- ----------------------------
CREATE TABLE files (
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
    CONSTRAINT fk_files_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_files_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE CASCADE
);

CREATE INDEX idx_files_order ON files(order_id);
CREATE INDEX idx_files_order_item ON files(order_item_id);
