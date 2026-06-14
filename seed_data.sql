-- ============================================
-- SEED DATA: Additional Employees, Clients, Materials, Orders
-- Uses existing employees (IDs 1-8) and workshops (IDs 1-4 for production)
-- ============================================

-- 1. INSERT ADDITIONAL EMPLOYEES (12 more, total 20)
INSERT INTO svtables.employees (full_name, username, position, phone, email, workshop_id) VALUES
('Морозов Мороз Морозович', 'morozov_m', 'Рабочий', '+79001234566', 'morozov@sv.ru', 1),
('Волков Волк Волкович', 'volkov_v', 'Рабочий', '+79001234567', 'volkov@sv.ru', 1),
('Соколов Сокол Соколович', 'sokolov_s', 'Рабочий', '+79001234568', 'sokolov@sv.ru', 2),
('Лебедев Лебедь Лебедевич', 'lebedev_l', 'Рабочий', '+79001234569', 'lebedev@sv.ru', 2),
('Кузнецов Кузнец Кузнецович', 'kuznetsov_k', 'Рабочий', '+79001234570', 'kuznetsov@sv.ru', 3),
('Попов Поп Попович', 'popov_p', 'Рабочий', '+79001234571', 'popov@sv.ru', 3),
('Васильев Василий Васильевич', 'vasiliev_v', 'Рабочий', '+79001234572', 'vasiliev@sv.ru', 4),
('Смирнов Смирнова Смирновна', 'smirnov_s', 'Рабочий', '+79001234573', 'smirnov@sv.ru', 4),
('Орлов Орел Орлович', 'orlov_o', 'Мастер цеха', '+79001234575', 'orlov@sv.ru', 1),
('Макаров Макар Макарович', 'makarov_m', 'Мастер цеха', '+79001234576', 'makarov@sv.ru', 2),
('Зайцев Заяц Зайцевич', 'zaitsev_z', 'Менеджер', '+79001234577', 'zaitsev@sv.ru', NULL),
('Павлов Павел Павлович', 'pavlov_p', 'Бухгалтер', '+79001234578', 'pavlov@sv.ru', NULL)
ON CONFLICT (username) DO NOTHING;

-- 2. INSERT ADDITIONAL CLIENTS (20 more, total 50)
INSERT INTO svtables.clients (name, type, contact_person, phone, email, inn, address, priceplus) VALUES
('ООО Рога и Копыта', 'ООО', 'Директор Рог', '+79101111111', 'roga@mail.ru', '7701234567', 'Москва, ул. Ленина 1', 10.00),
('ИП Копытов', 'ИП', 'Копытов И.И.', '+79102222222', 'kop@mail.ru', '7701234568', 'Москва, ул. Пушкина 2', 5.00),
('ООО Вектор', 'ООО', 'Менеджер Вектор', '+79103333333', 'vektor@mail.ru', '7701234569', 'СПб, Невский 3', 15.00),
('АО Прогресс', 'АО', 'Директор Прогресс', '+79104444444', 'progress@mail.ru', '7701234570', 'Екатеринбург, Мира 4', 12.00),
('ООО Спектр', 'ООО', 'Бухгалтер Спектр', '+79105555555', 'spektr@mail.ru', '7701234571', 'Новосибирск, Ленина 5', 8.00),
('ИП Сидоров', 'ИП', 'Сидоров С.С.', '+79106666666', 'sid@mail.ru', '7701234572', 'Казань, Баумана 6', 7.00),
('ООО Альфа', 'ООО', 'Менеджер Альфа', '+79107777777', 'alfa@mail.ru', '7701234573', 'Нижний Новгород, Горького 7', 11.00),
('ООО Бета', 'ООО', 'Директор Бета', '+79108888888', 'beta@mail.ru', '7701234574', 'Самара, Советская 8', 9.00),
('АО Гамма', 'АО', 'Менеджер Гамма', '+79109999999', 'gamma@mail.ru', '7701234575', 'Ростов, Пушкинская 9', 13.00),
('ООО Дельта', 'ООО', 'Бухгалтер Дельта', '+79101010101', 'delta@mail.ru', '7701234576', 'Воронеж, Кирова 10', 6.00),
('ИП Ершов', 'ИП', 'Ершов Е.Е.', '+79101112131', 'ershov@mail.ru', '7701234577', 'Краснодар, Красная 11', 10.00),
('ООО Жуков', 'ООО', 'Директор Жуков', '+79101415161', 'zhukov@mail.ru', '7701234578', 'Уфа, Ленина 12', 14.00),
('ООО Звезда', 'ООО', 'Менеджер Звезда', '+79101718191', 'zvezda@mail.ru', '7701234579', 'Пермь, Комсомольский 13', 8.00),
('АО Империал', 'АО', 'Директор Империал', '+79102021222', 'imperial@mail.ru', '7701234580', 'Тюмень, Республики 14', 16.00),
('ООО Корона', 'ООО', 'Бухгалтер Корона', '+79102324252', 'korona@mail.ru', '7701234581', 'Ижевск, Пушкина 15', 7.00),
('ИП Лисицын', 'ИП', 'Лисицын Л.Л.', '+79102627282', 'lis@mail.ru', '7701234582', 'Тула, Ленина 16', 5.00),
('ООО Метеор', 'ООО', 'Менеджер Метеор', '+79102930313', 'meteor@mail.ru', '7701234583', 'Ярославль, Свободы 17', 12.00),
('ООО Нептун', 'ООО', 'Директор Нептун', '+79103233343', 'neptun@mail.ru', '7701234584', 'Владимир, Горького 18', 9.00),
('АО Олимп', 'АО', 'Менеджер Олимп', '+79103536373', 'olimp@mail.ru', '7701234585', 'Калуга, Кирова 19', 11.00),
('ООО Планета', 'ООО', 'Бухгалтер Планета', '+79103839404', 'planeta@mail.ru', '7701234586', 'Тверь, Мира 20', 10.00)
ON CONFLICT DO NOTHING;

