ALTER TABLE public.comment_replies
    ALTER COLUMN employee_name TYPE TEXT;

ALTER TABLE public.comment_replies
    ALTER COLUMN image_url TYPE TEXT;

ALTER TABLE public.comments
    ALTER COLUMN employee_name TYPE TEXT;

ALTER TABLE public.comments
    ALTER COLUMN image_url TYPE TEXT;
