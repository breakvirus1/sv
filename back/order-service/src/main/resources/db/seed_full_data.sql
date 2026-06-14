DROP SCHEMA IF EXISTS ordschema CASCADE;
DROP SCHEMA IF EXISTS svtables CASCADE;
CREATE SCHEMA svtables;

CREATE TABLE svtables.clients (
    id BIGSERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, type VARCHAR(50),
    contact_person VARCHAR(255), phone VARCHAR(50), email VARCHAR(255),
    inn VARCHAR(100), address VARCHAR(255), priceplus DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT now(), updated_at TIMESTAMP DEFAULT now(), deleted BOOLEAN DEFAULT false
);
CREATE TABLE svtables.employees (
    id BIGSERIAL PRIMARY KEY, full_name VARCHAR(255) NOT NULL, username VARCHAR(100) NOT NULL UNIQUE,
    position VARCHAR(100), phone VARCHAR(50), email VARCHAR(255), workshop_id BIGINT,
    created_at TIMESTAMP DEFAULT now(), updated_at TIMESTAMP DEFAULT now(), deleted BOOLEAN DEFAULT false
);
CREATE TABLE svtables.workshops (
    id BIGSERIAL PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE, sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT now(), updated_at TIMESTAMP DEFAULT now(), deleted BOOLEAN DEFAULT false
);
CREATE TABLE svtables.workshop_operations (
    workshop_id BIGINT NOT NULL REFERENCES svtables.workshops(id) ON DELETE CASCADE,
    operation_id BIGINT NOT NULL, PRIMARY KEY (workshop_id, operation_id)
);
CREATE TABLE svtables.materials (
    id BIGSERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, article VARCHAR(100), type VARCHAR(50),
    unit VARCHAR(20), price NUMERIC(12,2) DEFAULT 0, waste_coefficient NUMERIC(5,3) DEFAULT 1.0,
    supplier VARCHAR(255), current_stock NUMERIC(12,2) DEFAULT 0, min_stock NUMERIC(12,2) DEFAULT 0,
    default_width_m NUMERIC(10,4), default_height_m NUMERIC(10,4),
    created_at TIMESTAMP DEFAULT now(), updated_at TIMESTAMP DEFAULT now(), deleted BOOLEAN DEFAULT false
);
CREATE TABLE svtables.material_operations (
    id BIGSERIAL PRIMARY KEY, material_id BIGINT REFERENCES svtables.materials(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL, description TEXT, operation_type VARCHAR(50),
    quantity_formula VARCHAR(255), base_price NUMERIC(12,2) DEFAULT 0, unit VARCHAR(20) DEFAULT 'шт',
    waste_coefficient NUMERIC(5,3) DEFAULT 1.0, requires_dimensions BOOLEAN DEFAULT false,
    allows_additional_materials BOOLEAN DEFAULT false, sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT now(), updated_at TIMESTAMP DEFAULT now(), deleted BOOLEAN DEFAULT false
);
CREATE TABLE svtables.roles (id BIGSERIAL PRIMARY KEY, name VARCHAR(20) NOT NULL);
CREATE TABLE svtables.orders (
    id BIGSERIAL PRIMARY KEY, order_number VARCHAR(50) NOT NULL UNIQUE,
    client_id BIGINT NOT NULL REFERENCES svtables.clients(id) ON DELETE RESTRICT,
    description TEXT, total_amount NUMERIC(12,2) DEFAULT 0, paid_amount NUMERIC(12,2) DEFAULT 0,
    debt_amount NUMERIC(12,2) DEFAULT 0, cost_price NUMERIC(15,2) DEFAULT 0, margin_percent NUMERIC(8,2),
    status VARCHAR(30), production_stage VARCHAR(50), order_date DATE, due_date DATE,
    manager_id BIGINT REFERENCES svtables.employees(id) ON DELETE SET NULL,
    priceplus DECIMAL(10,2), total_with_priceplus NUMERIC(12,2) DEFAULT 0,
    launched_at TIMESTAMP, ready_at TIMESTAMP, accepted_at TIMESTAMP, closed_at TIMESTAMP,
    has_documents BOOLEAN DEFAULT false, created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(), deleted BOOLEAN DEFAULT false
);
CREATE TABLE svtables.order_items (
    id BIGSERIAL PRIMARY KEY, order_id BIGINT NOT NULL REFERENCES svtables.orders(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL, width NUMERIC(10,3), height NUMERIC(10,3),
    price NUMERIC(12,2) DEFAULT 0, quantity INTEGER DEFAULT 1, cost NUMERIC(12,2) DEFAULT 0,
    ready_date DATE, product_id BIGINT, params JSONB, file_id BIGINT,
    created_at TIMESTAMP DEFAULT now(), updated_at TIMESTAMP DEFAULT now(), deleted BOOLEAN DEFAULT false
);
CREATE TABLE svtables.order_item_operations (
    id BIGSERIAL PRIMARY KEY, order_item_id BIGINT NOT NULL REFERENCES svtables.order_items(id) ON DELETE CASCADE,
    operation_id BIGINT NOT NULL, operation_name VARCHAR(255) NOT NULL, price_per_unit NUMERIC(12,2),
    calculated_quantity NUMERIC(12,4), subtotal NUMERIC(12,2), width_m NUMERIC(10,4), height_m NUMERIC(10,4),
    created_at TIMESTAMP DEFAULT now(), updated_at TIMESTAMP DEFAULT now(), deleted BOOLEAN DEFAULT false
);
CREATE TABLE svtables.order_stages (
    id BIGSERIAL PRIMARY KEY, order_id BIGINT NOT NULL REFERENCES svtables.orders(id) ON DELETE CASCADE,
    workshop_id BIGINT NOT NULL REFERENCES svtables.workshops(id) ON DELETE RESTRICT,
    wait_previous BOOLEAN DEFAULT true, due_date DATE, note VARCHAR(500), status VARCHAR(50) DEFAULT 'TODO',
    source_files TEXT, created_at TIMESTAMP DEFAULT now(), updated_at TIMESTAMP DEFAULT now(), deleted BOOLEAN DEFAULT false
);
CREATE TABLE svtables.order_materials (
    id BIGSERIAL PRIMARY KEY, order_id BIGINT REFERENCES svtables.orders(id) ON DELETE CASCADE,
    order_item_id BIGINT REFERENCES svtables.order_items(id) ON DELETE CASCADE,
    material_id BIGINT NOT NULL REFERENCES svtables.materials(id) ON DELETE RESTRICT,
    quantity NUMERIC(12,2) DEFAULT 0, waste_coefficient NUMERIC(5,3) DEFAULT 1.0,
    cost NUMERIC(12,2) DEFAULT 0, cost_priceplus NUMERIC(12,2) DEFAULT 0,
    width_m NUMERIC(10,3), height_m NUMERIC(10,3), eyelet_cost NUMERIC(12,2) DEFAULT 0,
    ready_date DATE, created_at TIMESTAMP DEFAULT now(), updated_at TIMESTAMP DEFAULT now(), deleted BOOLEAN DEFAULT false
);
CREATE TABLE svtables.order_material_operations (
    id BIGSERIAL PRIMARY KEY, order_id BIGINT NOT NULL REFERENCES svtables.orders(id) ON DELETE CASCADE,
    order_material_id BIGINT NOT NULL REFERENCES svtables.order_materials(id) ON DELETE CASCADE,
    material_operation_id BIGINT REFERENCES svtables.material_operations(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL, operation_type VARCHAR(30), base_price NUMERIC(12,2),
    unit VARCHAR(20) DEFAULT 'шт', waste_coefficient NUMERIC(5,3) DEFAULT 1.0,
    quantity NUMERIC(12,4) DEFAULT 1, cost NUMERIC(12,2) DEFAULT 0,
    parameters JSONB, additional_materials JSONB, active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT now(), updated_at TIMESTAMP DEFAULT now(), deleted BOOLEAN DEFAULT false
);
CREATE TABLE svtables.payments (
    id BIGSERIAL PRIMARY KEY, order_id BIGINT NOT NULL REFERENCES svtables.orders(id) ON DELETE CASCADE,
    payment_date DATE NOT NULL, amount NUMERIC(12,2) NOT NULL, payment_type VARCHAR(50),
    details VARCHAR(100), is_partial BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT now(), updated_at TIMESTAMP DEFAULT now(), deleted BOOLEAN DEFAULT false
);
CREATE TABLE svtables.order_comments (
    id BIGSERIAL PRIMARY KEY, order_id BIGINT NOT NULL REFERENCES svtables.orders(id) ON DELETE CASCADE,
    author_id BIGINT REFERENCES svtables.employees(id) ON DELETE SET NULL,
    message TEXT, is_internal BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT now(), updated_at TIMESTAMP DEFAULT now(), deleted BOOLEAN DEFAULT false
);
CREATE TABLE svtables.files (
    id BIGSERIAL PRIMARY KEY, file_name VARCHAR(500) NOT NULL, original_name VARCHAR(500),
    file_path VARCHAR(500) NOT NULL, file_url VARCHAR(500), mime_type VARCHAR(100), file_size BIGINT,
    order_id BIGINT REFERENCES svtables.orders(id) ON DELETE CASCADE,
    order_item_id BIGINT REFERENCES svtables.order_items(id) ON DELETE CASCADE,
    uploaded_by VARCHAR(100), created_at TIMESTAMP DEFAULT now(), updated_at TIMESTAMP DEFAULT now(), deleted BOOLEAN DEFAULT false
);

-- === Static data ===
INSERT INTO svtables.clients (name, type, contact_person, phone, email, inn, address, priceplus) VALUES
('ООО ПромПринт', 'Юридическое лицо', 'Иванов И.И.', '+79001234567', 'ivanov@promprint.ru', '7712345678', 'г. Москва, ул. Промышленная, 1', 10.00),
('ИП Сидоров', 'ИП', 'Сидоров А.А.', '+79002345678', 'sidorov@mail.ru', '501234567890', 'г. Москва, ул. Ленина, 25', 5.00),
('АО РекламаСервис', 'Юридическое лицо', 'Петров П.П.', '+79003456789', 'petrov@reklama.ru', '7723456789', 'г. Санкт-Петербург, пр. Невский, 100', 15.00),
('ООО БаннерХолдинг', 'Юридическое лицо', 'Козлов К.К.', '+79004567890', 'kozlov@banner.ru', '7734567890', 'г. Казань, ул. Баумана, 50', 0.00),
('ИП Кузнецов', 'ИП', 'Кузнецов Д.Д.', '+79005678901', 'kuznetsov@yandex.ru', '502345678901', 'г. Екатеринбург, ул. Ленина, 30', 20.00),
('ООО МедиаГрупп', 'Юридическое лицо', 'Смирнов С.С.', '+79006789012', 'smirnov@mediagroup.ru', '7745678901', 'г. Новосибирск, ул. Красный проспект, 75', 10.00),
('ЗАО Полиграфия Плюс', 'Юридическое лицо', 'Федоров Ф.Ф.', '+79007890123', 'fedorov@polygraph.ru', '7756789012', 'г. Нижний Новгород, ул. Советская, 40', 5.00),
('ИП Морозов', 'ИП', 'Морозов М.М.', '+79008901234', 'morozov@gmail.com', '503456789012', 'г. Самара, ул. Московская, 20', 15.00),
('ООО ВизуалПро', 'Юридическое лицо', 'Волков В.В.', '+79009012345', 'volkov@visualpro.ru', '7767890123', 'г. Ростов-на-Дону, ул. Садовая, 60', 10.00),
('ООО ДизайнСтудия', 'Юридическое лицо', 'Новиков Н.Н.', '+79000123456', 'novikov@design.ru', '7778901234', 'г. Краснодар, ул. Ленина, 80', 0.00);

INSERT INTO svtables.employees (full_name, username, position, phone, email) VALUES
('System Administrator', 'admin', 'Администратор', '+79001111111', 'admin@example.com'),
('Менеджер Тестовый', 'manager', 'Менеджер', '+79002222222', 'manager@example.com'),
('Производство Исполнитель', 'production', 'Оператор производства', '+79003333333', 'production@example.com'),
('Бухгалтер Главный', 'accountant', 'Бухгалтер', '+79004444444', 'accountant@example.com'),
('Иванов Иван Иванович', 'ivanov_i', 'Менеджер по продажам', '+79005555555', 'ivanov@company.ru'),
('Петрова Анна Сергеевна', 'petrova_a', 'Дизайнер', '+79006666666', 'petrova@company.ru'),
('Сидоров Дмитрий Александрович', 'sidorov_d', 'Оператор печати', '+79007777777', 'sidorov@company.ru'),
('Козлова Елена Викторовна', 'kozlova_e', 'Контролер качества', '+79008888888', 'kozlova@company.ru');

INSERT INTO svtables.workshops (name, sort_order) VALUES
('Цех печати', 1), ('Цех постпечатной обработки', 2), ('Цех сборки и упаковки', 3), ('Склад готовой продукции', 4);

INSERT INTO svtables.workshop_operations (workshop_id, operation_id) VALUES
(1, 1), (1, 2), (1, 3),
(2, 4), (2, 5), (2, 6), (2, 7), (2, 8), (2, 9), (2, 10), (2, 11),
(3, 5), (3, 6), (3, 7),
(4, 5);

UPDATE svtables.employees SET workshop_id = 1 WHERE username IN ('production', 'sidorov_d');
UPDATE svtables.employees SET workshop_id = 2 WHERE username IN ('petrova_a', 'kozlova_e');
UPDATE svtables.employees SET workshop_id = 3 WHERE username = 'ivanov_i';
UPDATE svtables.employees SET workshop_id = 4 WHERE username = 'accountant';

INSERT INTO svtables.materials (name, article, type, unit, price, waste_coefficient, supplier, default_width_m, default_height_m) VALUES
('Баннер Frontlit 440 г/м²', 'BF-440', 'BANNER', 'м2', 250.00, 1.100, 'Поставщик А', 3.2000, 1.0000),
('Баннер Blockout 510 г/м²', 'BB-510', 'BANNER', 'м2', 280.00, 1.100, 'Поставщик А', 3.2000, 1.0000),
('Баннер Литой 550 г/м²', 'BL-550', 'BANNER', 'м2', 320.00, 1.050, 'Поставщик Б', 3.2000, 1.0000),
('Баннер Тканевый 340 г/м²', 'BT-340', 'BANNER', 'м2', 380.00, 1.150, 'Поставщик В', 5.0000, 1.0000),
('Баннер Сетка 320 г/м²', 'BS-320', 'BANNER', 'м2', 200.00, 1.050, 'Поставщик А', 3.2000, 1.0000),
('Плёнка Monomer 80 мкм', 'PM-80', 'PLENKA', 'м2', 200.00, 1.100, 'Поставщик Г', 1.6000, 1.0000),
('Плёнка Cast 100 мкм', 'PC-100', 'PLENKA', 'м2', 600.00, 1.050, 'Поставщик Г', 1.6000, 1.0000),
('Плёнка Матовая 120 мкм', 'PM-120', 'PLENKA', 'м2', 450.00, 1.080, 'Поставщик Д', 1.6000, 1.0000),
('Плёнка Глянцевая 90 мкм', 'PG-90', 'PLENKA', 'м2', 500.00, 1.080, 'Поставщик Д', 1.6000, 1.0000),
('Баннер Двусторонний 480 г/м²', 'BD-480', 'BANNER', 'м2', 350.00, 1.100, 'Поставщик Б', 3.2000, 1.0000),
('Плёнка С клеевым слоем 70 мкм', 'PK-70', 'PLENKA', 'м2', 280.00, 1.120, 'Поставщик Е', 1.6000, 1.0000),
('Баннер Интерьерный 320 г/м²', 'BI-320', 'BANNER', 'м2', 420.00, 1.050, 'Поставщик В', 3.2000, 1.0000),
('Баннер Уличный 510 г/м²', 'BU-510', 'BANNER', 'м2', 300.00, 1.100, 'Поставщик А', 5.0000, 1.0000),
('Плёнка Транслюцентная 100 мкм', 'PT-100', 'PLENKA', 'м2', 550.00, 1.050, 'Поставщик Д', 1.6000, 1.0000),
('Баннер Перфорированный 400 г/м²', 'BP-400', 'BANNER', 'м2', 270.00, 1.080, 'Поставщик Ж', 3.2000, 1.0000),
('Плёнка Бэклит 100 мкм', 'PB-100', 'PLENKA', 'м2', 700.00, 1.030, 'Поставщик З', 1.6000, 1.0000),
('Баннер Магнитный 600 г/м²', 'BM-600', 'BANNER', 'м2', 500.00, 1.100, 'Поставщик И', 1.0000, 1.0000),
('Плёнка Антиграффити 120 мкм', 'PA-120', 'PLENKA', 'м2', 800.00, 1.050, 'Поставщик З', 1.6000, 1.0000),
('Баннер Светоотражающий 450 г/м²', 'BR-450', 'BANNER', 'м2', 450.00, 1.100, 'Поставщик И', 3.2000, 1.0000),
('Плёнка Термоперенос 80 мкм', 'PTP-80', 'PLENKA', 'м2', 350.00, 1.080, 'Поставщик Е', 1.6000, 1.0000);

INSERT INTO svtables.material_operations (material_id, name, operation_type, base_price, unit, sort_order) VALUES
(1, 'Печать 360 dpi', 'PRINTING', 80.00, 'м2', 1), (1, 'Печать 720 dpi', 'PRINTING', 120.00, 'м2', 2),
(1, 'Печать 1440 dpi', 'PRINTING', 200.00, 'м2', 3), (1, 'Порезка', 'CUTTING', 30.00, 'п.м.', 4),
(1, 'Подворот', 'HEMMING', 25.00, 'п.м.', 5), (1, 'Установка люверсов', 'EYELET', 15.00, 'шт', 6),
(1, 'Проклейка', 'WELDING', 20.00, 'п.м.', 7), (1, 'Сварка', 'WELDING', 25.00, 'п.м.', 8),
(1, 'Ламинация глянцевая', 'LAMINATION', 60.00, 'м2', 9), (1, 'Ламинация матовая', 'LAMINATION', 65.00, 'м2', 10),
(1, 'Контурная резка', 'CUTTING', 50.00, 'п.м.', 11), (1, 'Поклейка', 'APPLICATION', 40.00, 'м2', 12);

INSERT INTO svtables.material_operations (material_id, name, operation_type, base_price, unit, sort_order)
SELECT m.id, mo.name, mo.operation_type, mo.base_price, mo.unit, mo.sort_order
FROM generate_series(2, 5) AS m(id)
CROSS JOIN (SELECT * FROM svtables.material_operations WHERE material_id = 1) mo;

INSERT INTO svtables.material_operations (material_id, name, operation_type, base_price, unit, sort_order)
SELECT m.id, mo.name, mo.operation_type, mo.base_price, mo.unit, mo.sort_order
FROM unnest(ARRAY[10,12,13,15,17,19]) AS m(id)
CROSS JOIN (SELECT * FROM svtables.material_operations WHERE material_id = 1) mo;

INSERT INTO svtables.material_operations (material_id, name, operation_type, base_price, unit, sort_order)
SELECT m.id, mo.name, mo.operation_type, mo.base_price, mo.unit, mo.sort_order
FROM generate_series(6, 9) AS m(id)
CROSS JOIN (SELECT * FROM svtables.material_operations WHERE material_id = 1 AND operation_type NOT IN ('EYELET', 'WELDING')) mo;

INSERT INTO svtables.material_operations (material_id, name, operation_type, base_price, unit, sort_order)
SELECT m.id, mo.name, mo.operation_type, mo.base_price, mo.unit, mo.sort_order
FROM unnest(ARRAY[11,14,16,18,20]) AS m(id)
CROSS JOIN (SELECT * FROM svtables.material_operations WHERE material_id = 1 AND operation_type NOT IN ('EYELET', 'WELDING')) mo;

INSERT INTO svtables.roles (name) VALUES ('ADMIN'), ('MANAGER'), ('PRODUCTION'), ('ACCOUNTANT'), ('USER');

-- === 1000 orders using CTEs (no temp tables, single transaction) ===
WITH
_o AS (
    SELECT i, 'ORD-' || lpad(i::TEXT, 6, '0') AS order_number,
        (i % 10) + 1 AS client_id,
        CASE WHEN i % 5 = 0 THEN 2 WHEN i % 3 = 0 THEN 5 ELSE (i % 4) + 2 END AS manager_id,
        CURRENT_DATE - (i % 365) AS order_date,
        CURRENT_DATE - (i % 365) + 7 + (i % 21) AS due_date,
        (ARRAY['NEW','IN_PROGRESS','READY','ACCEPTED','CLOSED','CANCELLED','PAID'])[(i % 7) + 1] AS status,
        (ARRAY['Новый','В производстве','Печать','Постпечатная обработка','Сборка','Готов к выдаче','Выдан','Закрыт'])[(i % 8) + 1] AS stage,
        (i % 5) + 1 AS item_count,
        i % 3 = 0 AS has_documents
    FROM generate_series(1, 1000) i
),
_ins_orders AS (
    INSERT INTO svtables.orders (order_number, client_id, description, status, production_stage, order_date, due_date, manager_id, priceplus, launched_at, ready_at, accepted_at, closed_at, has_documents)
    SELECT o.order_number, o.client_id, 'Заказ #' || o.i || ' для клиента #' || o.client_id, o.status, o.stage,
        o.order_date, o.due_date, o.manager_id, c.priceplus,
        CASE WHEN o.status IN ('CLOSED','ACCEPTED','PAID') THEN o.order_date + (o.i % 3) * INTERVAL '1 day'
             WHEN o.status = 'READY' THEN o.order_date + (o.i % 3) * INTERVAL '1 day'
             WHEN o.status = 'IN_PROGRESS' THEN o.order_date + (o.i % 3) * INTERVAL '1 day'
             ELSE NULL END,
        CASE WHEN o.status IN ('CLOSED','ACCEPTED','PAID') THEN o.order_date + ((o.i % 3) + (o.i % 7)) * INTERVAL '1 day'
             WHEN o.status = 'READY' THEN o.order_date + ((o.i % 3) + (o.i % 7)) * INTERVAL '1 day'
             ELSE NULL END,
        CASE WHEN o.status IN ('CLOSED','ACCEPTED','PAID') THEN o.order_date + ((o.i % 3) + (o.i % 7) + 1) * INTERVAL '1 day'
             ELSE NULL END,
        CASE WHEN o.status IN ('CLOSED','PAID') THEN o.order_date + ((o.i % 3) + (o.i % 7) + 2) * INTERVAL '1 day'
             ELSE NULL END,
        o.has_documents
    FROM _o o JOIN svtables.clients c ON c.id = o.client_id
    RETURNING id, priceplus, due_date, status, has_documents
),
_oi AS (
    SELECT o.id AS order_id, o.priceplus, o.due_date, o.status, o.has_documents,
        j, ((o.id + j) % 20) + 1 AS material_id,
        ROUND((0.5 + ((o.id * j * 7) % 450) / 100.0)::NUMERIC, 3) AS width,
        ROUND((0.5 + ((o.id * j * 3) % 250) / 100.0)::NUMERIC, 3) AS height,
        m.name AS mat_name, m.price AS mat_price, m.waste_coefficient,
        (2 + ((o.id + j) % 5)) AS op_count
    FROM _ins_orders o
    CROSS JOIN generate_series(1, 1 + o.id % 5) j
    JOIN svtables.materials m ON m.id = ((o.id + j) % 20) + 1
),
_ins_items AS (
    INSERT INTO svtables.order_items (order_id, name, width, height, quantity, ready_date, product_id)
    SELECT order_id, mat_name || ' (' || width || 'x' || height || 'м)', width, height, 1, due_date - ((j % 3) || ' days')::interval, material_id
    FROM _oi
    RETURNING id, order_id, width, height, product_id
),
_ops AS (
    SELECT oi.id AS item_id, oi.order_id, oi.width, oi.height, oi.product_id,
        _oi.j,
        mo.id AS op_id, mo.name AS op_name, mo.base_price, mo.unit,
        ROW_NUMBER() OVER (PARTITION BY oi.id ORDER BY mo.sort_order) AS rn,
        _oi.op_count
    FROM _ins_items oi
    JOIN _oi ON _oi.order_id = oi.order_id AND _oi.material_id = oi.product_id AND _oi.width = oi.width AND _oi.height = oi.height
    JOIN svtables.material_operations mo ON mo.material_id = oi.product_id AND mo.deleted = false
)
INSERT INTO svtables.order_item_operations (order_item_id, operation_id, operation_name, price_per_unit, calculated_quantity, subtotal, width_m, height_m)
SELECT item_id, op_id, op_name, base_price,
    CASE WHEN unit = 'м2' THEN ROUND((width * height)::NUMERIC, 4)
         WHEN unit = 'п.м.' THEN ROUND((2 * (width + height))::NUMERIC, 4)
         ELSE 1 END,
    CASE WHEN unit = 'м2' THEN ROUND((width * height * base_price)::NUMERIC, 2)
         WHEN unit = 'п.м.' THEN ROUND((2 * (width + height) * base_price)::NUMERIC, 2)
         ELSE ROUND(base_price::NUMERIC, 2) END,
    ROUND(width::NUMERIC, 4), ROUND(height::NUMERIC, 4)
FROM _ops WHERE rn <= op_count;

WITH
_o2 AS (
    SELECT o.id AS order_id, o.priceplus, o.due_date, o.status, o.has_documents,
        j, ((o.id + j) % 20) + 1 AS material_id,
        ROUND((0.5 + ((o.id * j * 7) % 450) / 100.0)::NUMERIC, 3) AS width,
        ROUND((0.5 + ((o.id * j * 3) % 250) / 100.0)::NUMERIC, 3) AS height,
        m.price AS mat_price, m.waste_coefficient
    FROM svtables.orders o
    CROSS JOIN generate_series(1, o.id % 5 + 1) j
    JOIN svtables.materials m ON m.id = ((o.id + j) % 20) + 1
),
_ins_om AS (
    INSERT INTO svtables.order_materials (order_id, order_item_id, material_id, quantity, waste_coefficient, cost, cost_priceplus, width_m, height_m)
    SELECT _o2.order_id, oi.id, _o2.material_id,
        ROUND((_o2.width * _o2.height * _o2.waste_coefficient)::NUMERIC, 2),
        _o2.waste_coefficient,
        ROUND((_o2.width * _o2.height * _o2.mat_price * _o2.waste_coefficient)::NUMERIC, 2),
        ROUND((_o2.width * _o2.height * _o2.mat_price * _o2.waste_coefficient * (1 + _o2.priceplus / 100))::NUMERIC, 2),
        _o2.width, _o2.height
    FROM _o2
    JOIN svtables.order_items oi ON oi.order_id = _o2.order_id AND oi.product_id = _o2.material_id AND oi.width = _o2.width AND oi.height = _o2.height
    RETURNING id, order_id
)
INSERT INTO svtables.order_material_operations (order_id, order_material_id, name, operation_type, base_price, unit, waste_coefficient, quantity, cost, active)
SELECT om.order_id, om.id, 'Основная операция', 'PRINTING', m.price, 'м2', om.waste_coefficient,
    ROUND((om.width_m * om.height_m)::NUMERIC, 4), om.cost, true
FROM svtables.order_materials om
JOIN svtables.materials m ON m.id = om.material_id;

UPDATE svtables.order_items oi SET
    price = COALESCE((SELECT SUM(om.cost) FROM svtables.order_materials om WHERE om.order_item_id = oi.id), 0)
        + COALESCE((SELECT SUM(oo.subtotal) FROM svtables.order_item_operations oo WHERE oo.order_item_id = oi.id), 0),
    cost = COALESCE((SELECT SUM(om.cost) FROM svtables.order_materials om WHERE om.order_item_id = oi.id), 0);

UPDATE svtables.orders SET
    total_amount = (SELECT COALESCE(SUM(oi.price), 0) FROM svtables.order_items oi WHERE oi.order_id = orders.id AND oi.deleted = false),
    cost_price = (SELECT COALESCE(SUM(oi.cost), 0) FROM svtables.order_items oi WHERE oi.order_id = orders.id AND oi.deleted = false);

UPDATE svtables.orders SET
    total_with_priceplus = ROUND(total_amount * (1 + COALESCE(priceplus, 0) / 100), 2);

UPDATE svtables.orders SET
    paid_amount = CASE
        WHEN status IN ('PAID','CLOSED','ACCEPTED') THEN total_with_priceplus
        WHEN status = 'IN_PROGRESS' THEN ROUND((total_with_priceplus * 0.5)::NUMERIC, 2)
        ELSE 0 END;

UPDATE svtables.orders SET
    debt_amount = ROUND(total_with_priceplus - paid_amount, 2);

UPDATE svtables.orders SET
    margin_percent = CASE WHEN cost_price > 0 THEN ROUND(((total_amount - cost_price) / cost_price * 100)::NUMERIC, 2) ELSE 0 END;

INSERT INTO svtables.order_stages (order_id, workshop_id, wait_previous, due_date, note, status)
SELECT o.id, w.id, w.sort_order > 1, o.due_date - (4 - w.sort_order),
    CASE (o.id + w.sort_order) % 5 WHEN 0 THEN 'Этап начат' WHEN 1 THEN 'Ожидание материала' WHEN 2 THEN 'В работе' WHEN 3 THEN 'Проверка качества' ELSE NULL END,
    CASE
        WHEN w.sort_order = 1 AND o.status IN ('IN_PROGRESS','READY','ACCEPTED','CLOSED','PAID') THEN 'IN_PROGRESS'
        WHEN w.sort_order = 1 AND o.status = 'CANCELLED' THEN 'CANCELLED'
        WHEN w.sort_order <= 2 AND o.status IN ('READY','ACCEPTED','CLOSED','PAID') THEN 'DONE'
        WHEN w.sort_order <= 3 AND o.status IN ('ACCEPTED','CLOSED','PAID') THEN 'DONE'
        WHEN w.sort_order <= 3 AND o.status = 'READY' THEN 'IN_PROGRESS'
        WHEN w.sort_order = 4 AND o.status IN ('CLOSED','PAID') THEN 'DONE'
        ELSE 'TODO'
    END
FROM svtables.orders o CROSS JOIN svtables.workshops w;

INSERT INTO svtables.payments (order_id, payment_date, amount, payment_type, details, is_partial)
SELECT o.id, o.order_date + ((k * (1 + o.id % 10)) || ' days')::interval,
    CASE WHEN cnt = 1 THEN o.paid_amount
         WHEN k = cnt THEN o.paid_amount - ROUND((o.paid_amount / cnt)::NUMERIC, 2) * (cnt - 1)
         ELSE ROUND((o.paid_amount / cnt)::NUMERIC, 2) END,
    (ARRAY['Наличные','Безналичная оплата','Банковская карта','СБП'])[1 + (k % 4)],
    'Платёж #' || k, cnt > 1
FROM svtables.orders o
CROSS JOIN LATERAL (SELECT GREATEST(1, o.id % 4) AS cnt) c
CROSS JOIN LATERAL generate_series(1, c.cnt) k
WHERE o.paid_amount > 0;

INSERT INTO svtables.order_comments (order_id, author_id, message, is_internal)
SELECT o.id, (k % 8) + 1,
    (ARRAY['Заказ принят в работу','Уточнить детали у клиента','Материал в наличии',
        'Требуется согласование макета','Срочный заказ','Клиент просит скидку',
        'Переделка после замечаний','Отправлен макет на согласование','Ожидание оплаты',
        'Заказ готов к выдаче','Клиент забрал заказ','Частичная оплата получена',
        'Материал заказан у поставщика','Производство завершено','Требуется доработка'])[((o.id + k) % 15) + 1],
    k = 2
FROM svtables.orders o
CROSS JOIN generate_series(1, o.id % 3) k
WHERE o.id % 3 > 0;

INSERT INTO svtables.files (file_name, original_name, file_path, file_url, mime_type, file_size, order_id, uploaded_by)
SELECT 'order_' || o.id || '_file_' || k,
    (ARRAY['макет.pdf','эскиз.png','layout.ai','дизайн.psd','печать_образец.jpg'])[1 + ((o.id + k) % 5)],
    '/uploads/orders/' || o.id || '/file_' || k,
    '/api/files/orders/' || o.id || '/file_' || k,
    (ARRAY['application/pdf','image/png','application/postscript','image/vnd.adobe.photoshop','image/jpeg'])[1 + ((o.id + k) % 5)],
    1024 * (100 + ((o.id * k * 17) % 5000)),
    o.id, 'manager'
FROM svtables.orders o
CROSS JOIN generate_series(1, 1 + (o.id % 2)) k
WHERE o.has_documents;
