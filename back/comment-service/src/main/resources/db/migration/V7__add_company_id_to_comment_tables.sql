-- Комментарий-сервис работает с public.*-таблицами (создаются Hibernate).
-- Добавляем company_id, если таблицы уже существуют.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'comments') THEN
        ALTER TABLE public.comments ADD COLUMN IF NOT EXISTS company_id BIGINT;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'comment_replies') THEN
        ALTER TABLE public.comment_replies ADD COLUMN IF NOT EXISTS company_id BIGINT;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'images') THEN
        ALTER TABLE public.images ADD COLUMN IF NOT EXISTS company_id BIGINT;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'notifications') THEN
        ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS company_id BIGINT;
    END IF;
END $$;
