ALTER TABLE public.comments ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE public.comment_replies ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE public.images ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS company_id BIGINT;
