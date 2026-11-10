
-- =========================================================
-- Group 09 - Batumbakal Gym
-- COMP 018 - Activity 4
-- Database: grp99_gymdb
-- Schema: public
-- DBMS: PostgreSQL 18
--
-- PURPOSE:
-- Inspect the existing database, PostgreSQL roles,
-- memberships, and access privileges.
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
-- 2. DATABASE OWNER
-- =========================================================

SELECT
    datname AS database_name,
    pg_get_userbyid(datdba) AS database_owner
FROM pg_database
WHERE datname = current_database();


-- =========================================================
-- 3. DATABASE TABLES
-- =========================================================

SELECT
    schemaname,
    tablename
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;


-- =========================================================
-- 4. DATABASE SEQUENCES
-- =========================================================

SELECT
    sequence_schema,
    sequence_name
FROM information_schema.sequences
WHERE sequence_schema = 'public'
ORDER BY sequence_name;


-- =========================================================
-- 5. POSTGRESQL ROLES
-- =========================================================

SELECT
    rolname AS role_name,
    rolcanlogin AS can_login,
    rolsuper AS is_superuser,
    rolcreatedb AS can_create_database,
    rolcreaterole AS can_create_role
FROM pg_roles
WHERE rolname IN (
    'postgres',
    'activity4_test_role',
    'gym_app',
    'gym_readonly'
)
ORDER BY rolname;


-- =========================================================
-- 6. ROLE MEMBERSHIPS
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
    'postgres',
    'activity4_test_role',
    'gym_app',
    'gym_readonly'
)
OR granted.rolname IN (
    'postgres',
    'activity4_test_role',
    'gym_app',
    'gym_readonly'
)
ORDER BY member_role, granted_group_role;


-- =========================================================
-- 7. DATABASE PRIVILEGES
-- =========================================================

SELECT
    rolname AS role_name,
    has_database_privilege(
        rolname, current_database(), 'CONNECT'
    ) AS can_connect,
    has_database_privilege(
        rolname, current_database(), 'CREATE'
    ) AS can_create,
    has_database_privilege(
        rolname, current_database(), 'TEMP'
    ) AS can_create_temp_tables
FROM pg_roles
WHERE rolname IN (
    'postgres',
    'activity4_test_role'
)
ORDER BY rolname;


-- =========================================================
-- 8. SCHEMA PRIVILEGES
-- =========================================================

SELECT
    rolname AS role_name,
    has_schema_privilege(
        rolname, 'public', 'USAGE'
    ) AS can_use_schema,
    has_schema_privilege(
        rolname, 'public', 'CREATE'
    ) AS can_create_objects
FROM pg_roles
WHERE rolname IN (
    'postgres',
    'activity4_test_role'
)
ORDER BY rolname;


-- =========================================================
-- 9. TABLE PRIVILEGES
-- =========================================================

SELECT
    grantee,
    table_name,
    privilege_type,
    is_grantable
FROM information_schema.role_table_grants
WHERE table_schema = 'public'
  AND grantee IN (
      'postgres',
      'activity4_test_role'
  )
ORDER BY grantee, table_name, privilege_type;


-- =========================================================
-- 10. SEQUENCE PRIVILEGES
-- =========================================================

SELECT
    sequence_schema,
    sequence_name,
    has_sequence_privilege(
        'activity4_test_role',
        format('%I.%I', sequence_schema, sequence_name),
        'USAGE'
    ) AS can_use,
    has_sequence_privilege(
        'activity4_test_role',
        format('%I.%I', sequence_schema, sequence_name),
        'SELECT'
    ) AS can_select,
    has_sequence_privilege(
        'activity4_test_role',
        format('%I.%I', sequence_schema, sequence_name),
        'UPDATE'
    ) AS can_update
FROM information_schema.sequences
WHERE sequence_schema = 'public'
ORDER BY sequence_name;


-- =========================================================
-- 11. DEFAULT PRIVILEGES
-- =========================================================

SELECT
    COALESCE(n.nspname, '(Global)') AS target_schema,
    r.rolname AS object_creator,
    d.defaclobjtype AS object_type,
    d.defaclacl AS default_privileges
FROM pg_default_acl d
JOIN pg_roles r
    ON r.oid = d.defaclrole
LEFT JOIN pg_namespace n
    ON n.oid = d.defaclnamespace
WHERE d.defaclnamespace = 0
   OR d.defaclnamespace = 'public'::regnamespace
ORDER BY target_schema, object_creator, object_type;


-- =========================================================
-- END OF DATABASE INSPECTION SCRIPT
-- =========================================================
