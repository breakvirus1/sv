-- V103: Add role_id column to svschema.employees for direct role reference from Keycloak sync
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'svschema'
        AND table_name = 'employees'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'svschema'
            AND table_name = 'employees'
            AND column_name = 'role_id'
        ) THEN
            ALTER TABLE svschema.employees ADD COLUMN role_id BIGINT;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_employees_role_id'
            AND table_schema = 'svschema'
            AND table_name = 'employees'
        ) THEN
            ALTER TABLE svschema.employees
                ADD CONSTRAINT fk_employees_role_id FOREIGN KEY (role_id) REFERENCES svschema.roles(id) ON DELETE SET NULL;
        END IF;
    END IF;
END $$;