-- 3. INSERT ADDITIONAL MATERIALS (29 more, total 100)
INSERT INTO svtables.materials (name, article, type, unit, price, waste_coefficient, supplier, current_stock, min_stock, default_width_m, default_height_m) VALUES
('Баннерная ткань 440 г/м²', 'BNR-440', 'Баннер', 'м²', 450.00, 1.05, 'Поставщик А', 500.00, 50.00, 3.20, 1.00),
('Баннерная ткань 510 г/м²', 'BNR-510', 'Баннер', 'м²', 520.00, 1.05, 'Поставщик А', 300.00, 30.00, 3.20, 1.00),
('Виниловая плёнка белая', 'VNL-WHT', 'Плёнка', 'м²', 380.00, 1.10, 'Поставщик Б', 800.00, 80.00, 1.52, 1.00),
('Виниловая плёнка прозрачная', 'VNL-TRN', 'Плёнка', 'м²', 420.00, 1.10, 'Поставщик Б', 400.00, 40.00, 1.52, 1.00),
('Самоклеящаяся плёнка', 'SML-001', 'Плёнка', 'м²', 350.00, 1.15, 'Поставщик В', 600.00, 60.00, 1.26, 1.00),
('Бумага для печати 170 г/м²', 'PPR-170', 'Бумага', 'лист', 5.50, 1.02, 'Поставщик Г', 10000.00, 1000.00, 0.42, 0.297),
('Бумага для печати 250 г/м²', 'PPR-250', 'Бумага', 'лист', 8.00, 1.02, 'Поставщик Г', 8000.00, 800.00, 0.42, 0.297),
('Бумага для печати 300 г/м²', 'PPR-300', 'Бумага', 'лист', 10.00, 1.02, 'Поставщик Г', 5000.00, 500.00, 0.42, 0.297),
('Картон 300 г/м²', 'CRT-300', 'Картон', 'лист', 15.00, 1.03, 'Поставщик Д', 3000.00, 300.00, 0.70, 1.00),
('Картон 400 г/м²', 'CRT-400', 'Картон', 'лист', 20.00, 1.03, 'Поставщик Д', 2000.00, 200.00, 0.70, 1.00),
('ПВХ 3 мм', 'PVC-03', 'Пластик', 'лист', 800.00, 1.05, 'Поставщик Е', 100.00, 10.00, 1.22, 2.44),
('ПВХ 5 мм', 'PVC-05', 'Пластик', 'лист', 1200.00, 1.05, 'Поставщик Е', 80.00, 8.00, 1.22, 2.44),
('ПВХ 10 мм', 'PVC-10', 'Пластик', 'лист', 2000.00, 1.05, 'Поставщик Е', 50.00, 5.00, 1.22, 2.44),
('Оргстекло 3 мм', 'ORG-03', 'Стекло', 'лист', 1500.00, 1.10, 'Поставщик Ж', 60.00, 6.00, 1.00, 1.50),
('Оргстекло 5 мм', 'ORG-05', 'Стекло', 'лист', 2500.00, 1.10, 'Поставщик Ж', 40.00, 4.00, 1.00, 1.50),
('Алюминиевый композит 4 мм', 'ALC-04', 'Композит', 'лист', 3500.00, 1.05, 'Поставщик З', 30.00, 3.00, 1.22, 2.44),
('Поликарбонат 6 мм', 'PLC-06', 'Пластик', 'лист', 1800.00, 1.08, 'Поставщик И', 45.00, 5.00, 1.22, 2.44),
('Поликарбонат 8 мм', 'PLC-08', 'Пластик', 'лист', 2400.00, 1.08, 'Поставщик И', 35.00, 4.00, 1.22, 2.44),
('Полистирол 2 мм', 'PLS-02', 'Пластик', 'лист', 400.00, 1.10, 'Поставщик К', 200.00, 20.00, 1.00, 2.00),
('Полистирол 3 мм', 'PLS-03', 'Пластик', 'лист', 550.00, 1.10, 'Поставщик К', 150.00, 15.00, 1.00, 2.00),
('ПЭТ 0.5 мм', 'PET-05', 'Пластик', 'лист', 250.00, 1.12, 'Поставщик Л', 300.00, 30.00, 1.00, 1.50),
('ПЭТ 1 мм', 'PET-10', 'Пластик', 'лист', 400.00, 1.12, 'Поставщик Л', 250.00, 25.00, 1.00, 1.50),
('Фотобумага 240 г/м²', 'FPR-240', 'Бумага', 'лист', 12.00, 1.03, 'Поставщик М', 4000.00, 400.00, 0.61, 0.914),
('Фотобумага 260 г/м²', 'FPR-260', 'Бумага', 'лист', 15.00, 1.03, 'Поставщик М', 3000.00, 300.00, 0.61, 0.914),
('Холст для печати', 'HLT-001', 'Ткань', 'м²', 800.00, 1.08, 'Поставщик Н', 100.00, 10.00, 1.60, 1.00),
('Блюбэк', 'BLB-001', 'Подложка', 'м²', 280.00, 1.05, 'Поставщик О', 400.00, 40.00, 1.52, 1.00),
('Литой винил', 'VNL-CST', 'Плёнка', 'м²', 550.00, 1.10, 'Поставщик П', 350.00, 35.00, 1.52, 1.00),
('Перфорированная плёнка', 'VNL-PRF', 'Плёнка', 'м²', 480.00, 1.12, 'Поставщик Р', 200.00, 20.00, 1.26, 1.00),
('Магнитный винил', 'VNL-MGN', 'Плёнка', 'м²', 650.00, 1.08, 'Поставщик С', 150.00, 15.00, 0.61, 1.00)
ON CONFLICT DO NOTHING;

