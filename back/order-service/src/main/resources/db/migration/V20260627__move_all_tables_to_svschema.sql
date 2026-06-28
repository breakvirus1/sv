-- ============================================================
-- V20260627__move_all_tables_to_svschema.sql
-- Перенос всех таблиц приложения из схем ordschema, calculator, public
-- в единую схему svschema для базы данных svdb
-- ============================================================

CREATE SCHEMA IF NOT EXISTS svschema;

DO $$
DECLARE
    t RECORD;
    tbl_name TEXT;
    src_schema TEXT;
    seq_rec RECORD;
BEGIN
    FOR t IN
        SELECT table_schema, table_name
        FROM information_schema.tables
        WHERE table_schema IN ('ordschema', 'calculator')
          AND table_type = 'BASE TABLE'
          AND table_name NOT LIKE 'flyway_%'
        ORDER BY table_schema, table_name
    LOOP
        src_schema := t.table_schema;
        tbl_name := t.table_name;

        IF EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = 'svschema' AND table_name = tbl_name
        ) THEN
            CONTINUE;
        END IF;

        EXECUTE format('ALTER TABLE %I.%I SET SCHEMA svschema', src_schema, tbl_name);

        FOR seq_rec IN
            SELECT sequence_name
            FROM information_schema.sequences
            WHERE sequence_schema = src_schema
              AND sequence_name LIKE tbl_name || '%'
        LOOP
            IF NOT EXISTS (
                SELECT 1 FROM information_schema.sequences
                WHERE sequence_schema = 'svschema'
                  AND sequence_name = seq_rec.sequence_name
            ) THEN
                EXECUTE format('ALTER SEQUENCE %I.%I SET SCHEMA svschema', src_schema, seq_rec.sequence_name);
            END IF;
        END LOOP;
    END LOOP;

    -- Перенос таблиц из public (только приложения, не Keycloak)
    FOR t IN
        SELECT table_schema, table_name
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_type = 'BASE TABLE'
          AND table_name IN ('roles', 'employee_roles')
    LOOP
        src_schema := t.table_schema;
        tbl_name := t.table_name;

        IF EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = 'svschema' AND table_name = tbl_name
        ) THEN
            CONTINUE;
        END IF;

        EXECUTE format('ALTER TABLE %I.%I SET SCHEMA svschema', src_schema, tbl_name);

        FOR seq_rec IN
            SELECT sequence_name
            FROM information_schema.sequences
            WHERE sequence_schema = src_schema
              AND sequence_name LIKE tbl_name || '%'
        LOOP
            IF NOT EXISTS (
                SELECT 1 FROM information_schema.sequences
                WHERE sequence_schema = 'svschema'
                  AND sequence_name = seq_rec.sequence_name
            ) THEN
                EXECUTE format('ALTER SEQUENCE %I.%I SET SCHEMA svschema', src_schema, seq_rec.sequence_name);
            END IF;
        END LOOP;
    END LOOP;
END $$;
