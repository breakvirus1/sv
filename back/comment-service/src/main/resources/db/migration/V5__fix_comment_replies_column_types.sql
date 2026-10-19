-- Комментарии/ответы живут в svschema (V1/V2), legacy public.* таблицы могут не существовать.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'comment_replies') THEN
        ALTER TABLE public.comment_replies ALTER COLUMN employee_name TYPE TEXT;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'comments') THEN
        ALTER TABLE public.comments ALTER COLUMN employee_name TYPE TEXT;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'comment_replies') THEN
        ALTER TABLE public.comment_replies ALTER COLUMN image_url TYPE TEXT;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'comments') THEN
        ALTER TABLE public.comments ALTER COLUMN image_url TYPE TEXT;
    END IF;
END $$;
