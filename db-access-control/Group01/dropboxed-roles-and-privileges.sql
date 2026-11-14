# DropBoxed Database Roles and Privileges
**Course:** COMP 018 – Database Administration  
**Activity:** Activity 4 – Database Users, Roles, Privileges, and Access Control  
**Project:** DropBoxed (Group 1)  
**Database:** `dropboxed_db`  
**Schema:** `dropboxed`  
**Database Platform:** PostgreSQL 18.6 (MAINTAIN requires 17 or newer)

---

```sql
-- =========================================================
-- DropBoxed Database Roles and Privileges
-- COMP 018 - Activity 4
-- Database: dropboxed_db
-- Schema: dropboxed
--
-- Run as postgres. Passwords are NOT stored here; they are set
-- from .env/.env.setup by npm run db:setup.
-- Safe to run again: step 4 removes earlier grants first.
-- =========================================================


-- =========================================================
-- 1. CREATE GROUP ROLES
-- Privilege containers only; they cannot log in.
-- =========================================================

CREATE ROLE dropboxed_runtime_grp NOLOGIN;
CREATE ROLE dropboxed_readonly_grp NOLOGIN;
CREATE ROLE dropboxed_dba_grp NOLOGIN;
CREATE ROLE dropboxed_backup_grp NOLOGIN;

-- The DBA group can monitor every session without superuser rights.
GRANT pg_monitor TO dropboxed_dba_grp;


-- =========================================================
-- 2. CREATE LOGIN ROLES
-- Each belongs to exactly one group role and holds no
-- direct grants of its own.
-- =========================================================

CREATE ROLE dropboxed_app LOGIN;
CREATE ROLE dropboxed_report LOGIN;
CREATE ROLE dropboxed_dba LOGIN;
CREATE ROLE dropboxed_backup LOGIN;

-- Cap the application pool.
ALTER ROLE dropboxed_app CONNECTION LIMIT 20;


-- =========================================================
-- 3. ROLE MEMBERSHIPS
-- =========================================================

GRANT dropboxed_runtime_grp TO dropboxed_app;
GRANT dropboxed_readonly_grp TO dropboxed_report;
GRANT dropboxed_dba_grp TO dropboxed_dba;
GRANT dropboxed_backup_grp TO dropboxed_backup;


-- =========================================================
-- 4. RESET EARLIER GRANTS
-- Makes this script the complete statement of who can do what.
-- =========================================================

REVOKE ALL ON ALL TABLES IN SCHEMA dropboxed
    FROM dropboxed_runtime_grp, dropboxed_readonly_grp,
         dropboxed_dba_grp, dropboxed_backup_grp;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA dropboxed
    FROM dropboxed_runtime_grp, dropboxed_readonly_grp,
         dropboxed_dba_grp, dropboxed_backup_grp;
REVOKE ALL ON SCHEMA dropboxed
    FROM dropboxed_runtime_grp, dropboxed_readonly_grp,
         dropboxed_dba_grp, dropboxed_backup_grp;


-- =========================================================
-- 5. DATABASE AND SCHEMA PRIVILEGES
-- Run while connected to dropboxed_db.
-- =========================================================

-- Close the database to logins that belong to no group.
REVOKE CONNECT, TEMP ON DATABASE dropboxed_db FROM PUBLIC;

-- Only the four group roles may connect. TEMP is granted to none.
GRANT CONNECT ON DATABASE dropboxed_db
TO dropboxed_runtime_grp,
   dropboxed_readonly_grp,
   dropboxed_dba_grp,
   dropboxed_backup_grp;

-- Close both schemas to PUBLIC. The application stores nothing in public.
REVOKE ALL ON SCHEMA dropboxed FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM PUBLIC;

-- USAGE only: no group may create objects.
GRANT USAGE ON SCHEMA dropboxed
TO dropboxed_runtime_grp,
   dropboxed_readonly_grp,
   dropboxed_dba_grp,
   dropboxed_backup_grp;


-- =========================================================
-- 6. RUNTIME PRIVILEGES (dropboxed_runtime_grp)
-- Mirrors the operations in src/db/*.js.
-- =========================================================

GRANT SELECT, INSERT ON
    dropboxed.users,
    dropboxed.staff,
    dropboxed.complaint_categories,
    dropboxed.locations,
    dropboxed.complaints,
    dropboxed.complaint_assignments,
    dropboxed.status_logs,
    dropboxed.attachments
TO dropboxed_runtime_grp;

-- Column-scoped UPDATE: no email columns, no submitted_at.
GRANT UPDATE (full_name, phone_number, password_hash, account_status)
    ON dropboxed.users TO dropboxed_runtime_grp;
GRANT UPDATE (password_hash, account_status)
    ON dropboxed.staff TO dropboxed_runtime_grp;
GRANT UPDATE (category_name, description)
    ON dropboxed.complaint_categories TO dropboxed_runtime_grp;
GRANT UPDATE (status, resolved_at, is_flagged, flag_reason)
    ON dropboxed.complaints TO dropboxed_runtime_grp;

-- DELETE only where the application needs it.
-- complaints, users, staff: never deleted (Business Rules 11 and 12).
-- status_logs: append-only (Business Rule 7).
-- attachments, locations: no runtime DELETE.
GRANT DELETE ON dropboxed.complaint_categories TO dropboxed_runtime_grp;
GRANT DELETE ON dropboxed.complaint_assignments TO dropboxed_runtime_grp;

-- No sequence privileges: every key is GENERATED ALWAYS AS IDENTITY,
-- so INSERT advances the sequence without a grant and nextval() is denied.


-- =========================================================
-- 7. READ-ONLY PRIVILEGES (dropboxed_readonly_grp)
-- No credentials, no contact details, no staff rows, no writes.
-- =========================================================

GRANT SELECT ON
    dropboxed.complaint_categories,
    dropboxed.locations,
    dropboxed.complaints,
    dropboxed.complaint_assignments,
    dropboxed.status_logs,
    dropboxed.attachments
TO dropboxed_readonly_grp;

GRANT SELECT (user_id, full_name, account_status, created_at)
    ON dropboxed.users TO dropboxed_readonly_grp;
GRANT SELECT (staff_id, full_name, account_status, created_at)
    ON dropboxed.staff TO dropboxed_readonly_grp;


-- =========================================================
-- 8. BACKUP PRIVILEGES (dropboxed_backup_grp)
-- Everything pg_dump needs; nothing that can change data.
-- =========================================================

GRANT SELECT ON ALL TABLES IN SCHEMA dropboxed
TO dropboxed_backup_grp;

GRANT SELECT ON ALL SEQUENCES IN SCHEMA dropboxed
TO dropboxed_backup_grp;


-- =========================================================
-- 9. DBA PRIVILEGES (dropboxed_dba_grp)
-- Data correction plus maintenance, but no ownership:
-- DROP, ALTER, and TRUNCATE stay denied.
-- MAINTAIN requires PostgreSQL 17 or newer.
-- =========================================================

GRANT SELECT, INSERT, UPDATE, DELETE, MAINTAIN ON ALL TABLES IN SCHEMA dropboxed
TO dropboxed_dba_grp;

GRANT SELECT ON ALL SEQUENCES IN SCHEMA dropboxed
TO dropboxed_dba_grp;


-- =========================================================
-- 10. DEFAULT PRIVILEGES FOR FUTURE OBJECTS
-- Creator role: postgres. Schema: dropboxed.
-- Runtime and read-only receive nothing by default.
-- =========================================================

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA dropboxed
GRANT SELECT ON TABLES TO dropboxed_backup_grp;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA dropboxed
GRANT SELECT ON SEQUENCES TO dropboxed_backup_grp;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA dropboxed
GRANT SELECT, INSERT, UPDATE, DELETE, MAINTAIN ON TABLES TO dropboxed_dba_grp;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA dropboxed
GRANT SELECT ON SEQUENCES TO dropboxed_dba_grp;


-- =========================================================
-- 11. VERIFICATION QUERIES
-- Run these separately when checking the setup.
-- =========================================================


-- 11.1 Role login and administrative attributes
-- Expect: group roles rolcanlogin = f, login roles rolcanlogin = t,
-- and no elevated attribute on any row.

SELECT
    rolname,
    rolcanlogin,
    rolsuper,
    rolcreatedb,
    rolcreaterole,
    rolreplication,
    rolbypassrls,
    rolconnlimit
FROM pg_roles
WHERE rolname IN (
    'dropboxed_app',
    'dropboxed_report',
    'dropboxed_dba',
    'dropboxed_backup',
    'dropboxed_runtime_grp',
    'dropboxed_readonly_grp',
    'dropboxed_dba_grp',
    'dropboxed_backup_grp'
)
ORDER BY rolname;


-- 11.2 Role memberships
-- Expect one group role per login role, plus pg_monitor for the DBA group.

SELECT
    member_role.rolname AS role_name,
    parent_role.rolname AS member_of
FROM pg_auth_members AS m
JOIN pg_roles AS parent_role ON parent_role.oid = m.roleid
JOIN pg_roles AS member_role ON member_role.oid = m.member
WHERE member_role.rolname LIKE 'dropboxed\_%'
   OR parent_role.rolname LIKE 'dropboxed\_%'
ORDER BY member_role.rolname, parent_role.rolname;


-- 11.3 Database and schema privileges
-- Expect can_connect = t, can_use_schema = t, can_create_objects = f.

SELECT
    rolname,
    has_database_privilege(rolname, current_database(), 'CONNECT') AS can_connect,
    has_database_privilege(rolname, current_database(), 'TEMP')    AS can_temp,
    has_schema_privilege(rolname, 'dropboxed', 'USAGE')            AS can_use_schema,
    has_schema_privilege(rolname, 'dropboxed', 'CREATE')           AS can_create_objects
FROM pg_roles
WHERE rolname IN (
    'dropboxed_app',
    'dropboxed_report',
    'dropboxed_dba',
    'dropboxed_backup'
)
ORDER BY rolname;


-- 11.4 Table privileges granted to each group role

SELECT
    grantee,
    table_name,
    privilege_type
FROM information_schema.role_table_grants
WHERE grantee LIKE 'dropboxed\_%'
  AND table_schema = 'dropboxed'
ORDER BY grantee, table_name, privilege_type;


-- 11.5 Column-level privileges granted to each group role
-- Shows the column lists that limit runtime UPDATE and read-only SELECT.

SELECT
    grantee,
    table_name,
    column_name,
    privilege_type
FROM information_schema.role_column_grants
WHERE grantee LIKE 'dropboxed\_%'
  AND table_schema = 'dropboxed'
ORDER BY grantee, table_name, column_name, privilege_type;


-- 11.6 Effective table privileges of the runtime account
-- Expected pattern: app_delete = f on complaints, users, staff,
-- status_logs, attachments, locations; app_update = f on status_logs.

SELECT
    table_name,
    has_table_privilege('dropboxed_app', format('%I.%I', table_schema, table_name), 'SELECT') AS app_select,
    has_table_privilege('dropboxed_app', format('%I.%I', table_schema, table_name), 'INSERT') AS app_insert,
    has_table_privilege('dropboxed_app', format('%I.%I', table_schema, table_name), 'UPDATE') AS app_update,
    has_table_privilege('dropboxed_app', format('%I.%I', table_schema, table_name), 'DELETE') AS app_delete,
    has_table_privilege('dropboxed_app', format('%I.%I', table_schema, table_name), 'TRUNCATE') AS app_truncate
FROM information_schema.tables
WHERE table_schema = 'dropboxed'
  AND table_type = 'BASE TABLE'
ORDER BY table_name;


-- 11.7 Column-scoped checks for the runtime account
-- Expect: app_can_update_email = f, app_can_update_submitted_at = f,
-- app_can_update_status = t.

SELECT
    has_column_privilege('dropboxed_app', 'dropboxed.users', 'email', 'UPDATE') AS app_can_update_email,
    has_column_privilege('dropboxed_app', 'dropboxed.users', 'full_name', 'UPDATE') AS app_can_update_full_name,
    has_column_privilege('dropboxed_app', 'dropboxed.complaints', 'submitted_at', 'UPDATE') AS app_can_update_submitted_at,
    has_column_privilege('dropboxed_app', 'dropboxed.complaints', 'status', 'UPDATE') AS app_can_update_status;


-- 11.8 Read-only account must not see credentials or staff rows
-- Expect all four to be f.

SELECT
    has_column_privilege('dropboxed_report', 'dropboxed.users', 'password_hash', 'SELECT') AS report_can_read_hash,
    has_column_privilege('dropboxed_report', 'dropboxed.users', 'email', 'SELECT')         AS report_can_read_email,
    has_column_privilege('dropboxed_report', 'dropboxed.users', 'phone_number', 'SELECT')  AS report_can_read_phone,
    has_table_privilege('dropboxed_report', 'dropboxed.staff', 'SELECT')                   AS report_can_read_staff;


-- 11.9 Sequence privileges
-- Expect runtime_usage = f for every row; backup and DBA = t.

SELECT
    sequence_schema,
    sequence_name,
    has_sequence_privilege('dropboxed_runtime_grp', format('%I.%I', sequence_schema, sequence_name), 'USAGE') AS runtime_usage,
    has_sequence_privilege('dropboxed_backup_grp',  format('%I.%I', sequence_schema, sequence_name), 'SELECT') AS backup_select,
    has_sequence_privilege('dropboxed_dba_grp',     format('%I.%I', sequence_schema, sequence_name), 'SELECT') AS dba_select
FROM information_schema.sequences
WHERE sequence_schema = 'dropboxed'
ORDER BY sequence_name;


-- 11.10 PUBLIC database privileges
-- Expect no CONNECT or TEMP row for this database.

SELECT
    d.datname,
    a.privilege_type,
    a.is_grantable
FROM pg_database AS d
CROSS JOIN LATERAL aclexplode(
    COALESCE(d.datacl, acldefault('d', d.datdba))
) AS a
WHERE d.datname = current_database()
  AND a.grantee = 0
ORDER BY a.privilege_type;


-- 11.11 Object ownership
-- Expect owner = postgres for the database, schema, tables, and sequences.

SELECT
    c.relname,
    c.relkind,
    pg_get_userbyid(c.relowner) AS owner
FROM pg_class AS c
WHERE c.relnamespace = 'dropboxed'::regnamespace
  AND c.relkind IN ('r', 'S')
ORDER BY c.relkind, c.relname;

SELECT
    n.nspname AS schema_name,
    pg_get_userbyid(n.nspowner) AS owner
FROM pg_namespace AS n
WHERE n.nspname = 'dropboxed';


-- 11.12 Total tables and sequences in the dropboxed schema
-- Expect 8 tables and 8 sequences.

SELECT COUNT(*) AS total_tables
FROM information_schema.tables
WHERE table_schema = 'dropboxed'
  AND table_type = 'BASE TABLE';

SELECT COUNT(*) AS total_sequences
FROM pg_class AS c
JOIN pg_namespace AS n ON n.oid = c.relnamespace
WHERE c.relkind = 'S'
  AND n.nspname = 'dropboxed';


-- =========================================================
-- END OF DROPBOXED DATABASE ROLES AND PRIVILEGES
-- =========================================================
```
