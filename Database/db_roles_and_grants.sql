-- PostgreSQL group roles and least-privilege grants (see Docs/User_and_Role_Documantation.md).
-- Application roles (Customer, Staff, Finance, Admin, System Admin) are enforced by the Spring Boot backend, NOT by these roles.
-- The application connects as ONE runtime login that is a member of app_runtime_grp.
-- Run once as the database owner / an authorized DBA, after schema.sql:
--   psql -U postgres -d car_rental_db -f db_roles_and_grants.sql
-- Note: DataSeeder runs a few ALTER TABLE / CREATE TABLE IF NOT EXISTS statements at start-up. A runtime login without DDL rights skips them
-- (each is wrapped in try/catch), so apply payment_proof.sql, payment_codes.sql, damage_dispute_resolution.sql and inspection_checklist.sql once as the owner.
-- Then create logins with passwords taken from your secret store (never commit passwords), for example:
--   CREATE ROLE car_rental_app LOGIN PASSWORD '...' IN ROLE app_runtime_grp;
--   CREATE ROLE car_rental_report LOGIN PASSWORD '...' IN ROLE app_readonly_grp;
--   CREATE ROLE car_rental_dba LOGIN PASSWORD '...' IN ROLE app_admin_grp;   -- only if a DBA login is needed

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_runtime_grp')  THEN CREATE ROLE app_runtime_grp  NOLOGIN; END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_readonly_grp') THEN CREATE ROLE app_readonly_grp NOLOGIN; END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_admin_grp')    THEN CREATE ROLE app_admin_grp    NOLOGIN; END IF;
END $$;

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM PUBLIC;

DO $$ BEGIN EXECUTE format('GRANT CONNECT ON DATABASE %I TO app_runtime_grp, app_readonly_grp, app_admin_grp', current_database()); END $$;
GRANT USAGE ON SCHEMA public TO app_runtime_grp, app_readonly_grp, app_admin_grp;

-- Runtime: day-to-day reads and writes. No DELETE except where the application really deletes rows.
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO app_runtime_grp;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_runtime_grp;
-- The audit log is append-only for the application: nobody edits history.
REVOKE UPDATE, DELETE ON public.audit_log FROM app_runtime_grp;
-- The application deletes only vehicles (Admin, when a vehicle has no history); remove this line if you only ever deactivate them.
GRANT DELETE ON public.vehicle TO app_runtime_grp;

-- Read-only: reporting and inspection. The password hashes stay out of reach.
GRANT SELECT ON ALL TABLES IN SCHEMA public TO app_readonly_grp;
REVOKE SELECT ON public.staff, public.customer FROM app_readonly_grp;
GRANT SELECT (staff_id, first_name, last_name, role, account_status, created_at) ON public.staff TO app_readonly_grp;
GRANT SELECT (customer_id, first_name, last_name, account_status, created_at) ON public.customer TO app_readonly_grp;

-- Admin group: additional DBA-style rights on the application's objects. Deliberately not a superuser, no CREATEDB / CREATEROLE.
GRANT app_runtime_grp TO app_admin_grp;
GRANT ALL ON ALL TABLES IN SCHEMA public TO app_admin_grp;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO app_admin_grp;

-- Objects created later get the same treatment.
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE ON TABLES TO app_runtime_grp;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO app_runtime_grp;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO app_readonly_grp;

-- Verify (run as each login): the runtime login should fail the first statement, succeed on the second.
--   DELETE FROM public.audit_log;                 -- expected: permission denied
--   SELECT count(*) FROM public.rental;           -- expected: works