-- 4. INSERT ADDITIONAL MATERIAL OPERATIONS
INSERT INTO svtables.material_operations (material_id, name, description, operation_type, quantity_formula, base_price, unit, waste_coefficient, requires_dimensions, allows_additional_materials, sort_order) VALUES
(1, 'Широкоформатная печать', 'Печать на баннерной ткани', 'PRINT', 'width * height', 450.00, 'м²', 1.05, true, false, 1),
(2, 'Широкоформатная печать 510', 'Печать на баннерной ткани 510', 'PRINT', 'width * height', 520.00, 'м²', 1.05, true, false, 1),
(3, 'Печать на виниле', 'Печать на белом виниле', 'PRINT', 'width * height', 380.00, 'м²', 1.10, true, false, 1),
(31, 'Ламинация глянцевая', 'Нанесение глянцевой ламинации', 'LAMINATE', 'width * height', 120.00, 'м²', 1.05, true, false, 1),
(32, 'Ламинация матовая', 'Нанесение матовой ламинации', 'LAMINATE', 'width * height', 130.00, 'м²', 1.05, true, false, 1),
(NULL, 'Резка по контуру', 'Порезка по заданному контуру', 'CUT', 'quantity', 50.00, 'шт', 1.00, false, false, 1),
(NULL, 'Фигурная резка', 'Сложная фигурная резка', 'CUT', 'quantity', 80.00, 'шт', 1.00, false, false, 2),
(NULL, 'Перфорация', 'Пробивка отверстий', 'PUNCH', 'quantity', 15.00, 'шт', 1.00, false, false, 1),
(NULL, 'Установка люверсов', 'Монтаж металлических колец', 'EYELET', 'quantity', 25.00, 'шт', 1.00, false, false, 1),
(NULL, 'Склейка', 'Склейка элементов', 'GLUE', 'quantity', 30.00, 'шт', 1.00, false, false, 1),
(NULL, 'Сборка конструкции', 'Финальная сборка', 'ASSEMBLE', 'quantity', 150.00, 'шт', 1.00, false, false, 1),
(NULL, 'Упаковка', 'Финальная упаковка заказа', 'PACKAGE', 'quantity', 50.00, 'шт', 1.00, false, false, 1)
ON CONFLICT DO NOTHING;

