-- Fix notification reference IDs to use order_id instead of reply_id/comment_id
-- This migration is idempotent: it only updates if the reference_id doesn't match the expected order_id

UPDATE notifications n
SET reference_id = cr.order_id
FROM comment_replies cr
WHERE n.type = 'REPLY'
  AND n.reference_id = cr.id
  AND n.reference_id != cr.order_id;

UPDATE notifications n
SET reference_id = c.order_id
FROM comments c
WHERE n.type = 'COMMENT'
  AND n.reference_id = c.id
  AND n.reference_id != c.order_id;
