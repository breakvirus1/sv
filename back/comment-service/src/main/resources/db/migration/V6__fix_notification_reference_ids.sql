-- Fix notification reference IDs to use order_id instead of reply_id/comment_id
-- This migration is idempotent: it only updates if the reference_id doesn't match the expected order_id
-- Таблицы notifications/comment_replies/comments создаются Hibernate в public (могут отсутствовать при чистой БД).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'notifications')
       AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'comment_replies')
       AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'comments') THEN
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
    END IF;
END $$;