-- 5. ASSIGN WORKSHOP OPERATIONS (using existing workshops 1-4)
INSERT INTO svtables.workshop_operations (workshop_id, operation_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5),  -- Цех печати
(2, 6), (2, 7),  -- Цех постпечатной обработки
(3, 8), (3, 9), (3, 10),  -- Цех сборки и упаковки
(4, 11), (4, 12)  -- Склад готовой продукции
ON CONFLICT DO NOTHING;

-- 6. GENERATE 1000 ORDERS WITH ITEMS, MATERIALS, OPERATIONS, FILES
DO $$
DECLARE
    v_order_id BIGINT;
    v_order_number VARCHAR(50);
    v_client_id BIGINT;
    v_manager_id BIGINT;
    v_status VARCHAR(30);
    v_production_stage VARCHAR(50);
    v_order_date DATE;
    v_due_date DATE;
    v_total_amount NUMERIC(12,2);
    v_paid_amount NUMERIC(12,2);
    v_cost_price NUMERIC(15,2);
    v_margin_percent NUMERIC(8,2);
    v_priceplus NUMERIC(10,2);
    v_item_id BIGINT;
    v_material_id BIGINT;
    v_num_items INTEGER;
    v_num_materials INTEGER;
    v_num_operations INTEGER;
    v_num_files INTEGER;
    v_num_stages INTEGER;
    v_workshop_id BIGINT;
    v_stage_status VARCHAR(50);
    v_statuses TEXT[] := ARRAY['NEW', 'IN_PROGRESS', 'READY', 'DELIVERED', 'CANCELLED'];
    v_stages TEXT[] := ARRAY['PRINTING', 'LAMINATING', 'CUTTING', 'ASSEMBLING', 'PACKAGING', 'SHIPPED'];
    v_stage_statuses TEXT[] := ARRAY['TODO', 'IN_PROGRESS', 'DONE', 'SKIPPED'];
    v_item_names TEXT[] := ARRAY['Баннер 3x1.5', 'Баннер 2x1', 'Плакат А1', 'Плакат А2', 'Наклейка', 'Вывеска', 'Штендер', 'Табличка', 'Плотер', 'Холст', 'Фотообои', 'Roll-up', 'Пресс-волл', 'Мобильная стойка', 'Информационный стенд'];
    v_descriptions TEXT[] := ARRAY['Срочный заказ', 'Стандартный заказ', 'Для выставки', 'Для магазина', 'Для офиса', 'Для мероприятия', 'Тестовый заказ', 'Повторный заказ'];
    v_file_names TEXT[] := ARRAY['design.pdf', 'layout.ai', 'image.png', 'photo.jpg', 'logo.svg', 'scheme.dwg', 'mockup.psd', 'preview.jpg', 'tech_spec.docx', 'requirements.pdf'];
    v_mime_types TEXT[] := ARRAY['application/pdf', 'application/postscript', 'image/png', 'image/jpeg', 'image/svg+xml', 'application/acad', 'image/vnd.adobe.photoshop', 'image/jpeg', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'application/pdf'];
    v_comment_texts TEXT[] := ARRAY['Заказ принят', 'Начато производство', 'Ожидает согласования', 'Отправлено клиенту', 'Получено подтверждение', 'Производство завершено', 'Отгружено', 'Закрыто'];
    v_comment_author_id BIGINT;
    v_payment_amount NUMERIC(12,2);
    v_payment_date TIMESTAMP;
    v_manager_ids BIGINT[] := ARRAY[2, 11, 12];  -- Existing manager IDs
    v_workshop_ids BIGINT[] := ARRAY[1, 2, 3, 4];  -- Existing workshop IDs
    i INTEGER;
    j INTEGER;
    k INTEGER;
    m INTEGER;
    n INTEGER;
