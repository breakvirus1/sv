-- Change eyelet_quantity from NUMERIC to INTEGER
ALTER TABLE svschema.order_materials ALTER COLUMN eyelet_quantity TYPE INTEGER USING CAST(eyelet_quantity AS INTEGER);

UPDATE svschema.order_materials SET eyelet_quantity = 0 WHERE eyelet_quantity IS NULL;
