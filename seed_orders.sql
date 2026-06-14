-- ============================================
-- SEED DATA: Generate 1000 Orders with Items, Materials, Operations, Files
-- ============================================

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
    v_comment_author_id BIGINT;
    v_payment_amount NUMERIC(12,2);
    v_payment_date DATE;
    v_employee_id BIGINT;
    v_statuses TEXT[] := ARRAY['NEW', 'IN_PROGRESS', 'READY', 'DELIVERED', 'CANCELLED'];
    v_stages TEXT[] := ARRAY['PRINTING', 'LAMINATING', 'CUTTING', 'ASSEMBLING', 'PACKAGING', 'SHIPPED'];
    v_stage_statuses TEXT[] := ARRAY['TODO', 'IN_PROGRESS', 'DONE', 'SKIPPED'];
    v_item_names TEXT[] := ARRAY['Баннер 3x1.5', 'Баннер 2x1', 'Плакат А1', 'Плакат А2', 'Наклейка', 'Вывеска', 'Штендер', 'Табличка', 'Плотер', 'Холст', 'Фотообои', 'Roll-up', 'Пресс-волл', 'Мобильная стойка', 'Информационный стенд'];
    v_descriptions TEXT[] := ARRAY['Срочный заказ', 'Стандартный заказ', 'Для выставки', 'Для магазина', 'Для офиса', 'Для мероприятия', 'Тестовый заказ', 'Повторный заказ'];
    v_file_names TEXT[] := ARRAY['design.pdf', 'layout.ai', 'image.png', 'photo.jpg', 'logo.svg', 'scheme.dwg', 'mockup.psd', 'preview.jpg', 'tech_spec.docx', 'requirements.pdf'];
    v_mime_types TEXT[] := ARRAY['application/pdf', 'application/postscript', 'image/png', 'image/jpeg', 'image/svg+xml', 'application/acad', 'image/vnd.adobe.photoshop', 'image/jpeg', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'application/pdf'];
    v_comment_texts TEXT[] := ARRAY['Заказ принят', 'Начато производство', 'Ожидает согласования', 'Отправлено клиенту', 'Получено подтверждение', 'Производство завершено', 'Отгружено', 'Закрыто'];
    v_manager_ids BIGINT[] := ARRAY[2, 11, 12];
    v_workshop_ids BIGINT[] := ARRAY[1, 2, 3, 4];
    v_employee_ids BIGINT[] := ARRAY[1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21];
    i INTEGER;
    j INTEGER;
    k INTEGER;
    m INTEGER;
    n INTEGER;
    v_emp_idx INTEGER;
BEGIN
    FOR i IN 1..1000 LOOP
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

            v_num_materials := 1 + (j % 3);
            FOR k IN 1..v_num_materials LOOP
                v_material_id := 1 + ((i + j + k) % 100);
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

            v_num_files := (j % 3);
            FOR n IN 1..v_num_files LOOP
                INSERT INTO svtables.files (
                    file_name, original_name, file_path, file_url, mime_type,
                    file_size, order_id, order_item_id, uploaded_by
                ) VALUES (
                    'file_' || v_item_id || '_' || n || '_' || (1000000 + i)::BIGINT,
                    v_file_names[1 + ((i + j + n) % 10)],
                    '/uploads/orders/' || v_order_id || '/items/' || v_item_id || '/',
                    '/api/files/download/' || v_item_id || '/' || n,
                    v_mime_types[1 + ((i + j + n) % 10)],
                    (100000 + (random() * 5000000)::BIGINT)::BIGINT,
                    v_order_id,
                    v_item_id,
                    'manager_' || v_manager_id::TEXT
                );
            END LOOP;
        END LOOP;

        v_num_files := (i % 4);
        FOR n IN 1..v_num_files LOOP
            INSERT INTO svtables.files (
                file_name, original_name, file_path, file_url, mime_type,
                file_size, order_id, uploaded_by
            ) VALUES (
                'file_order_' || v_order_id || '_' || n || '_' || (2000000 + i)::BIGINT,
                v_file_names[1 + ((i + n) % 10)],
                '/uploads/orders/' || v_order_id || '/',
                '/api/files/download/order/' || v_order_id || '/' || n,
                v_mime_types[1 + ((i + n) % 10)],
                (100000 + (random() * 5000000)::BIGINT)::BIGINT,
                v_order_id,
                'manager_' || v_manager_id::TEXT
            );
        END LOOP;

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

        IF i % 3 = 0 THEN
            v_emp_idx := 1 + ((i - 1) % 20);
            v_employee_id := v_employee_ids[v_emp_idx];
            INSERT INTO svtables.order_comments (order_id, author_id, message, created_at)
            VALUES (v_order_id, v_employee_id, v_comment_texts[1 + (i % 8)], v_order_date + INTERVAL '1 day');
        END IF;
        IF i % 5 = 0 THEN
            v_emp_idx := 1 + (i % 20);
            v_employee_id := v_employee_ids[v_emp_idx];
            INSERT INTO svtables.order_comments (order_id, author_id, message, created_at)
            VALUES (v_order_id, v_employee_id, v_comment_texts[1 + ((i + 3) % 8)], v_order_date + INTERVAL '2 days');
        END IF;

        IF v_paid_amount > 0 THEN
            v_payment_date := v_order_date + 1;
            INSERT INTO svtables.payments (order_id, amount, payment_date, payment_type, details, is_partial)
            VALUES (v_order_id, 
                CASE WHEN v_paid_amount > 10000 THEN (v_paid_amount * 0.5)::NUMERIC(12,2) ELSE v_paid_amount END,
                v_payment_date,
                CASE i % 3 WHEN 0 THEN 'CASH' WHEN 1 THEN 'CARD' ELSE 'TRANSFER' END,
                'Оплата заказа ' || v_order_number,
                v_paid_amount > 10000
            );
            
            IF v_paid_amount > 10000 THEN
                INSERT INTO svtables.payments (order_id, amount, payment_date, payment_type, details, is_partial)
                VALUES (v_order_id, 
                    (v_paid_amount * 0.5)::NUMERIC(12,2),
                    v_payment_date + 7,
                    CASE (i + 1) % 3 WHEN 0 THEN 'CASH' WHEN 1 THEN 'CARD' ELSE 'TRANSFER' END,
                    'Доплата заказа ' || v_order_number,
                    true
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
