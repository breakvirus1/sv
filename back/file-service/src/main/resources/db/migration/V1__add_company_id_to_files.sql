-- Добавляем company_id к файлам (svschema.files создаётся в order-service V1).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'svschema' AND table_name = 'files') THEN
        ALTER TABLE svschema.files ADD COLUMN IF NOT EXISTS company_id BIGINT;
    END IF;
END $$;
