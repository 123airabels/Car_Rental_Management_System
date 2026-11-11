
-- Group04 - HealthBridge Clinic Management System
-- Activity 4: Database Access Control
-- Database: grp03_projectdb
-- Schema: public
-- No passwords are stored in this script.

-- 1. Create roles if they do not already exist.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_roles WHERE rolname = 'hb_admin'
    ) THEN
        CREATE ROLE hb_admin NOLOGIN INHERIT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_roles WHERE rolname = 'hb_staff'
    ) THEN
        CREATE ROLE hb_staff NOLOGIN INHERIT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_roles WHERE rolname = 'hb_app'
    ) THEN
        CREATE ROLE hb_app LOGIN INHERIT;
    END IF;
END
$$;

-- 2. Database and schema access.
GRANT CONNECT ON DATABASE grp03_projectdb
TO hb_admin, hb_staff, hb_app;

GRANT USAGE ON SCHEMA public
TO hb_admin, hb_staff, hb_app;

-- 3. Administrative privileges.
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public
TO hb_admin;

GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public
TO hb_admin;

-- 4. Staff data access.
GRANT SELECT, INSERT, UPDATE
ON patients, appointments, medical_notes, prescriptions
TO hb_staff;

GRANT DELETE
ON patients, appointments, medical_notes, prescriptions
TO hb_staff;

GRANT SELECT ON doctors, specialties
TO hb_staff;

GRANT SELECT, UPDATE ON staff_users
TO hb_staff;

GRANT SELECT, INSERT, UPDATE
ON staff_password_resets, patient_password_resets
TO hb_staff;

-- 5. Billing and backup records.
GRANT SELECT, INSERT, UPDATE, DELETE
ON billing, deleted_records
TO hb_staff;

-- 6. Sequence permissions.
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public
TO hb_staff;

-- 7. Application account inherits staff permissions.
GRANT hb_staff TO hb_app;

-- 8. View access (added after initial permission testing).
GRANT SELECT ON appointment_details, visit_records
TO hb_staff;

-- 9. Default privileges for future tables created by postgres.
ALTER DEFAULT PRIVILEGES
FOR ROLE postgres
IN SCHEMA public
GRANT ALL PRIVILEGES ON TABLES TO hb_admin;
