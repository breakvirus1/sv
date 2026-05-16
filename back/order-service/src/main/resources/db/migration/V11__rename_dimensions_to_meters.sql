-- Rename dimension columns from mm to meters
-- order_materials: width_mm -> width_m, height_mm -> height_m
ALTER TABLE ordschema.order_materials RENAME COLUMN width_mm TO width_m;
ALTER TABLE ordschema.order_materials RENAME COLUMN height_mm TO height_m;

-- Change precision to 4 decimal places for meters
ALTER TABLE ordschema.order_materials ALTER COLUMN width_m TYPE NUMERIC(10,4);
ALTER TABLE ordschema.order_materials ALTER COLUMN height_m TYPE NUMERIC(10,4);

-- order_item_operations: width_mm -> width_m, height_mm -> height_m
ALTER TABLE ordschema.order_item_operations RENAME COLUMN width_mm TO width_m;
ALTER TABLE ordschema.order_item_operations RENAME COLUMN height_mm TO height_m;

ALTER TABLE ordschema.order_item_operations ALTER COLUMN width_m TYPE NUMERIC(10,4);
ALTER TABLE ordschema.order_item_operations ALTER COLUMN height_m TYPE NUMERIC(10,4);

-- materials: default_width_mm -> default_width_m, default_height_mm -> default_height_m
ALTER TABLE ordschema.materials RENAME COLUMN default_width_mm TO default_width_m;
ALTER TABLE ordschema.materials RENAME COLUMN default_height_mm TO default_height_m;

ALTER TABLE ordschema.materials ALTER COLUMN default_width_m TYPE NUMERIC(10,4);
ALTER TABLE ordschema.materials ALTER COLUMN default_height_m TYPE NUMERIC(10,4);