BEGIN
    FOR i IN 1..1000 LOOP
        -- Generate order data
        v_order_number := 'ORD-' || LPAD(i::TEXT, 6, '0');
        v_client_id := 1 + (i % 50);
        v_manager_id := v_manager_ids[1 + (i % 3)];
        v_status := v_statuses[1 + (i % 5)];
        v_production_stage := v_stages[1 + (i % 6)];
        v_order_date := CURRENT_DATE - (30 + (i % 335));
        v_due_date := v_order_date + (3 + (i % 14));
        v_priceplus := 5 + (i % 15);
        v_total_amount := 5000 + (i * 100) + (random() * 5000)::NUMERIC(12,2);
        v_paid_amount := CASE 
            WHEN v_status IN ('DELIVERED', 'READY') THEN v_total_amount
            WHEN v_status = 'IN_PROGRESS' THEN (v_total_amount * (0.3 + random() * 0.5))::NUMERIC(12,2)
            ELSE (v_total_amount * random() * 0.3)::NUMERIC(12,2)
        END;
        v_cost_price := (v_total_amount * (0.4 + random() * 0.3))::NUMERIC(15,2);
        v_margin_percent := CASE WHEN v_cost_price > 0 THEN ((v_total_amount - v_cost_price) / v_cost_price * 100)::NUMERIC(8,2) ELSE 0 END;

        -- Insert order
        INSERT INTO svtables.orders (
            order_number, client_id, description, total_amount, paid_amount, debt_amount,
            cost_price, margin_percent, status, production_stage, order_date, due_date,
            manager_id, priceplus, total_with_priceplus, launched_at, ready_at, accepted_at, closed_at
        ) VALUES (
            v_order_number, v_client_id, v_descriptions[1 + (i % 8)], 
            v_total_amount, v_paid_amount, v_total_amount - v_paid_amount,
            v_cost_price, v_margin_percent, v_status, v_production_stage, v_order_date, v_due_date,
            v_manager_id, v_priceplus, v_total_amount + v_priceplus,
            CASE WHEN v_status != 'NEW' THEN v_order_date + INTERVAL '1 day' ELSE NULL END,
            CASE WHEN v_status IN ('READY', 'DELIVERED') THEN v_due_date - INTERVAL '1 day' ELSE NULL END,
            CASE WHEN v_status IN ('IN_PROGRESS', 'READY', 'DELIVERED') THEN v_order_date + INTERVAL '2 days' ELSE NULL END,
            CASE WHEN v_status = 'DELIVERED' THEN v_due_date + INTERVAL '1 day' ELSE NULL END
        ) RETURNING id INTO v_order_id;

        -- Generate 1-5 items per order
        v_num_items := 1 + (i % 5);
        FOR j IN 1..v_num_items LOOP
            INSERT INTO svtables.order_items (
                order_id, name, width, height, price, quantity, cost, ready_date, params
            ) VALUES (
                v_order_id, 
                v_item_names[1 + ((i + j) % 15)],
                (0.5 + random() * 3)::NUMERIC(10,3),
                (0.5 + random() * 2)::NUMERIC(10,3),
                (500 + random() * 5000)::NUMERIC(12,2),
                1 + (j % 10),
                (200 + random() * 2000)::NUMERIC(12,2),
                v_due_date - (j % 3),
                jsonb_build_object(
                    'color', CASE (i + j) % 4 WHEN 0 THEN 'CMYK' WHEN 1 THEN 'RGB' WHEN 2 THEN 'Pantone' ELSE 'Black' END,
                    'resolution', CASE (i + j) % 3 WHEN 0 THEN '720dpi' WHEN 1 THEN '1440dpi' ELSE '360dpi' END,
                    'finish', CASE (i + j) % 3 WHEN 0 THEN 'glossy' WHEN 1 THEN 'matte' ELSE 'satin' END
                )
            ) RETURNING id INTO v_item_id;

            -- Generate 1-3 materials per item
            v_num_materials := 1 + (j % 3);
            FOR k IN 1..v_num_materials LOOP
                v_material_id := 1 + ((i + j + k) % 71);
                INSERT INTO svtables.order_materials (
                    order_id, order_item_id, material_id, quantity, waste_coefficient,
                    cost, cost_priceplus, width_m, height_m, eyelet_cost, ready_date
                ) VALUES (
                    v_order_id,
                    v_item_id,
                    v_material_id,
                    (1 + random() * 20)::NUMERIC(12,2),
                    1 + (random() * 0.15)::NUMERIC(5,3),
                    (100 + random() * 2000)::NUMERIC(12,2),
                    (100 + random() * 2200)::NUMERIC(12,2),
                    (0.5 + random() * 3)::NUMERIC(10,3),
                    (0.5 + random() * 2)::NUMERIC(10,3),
                    CASE WHEN k = 1 THEN (10 + random() * 50)::NUMERIC(12,2) ELSE 0 END,
                    v_due_date - (k % 2)
                );
            END LOOP;

            -- Generate 1-4 operations per item
            v_num_operations := 1 + (j % 4);
            FOR m IN 1..v_num_operations LOOP
                INSERT INTO svtables.order_item_operations (
                    order_item_id, operation_id, operation_name, price_per_unit,
                    calculated_quantity, subtotal, width_m, height_m
                ) VALUES (
                    v_item_id,
                    1 + ((i + j + m) % 20),
                    (SELECT name FROM svtables.material_operations WHERE id = 1 + ((i + j + m) % 20)),
                    (50 + random() * 500)::NUMERIC(12,2),
                    (1 + random() * 10)::NUMERIC(12,4),
                    (100 + random() * 3000)::NUMERIC(12,2),
                    (0.5 + random() * 3)::NUMERIC(10,4),
                    (0.5 + random() * 2)::NUMERIC(10,4)
                );
            END LOOP;

            -- Generate 0-2 files per item
            v_num_files := (j % 3);
            FOR n IN 1..v_num_files LOOP
                INSERT INTO svtables.files (
                    file_name, original_name, file_path, file_url, mime_type,
                    file_size, order_id, order_item_id, uploaded_by
                ) VALUES (
                    'file_' || v_item_id || '_' || n || '_' || extract(epoch from now())::BIGINT,
                    v_file_names[1 + ((i + j + n) % 10)],
                    '/uploads/orders/' || v_order_id || '/items/' || v_item_id || '/',
                    '/api/files/download/' || v_item_id || '/' || n,
                    v_mime_types[1 + ((i + j + n) % 10)],
                    (100000 + random() * 5000000)::BIGINT,
                    v_order_id,
                    v_item_id,
                    'manager_' || v_manager_id::TEXT
                );
            END LOOP;
        END LOOP;

        -- Generate 0-3 files per order (without item)
        v_num_files := (i % 4);
        FOR n IN 1..v_num_files LOOP
            INSERT INTO svtables.files (
                file_name, original_name, file_path, file_url, mime_type,
                file_size, order_id, uploaded_by
            ) VALUES (
                'file_order_' || v_order_id || '_' || n || '_' || extract(epoch from now())::BIGINT,
                v_file_names[1 + ((i + n) % 10)],
                '/uploads/orders/' || v_order_id || '/',
                '/api/files/download/order/' || v_order_id || '/' || n,
                v_mime_types[1 + ((i + n) % 10)],
                (100000 + random() * 5000000)::BIGINT,
                v_order_id,
                'manager_' || v_manager_id::TEXT
            );
        END LOOP;

        -- Generate 1-4 stages per order
        v_num_stages := 1 + (i % 4);
        FOR k IN 1..v_num_stages LOOP
            v_workshop_id := v_workshop_ids[1 + ((k - 1) % 4)];
            v_stage_status := CASE 
                WHEN v_status = 'DELIVERED' THEN 'DONE'
                WHEN v_status = 'READY' AND k < v_num_stages THEN 'DONE'
                WHEN v_status = 'IN_PROGRESS' AND k < 3 THEN 'DONE'
                WHEN v_status = 'IN_PROGRESS' AND k = 3 THEN 'IN_PROGRESS'
                ELSE v_stage_statuses[1 + (k % 4)]
            END;
            
            INSERT INTO svtables.order_stages (
                order_id, workshop_id, wait_previous, due_date, note, status, source_files
            ) VALUES (
                v_order_id,
                v_workshop_id,
                k > 1,
                v_order_date + (k * 2),
                'Этап ' || k || ': ' || (SELECT name FROM svtables.workshops WHERE id = v_workshop_id),
                v_stage_status,
                CASE WHEN k = 1 THEN '/uploads/orders/' || v_order_id || '/source/' ELSE NULL END
            );
        END LOOP;

        -- Generate 0-2 comments per order
        IF i % 3 = 0 THEN
            v_comment_author_id := 1 + ((i - 1) % 8);
            INSERT INTO svtables.order_comments (order_id, author_id, comment, created_at)
            VALUES (v_order_id, v_comment_author_id, v_comment_texts[1 + (i % 8)], v_order_date + INTERVAL '1 day');
        END IF;
        IF i % 5 = 0 THEN
            v_comment_author_id := 1 + (i % 8);
            INSERT INTO svtables.order_comments (order_id, author_id, comment, created_at)
            VALUES (v_order_id, v_comment_author_id, v_comment_texts[1 + ((i + 3) % 8)], v_order_date + INTERVAL '2 days');
        END IF;

        -- Generate 1-3 payments per order
        IF v_paid_amount > 0 THEN
            v_payment_date := v_order_date + INTERVAL '1 day';
            INSERT INTO svtables.payments (order_id, amount, payment_date, payment_method, note)
            VALUES (v_order_id, 
                CASE WHEN v_paid_amount > 10000 THEN (v_paid_amount * 0.5)::NUMERIC(12,2) ELSE v_paid_amount END,
                v_payment_date,
                CASE i % 3 WHEN 0 THEN 'CASH' WHEN 1 THEN 'CARD' ELSE 'TRANSFER' END,
                'Оплата заказа ' || v_order_number
            );
            
            IF v_paid_amount > 10000 THEN
                INSERT INTO svtables.payments (order_id, amount, payment_date, payment_method, note)
                VALUES (v_order_id, 
                    (v_paid_amount * 0.5)::NUMERIC(12,2),
                    v_payment_date + INTERVAL '7 days',
                    CASE (i + 1) % 3 WHEN 0 THEN 'CASH' WHEN 1 THEN 'CARD' ELSE 'TRANSFER' END,
                    'Доплата заказа ' || v_order_number
                );
            END IF;
        END IF;
    END LOOP;
