-- Hibernate's schema update does not reliably widen an existing enum CHECK constraint.
-- Fresh installations create users later through the preserved hospital JPA mapping.
DO $$
BEGIN
    IF to_regclass('users') IS NOT NULL THEN
        ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
        ALTER TABLE users ADD CONSTRAINT users_role_check
            CHECK (role IN ('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'REQUESTER', 'CLINICAL_STAFF', 'INVENTORY_MANAGER'));
    END IF;
END $$;
