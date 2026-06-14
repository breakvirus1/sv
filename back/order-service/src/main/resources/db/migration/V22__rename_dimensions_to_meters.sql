-- Rename dimension columns from mm to meters (idempotent)
-- Handles case where both _mm and _m columns exist

-- order_materials: width_mm -> width_m, height_mm -> height_m
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='order_materials' AND column_name='width_mm') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='order_materials' AND column_name='width_m') THEN
            ALTER TABLE svtables.order_materials DROP COLUMN width_m;
        END IF;
        ALTER TABLE svtables.order_materials RENAME COLUMN width_mm TO width_m;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='order_materials' AND column_name='height_mm') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='order_materials' AND column_name='height_m') THEN
            ALTER TABLE svtables.order_materials DROP COLUMN height_m;
        END IF;
        ALTER TABLE svtables.order_materials RENAME COLUMN height_mm TO height_m;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='order_materials' AND column_name='width_m' AND numeric_scale != 4) THEN
        ALTER TABLE svtables.order_materials ALTER COLUMN width_m TYPE NUMERIC(10,4);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='order_materials' AND column_name='height_m' AND numeric_scale != 4) THEN
        ALTER TABLE svtables.order_materials ALTER COLUMN height_m TYPE NUMERIC(10,4);
    END IF;
END $$;

-- order_item_operations: width_mm -> width_m, height_mm -> height_m
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='order_item_operations' AND column_name='width_mm') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='order_item_operations' AND column_name='width_m') THEN
            ALTER TABLE svtables.order_item_operations DROP COLUMN width_m;
        END IF;
        ALTER TABLE svtables.order_item_operations RENAME COLUMN width_mm TO width_m;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='order_item_operations' AND column_name='height_mm') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='order_item_operations' AND column_name='height_m') THEN
            ALTER TABLE svtables.order_item_operations DROP COLUMN height_m;
        END IF;
        ALTER TABLE svtables.order_item_operations RENAME COLUMN height_mm TO height_m;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='order_item_operations' AND column_name='width_m' AND numeric_scale != 4) THEN
        ALTER TABLE svtables.order_item_operations ALTER COLUMN width_m TYPE NUMERIC(10,4);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='order_item_operations' AND column_name='height_m' AND numeric_scale != 4) THEN
        ALTER TABLE svtables.order_item_operations ALTER COLUMN height_m TYPE NUMERIC(10,4);
    END IF;
END $$;

-- materials: default_width_mm -> default_width_m, default_height_mm -> default_height_m
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='materials' AND column_name='default_width_mm') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='materials' AND column_name='default_width_m') THEN
            ALTER TABLE svtables.materials DROP COLUMN default_width_m;
        END IF;
        ALTER TABLE svtables.materials RENAME COLUMN default_width_mm TO default_width_m;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='materials' AND column_name='default_height_mm') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='materials' AND column_name='default_height_m') THEN
            ALTER TABLE svtables.materials DROP COLUMN default_height_m;
        END IF;
        ALTER TABLE svtables.materials RENAME COLUMN default_height_mm TO default_height_m;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='materials' AND column_name='default_width_m' AND numeric_scale != 4) THEN
        ALTER TABLE svtables.materials ALTER COLUMN default_width_m TYPE NUMERIC(10,4);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='svtables' AND table_name='materials' AND column_name='default_height_m' AND numeric_scale != 4) THEN
        ALTER TABLE svtables.materials ALTER COLUMN default_height_m TYPE NUMERIC(10,4);
    END IF;
END $$;
