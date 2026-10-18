ALTER TABLE public.comment_replies
    ALTER COLUMN employee_name TYPE TEXT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'comment_replies' AND column_name = 'image_url'
    ) THEN
        ALTER TABLE public.comment_replies ALTER COLUMN image_url TYPE TEXT;
    END IF;
END $$;

ALTER TABLE public.comments
    ALTER COLUMN employee_name TYPE TEXT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'comments' AND column_name = 'image_url'
    ) THEN
        ALTER TABLE public.comments ALTER COLUMN image_url TYPE TEXT;
    END IF;
END $$;
