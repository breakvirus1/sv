-- ============================================
-- UPDATE ORDER STAGES with ProductionStage enum values
-- ProductionStage: NOT_STARTED, DESIGN, PRINTING, FINISHING, QUALITY_CONTROL, PACKAGING, SHIPPING
-- Workshops: 1=Цех печати, 2=Цех постпечатной обработки, 3=Цех сборки и упаковки, 4=Склад готовой продукции
-- ============================================

-- First, let's see current state
SELECT 'Before update' as info, status, COUNT(*) as count 
FROM svtables.order_stages 
GROUP BY status 
ORDER BY status;

-- Update stages to use proper ProductionStage enum values
-- Map stages to workshops:
-- DESIGN -> Цех печати (1) - design phase before printing
-- PRINTING -> Цех печати (1)
-- FINISHING -> Цех постпечатной обработки (2)
-- QUALITY_CONTROL -> Склад готовой продукции (4)
-- PACKAGING -> Цех сборки и упаковки (3)
-- SHIPPING -> Склад готовой продукции (4)

DO $$
DECLARE
    v_order_id BIGINT;
    v_order_status VARCHAR(30);
    v_num_stages INTEGER;
    v_current_stage INTEGER;
    v_stage_status VARCHAR(50);
    v_workshop_id BIGINT;
    v_stage_name VARCHAR(50);
    v_stages TEXT[] := ARRAY['DESIGN', 'PRINTING', 'FINISHING', 'QUALITY_CONTROL', 'PACKAGING', 'SHIPPING'];
    v_stage_statuses TEXT[] := ARRAY['NOT_STARTED', 'IN_PROGRESS', 'DONE'];
    v_order_cursor CURSOR FOR 
        SELECT o.id, o.status 
        FROM svtables.orders o
        ORDER BY o.id;
BEGIN
    -- Delete existing stages
    DELETE FROM svtables.order_stages;
    
    -- Recreate stages for each order
    OPEN v_order_cursor;
    LOOP
        FETCH v_order_cursor INTO v_order_id, v_order_status;
        EXIT WHEN NOT FOUND;
        
        -- Determine number of stages (2-6)
        v_num_stages := 2 + (v_order_id % 5);
        v_current_stage := 1;
        
        FOR i IN 1..v_num_stages LOOP
            v_stage_name := v_stages[i];
            
            -- Map stage to workshop
            v_workshop_id := CASE v_stage_name
                WHEN 'DESIGN' THEN 1  -- Цех печати
                WHEN 'PRINTING' THEN 1  -- Цех печати
                WHEN 'FINISHING' THEN 2  -- Цех постпечатной обработки
                WHEN 'QUALITY_CONTROL' THEN 4  -- Склад готовой продукции
                WHEN 'PACKAGING' THEN 3  -- Цех сборки и упаковки
                WHEN 'SHIPPING' THEN 4  -- Склад готовой продукции
                ELSE 1
            END;
            
            -- Determine stage status based on order status and position
            v_stage_status := CASE v_order_status
                WHEN 'DELIVERED' THEN 'DONE'
                WHEN 'READY' THEN 
                    CASE WHEN i < v_num_stages THEN 'DONE' ELSE 'IN_PROGRESS' END
                WHEN 'IN_PROGRESS' THEN
                    CASE 
                        WHEN i <= 2 THEN 'DONE'
                        WHEN i = 3 THEN 'IN_PROGRESS'
                        ELSE 'NOT_STARTED'
                    END
                WHEN 'NEW' THEN
                    CASE 
                        WHEN i = 1 THEN 'IN_PROGRESS'
                        ELSE 'NOT_STARTED'
                    END
                WHEN 'CANCELLED' THEN
                    CASE 
                        WHEN i <= 2 THEN 'DONE'
                        ELSE 'NOT_STARTED'
                    END
                ELSE 'NOT_STARTED'
            END;
            
            INSERT INTO svtables.order_stages (
                order_id, 
                workshop_id, 
                wait_previous, 
                due_date, 
                note, 
                status, 
                source_files
            ) VALUES (
                v_order_id,
                v_workshop_id,
                i > 1,
                CURRENT_DATE + (i * 2),
                v_stage_name || ': ' || (SELECT name FROM svtables.workshops WHERE id = v_workshop_id),
                v_stage_status,
                CASE WHEN i = 1 THEN '/uploads/orders/' || v_order_id || '/source/' ELSE NULL END
            );
            
            v_current_stage := v_current_stage + 1;
        END LOOP;
    END LOOP;
    CLOSE v_order_cursor;
END $$;

-- Show results
SELECT 'After update' as info, status, COUNT(*) as count 
FROM svtables.order_stages 
GROUP BY status 
ORDER BY status;

-- Show stages distribution by workshop
SELECT w.name as workshop, os.status, COUNT(*) as count
FROM svtables.order_stages os
JOIN svtables.workshops w ON os.workshop_id = w.id
GROUP BY w.name, os.status
ORDER BY w.name, os.status;

-- Show sample stages for first 5 orders
SELECT o.order_number, o.status as order_status, os.status as stage_status, w.name as workshop, os.note
FROM svtables.orders o
JOIN svtables.order_stages os ON o.id = os.order_id
JOIN svtables.workshops w ON os.workshop_id = w.id
WHERE o.id <= 5
ORDER BY o.id, os.id;
