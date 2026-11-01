-- order_items/order_materials/order_item_operations were saved without company_id,
-- so cross-service checks (file-service asserts entity ownership) rejected them.

UPDATE svschema.order_items oi
   SET company_id = o.company_id
  FROM svschema.orders o
 WHERE oi.order_id = o.id
   AND oi.company_id IS NULL;

UPDATE svschema.order_materials om
   SET company_id = o.company_id
  FROM svschema.orders o
 WHERE om.order_id = o.id
   AND om.company_id IS NULL;

UPDATE svschema.order_item_operations op
   SET company_id = o.company_id
  FROM svschema.order_items oi
  JOIN svschema.orders o ON o.id = oi.order_id
 WHERE op.order_item_id = oi.id
   AND op.company_id IS NULL;
