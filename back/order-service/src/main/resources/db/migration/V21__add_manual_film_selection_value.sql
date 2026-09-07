-- Add manual_film_selection_value to order_materials
ALTER TABLE svschema.order_materials ADD COLUMN IF NOT EXISTS manual_film_selection_value INTEGER;

UPDATE svschema.order_materials SET manual_film_selection_value = 0 WHERE manual_film_selection_value IS NULL;
