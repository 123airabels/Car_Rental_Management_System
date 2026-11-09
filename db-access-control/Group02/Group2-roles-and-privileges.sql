```sql
-- =========================================================
-- Group 2 - High School Event Management System
-- COMP 018 - Activity 4
-- Database: grp02_event_management_db
-- Schema: school_event_management_db
-- DBMS: PostgreSQL 18
--
-- PURPOSE:
-- Inspect existing database roles, memberships,
-- privileges, and access restrictions.
--
-- NOTE:
-- This is a verification script.
-- It does not create roles or modify existing privileges.
-- =========================================================


-- =========================================================
-- 1. DATABASE AND CURRENT ACCOUNT
-- =========================================================

SELECT
    current_database() AS database_name,
    current_user AS connected_account;


-- =========================================================
-- 2. POSTGRESQL ROLES
-- =========================================================

SELECT
    rolname AS role_name,
    rolcanlogin AS can_login,
    rolsuper AS is_superuser,
    rolcreatedb AS can_create_database,
    rolcreaterole AS can_create_role
FROM pg_roles
WHERE rolname IN (
    'event_app',
    'event_owner',
    'app_runtime_grp',
    'app_admin_grp',
    'postgres'
)
ORDER BY rolname;


-- =========================================================
-- 3. ROLE MEMBERSHIPS
-- =========================================================

SELECT
    member.rolname AS member_role,
    granted.rolname AS granted_group_role
FROM pg_auth_members AS m
JOIN pg_roles AS granted
    ON granted.oid = m.roleid
JOIN pg_roles AS member
    ON member.oid = m.member
WHERE member.rolname IN (
    'event_app',
    'event_owner'
)
OR granted.rolname IN (
    'app_runtime_grp',
    'app_admin_grp'
)
ORDER BY member_role, granted_group_role;


-- =========================================================
-- 4. DATABASE PRIVILEGES
-- =========================================================

SELECT
    rolname AS role_name,
    has_database_privilege(
        rolname,
        current_database(),
        'CONNECT'
    ) AS can_connect,
    has_database_privilege(
        rolname,
        current_database(),
        'CREATE'
    ) AS can_create_in_database,
    has_database_privilege(
        rolname,
        current_database(),
        'TEMP'
    ) AS can_create_temp_tables
FROM pg_roles
WHERE rolname IN (
    'event_app',
    'event_owner',
    'app_runtime_grp',
    'app_admin_grp'
)
ORDER BY rolname;


-- =========================================================
-- 5. SCHEMA PRIVILEGES
-- =========================================================

SELECT
    rolname AS role_name,
    has_schema_privilege(
        rolname,
        'school_event_management_db',
        'USAGE'
    ) AS can_use_schema,
    has_schema_privilege(
        rolname,
        'school_event_management_db',
        'CREATE'
    ) AS can_create_objects
FROM pg_roles
WHERE rolname IN (
    'event_app',
    'event_owner',
    'app_runtime_grp',
    'app_admin_grp'
)
ORDER BY rolname;


-- =========================================================
-- 6. TABLE PRIVILEGES
-- =========================================================

SELECT
    grantee,
    table_name,
    privilege_type,
    is_grantable
FROM information_schema.role_table_grants
WHERE table_schema = 'school_event_management_db'
  AND grantee IN (
      'event_app',
      'event_owner',
      'app_runtime_grp',
      'app_admin_grp'
  )
ORDER BY grantee, table_name, privilege_type;


-- =========================================================
-- 7. SEQUENCE PRIVILEGES
-- =========================================================

SELECT
    sequence_schema,
    sequence_name,
    has_sequence_privilege(
        'event_app',
        format('%I.%I', sequence_schema, sequence_name),
        'USAGE'
    ) AS app_usage,
    has_sequence_privilege(
        'event_app',
        format('%I.%I', sequence_schema, sequence_name),
        'SELECT'
    ) AS app_select,
    has_sequence_privilege(
        'event_app',
        format('%I.%I', sequence_schema, sequence_name),
        'UPDATE'
    ) AS app_update
FROM information_schema.sequences
WHERE sequence_schema = 'school_event_management_db'
ORDER BY sequence_name;


-- =========================================================
-- 8. SEQUENCE OWNERSHIP
-- =========================================================

SELECT
    sequence_schema,
    sequence_name,
    sequence_owner
FROM information_schema.sequences
WHERE sequence_schema = 'school_event_management_db'
ORDER BY sequence_name;


-- =========================================================
-- 9. DEFAULT PRIVILEGES
-- =========================================================

SELECT
    defaclrole::regrole AS creator_role,
    defaclnamespace::regnamespace AS schema_name,
    defaclobjtype AS object_type,
    defaclacl AS default_acl
FROM pg_default_acl
WHERE defaclnamespace = (
    SELECT oid
    FROM pg_namespace
    WHERE nspname = 'school_event_management_db'
);


-- =========================================================
-- 10. DATABASE OWNER
-- =========================================================

SELECT
    datname AS database_name,
    pg_get_userbyid(datdba) AS database_owner
FROM pg_database
WHERE datname = current_database();


-- =========================================================
-- 11. RUNTIME ACCOUNT PERMISSION TESTS
-- Run these separately while connected as event_app.
-- Expected denied operations are commented out.
-- =========================================================

-- 11.1 ALLOWED: Read event records

-- SELECT *
-- FROM school_event_management_db.event
-- LIMIT 5;


-- 11.2 DENIED: Create a table in the application schema

-- CREATE TABLE school_event_management_db.permission_test (
--     test_id INTEGER
-- );


-- 11.3 DENIED: Truncate an event table

-- TRUNCATE TABLE school_event_management_db.event;


-- 11.4 DENIED: Create a PostgreSQL role

-- CREATE ROLE activity4_permission_test_role;


-- 11.5 ALLOWED: Create a temporary table

-- CREATE TEMP TABLE activity4_temp_test (
--     test_id INTEGER
-- );

-- DROP TABLE activity4_temp_test;


-- =========================================================
-- 12. TABLE-SPECIFIC PRIVILEGE CHECK
-- =========================================================

SELECT
    table_name,
    privilege_type
FROM information_schema.role_table_grants
WHERE table_schema = 'school_event_management_db'
  AND grantee = 'event_app'
ORDER BY table_name, privilege_type;


-- =========================================================
-- END OF VERIFICATION SCRIPT
-- =========================================================
```