END $$;

-- Show statistics
SELECT 'Orders' as entity, COUNT(*)::TEXT as count FROM svtables.orders
UNION ALL
SELECT 'Order Items', COUNT(*)::TEXT FROM svtables.order_items
UNION ALL
SELECT 'Order Materials', COUNT(*)::TEXT FROM svtables.order_materials
UNION ALL
SELECT 'Order Item Operations', COUNT(*)::TEXT FROM svtables.order_item_operations
UNION ALL
SELECT 'Files', COUNT(*)::TEXT FROM svtables.files
UNION ALL
SELECT 'Order Stages', COUNT(*)::TEXT FROM svtables.order_stages
UNION ALL
SELECT 'Payments', COUNT(*)::TEXT FROM svtables.payments
UNION ALL
SELECT 'Comments', COUNT(*)::TEXT FROM svtables.order_comments
UNION ALL
SELECT 'Clients', COUNT(*)::TEXT FROM svtables.clients
UNION ALL
SELECT 'Employees', COUNT(*)::TEXT FROM svtables.employees
UNION ALL
SELECT 'Materials', COUNT(*)::TEXT FROM svtables.materials
UNION ALL
SELECT 'Workshops', COUNT(*)::TEXT FROM svtables.workshops
UNION ALL
SELECT 'Material Operations', COUNT(*)::TEXT FROM svtables.material_operations;
