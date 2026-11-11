
-- =========================================================
-- SHIORI CAFE - ACTIVITY 4
-- Database Users, Roles, Privileges, and Access Control
--
-- Database: grp10_shioricafedb
-- Schema: shioricafe_schema
-- DBMS: PostgreSQL 18
--
-- PURPOSE:
-- Verify database accounts, roles, memberships,
-- privileges, ownership, and runtime access readiness.
--
-- =========================================================


-- 1. DATABASE AND CURRENT CONNECTION

SELECT
    current_database() AS database_name,
    current_user AS connected_account;


-- 2. DATABASE OWNER

SELECT
    datname AS database_name,
    pg_get_userbyid(datdba) AS database_owner,
    datacl AS access_privileges
FROM pg_database
WHERE datname = current_database();


-- 3. SHIORI CAFE LOGIN ACCOUNTS AND GROUP ROLES

SELECT
    rolname AS role_name,
    rolcanlogin AS can_login,
    rolsuper AS is_superuser,
    rolcreatedb AS can_create_database,
    rolcreaterole AS can_create_role
FROM pg_roles
WHERE rolname IN (
    'shioricafe_app_login',
    'shioricafe_readonly_login',
    'shioricafe_migration_login',
    'app_runtime_grp',
    'app_readonly_grp',
    'app_migration_grp',
    'postgres'
)
ORDER BY rolname;


-- 4. SHIORI CAFE ROLE MEMBERSHIPS

SELECT
    member.rolname AS member_role,
    granted.rolname AS granted_group_role,
    member.rolcanlogin AS member_can_login
FROM pg_auth_members AS m
JOIN pg_roles AS granted
    ON granted.oid = m.roleid
JOIN pg_roles AS member
    ON member.oid = m.member
WHERE member.rolname ILIKE '%shioricafe%'
ORDER BY member_role, granted_group_role;


-- 5. DATABASE PRIVILEGES

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
    'shioricafe_app_login',
    'shioricafe_readonly_login',
    'shioricafe_migration_login',
    'app_runtime_grp',
    'app_readonly_grp',
    'app_migration_grp'
)
ORDER BY rolname;


-- 6. APPLICATION SCHEMA AND OWNER

SELECT
    n.nspname AS schema_name,
    pg_get_userbyid(n.nspowner) AS schema_owner
FROM pg_namespace AS n
WHERE n.nspname = 'shioricafe_schema';


-- 7. SCHEMA PRIVILEGES

SELECT
    r.rolname AS role_name,
    has_schema_privilege(
        r.rolname,
        n.nspname,
        'USAGE'
    ) AS can_use_schema,
    has_schema_privilege(
        r.rolname,
        n.nspname,
        'CREATE'
    ) AS can_create_objects
FROM pg_roles AS r
CROSS JOIN pg_namespace AS n
WHERE n.nspname = 'shioricafe_schema'
  AND r.rolname IN (
      'shioricafe_app_login',
      'shioricafe_readonly_login',
      'shioricafe_migration_login',
      'app_runtime_grp',
      'app_readonly_grp',
      'app_migration_grp'
  )
ORDER BY r.rolname;


-- 8. APPLICATION TABLES

SELECT
    schemaname AS schema_name,
    tablename AS table_name,
    tableowner AS table_owner
FROM pg_tables
WHERE schemaname = 'shioricafe_schema'
ORDER BY tablename;


-- 9. TABLE PRIVILEGES

SELECT
    grantee,
    table_name,
    privilege_type,
    is_grantable
FROM information_schema.role_table_grants
WHERE table_schema = 'shioricafe_schema'
  AND grantee IN (
      'shioricafe_app_login',
      'shioricafe_readonly_login',
      'shioricafe_migration_login',
      'app_runtime_grp',
      'app_readonly_grp',
      'app_migration_grp'
  )
ORDER BY grantee, table_name, privilege_type;


-- 10. SEQUENCE PRIVILEGES

SELECT
    s.sequence_schema,
    s.sequence_name,
    r.rolname AS role_name,
    has_sequence_privilege(
        r.rolname,
        format('%I.%I', s.sequence_schema, s.sequence_name),
        'USAGE'
    ) AS can_use,
    has_sequence_privilege(
        r.rolname,
        format('%I.%I', s.sequence_schema, s.sequence_name),
        'SELECT'
    ) AS can_read,
    has_sequence_privilege(
        r.rolname,
        format('%I.%I', s.sequence_schema, s.sequence_name),
        'UPDATE'
    ) AS can_update
FROM information_schema.sequences AS s
CROSS JOIN pg_roles AS r
WHERE s.sequence_schema = 'shioricafe_schema'
  AND r.rolname IN (
      'shioricafe_app_login',
      'shioricafe_readonly_login',
      'shioricafe_migration_login',
      'app_runtime_grp',
      'app_readonly_grp',
      'app_migration_grp'
  )
ORDER BY s.sequence_name, r.rolname;


-- 11. SEQUENCE OWNERSHIP

SELECT
    n.nspname AS sequence_schema,
    c.relname AS sequence_name,
    pg_get_userbyid(c.relowner) AS sequence_owner
FROM pg_class AS c
JOIN pg_namespace AS n
    ON n.oid = c.relnamespace
WHERE c.relkind = 'S'
  AND n.nspname = 'shioricafe_schema'
ORDER BY c.relname;


-- 12. DEFAULT PRIVILEGES

SELECT
    defaclrole::regrole AS creator_role,
    defaclnamespace::regnamespace AS schema_name,
    defaclobjtype AS object_type,
    defaclacl AS default_acl
FROM pg_default_acl
WHERE defaclnamespace = (
    SELECT oid
    FROM pg_namespace
    WHERE nspname = 'shioricafe_schema'
);


-- 13. APPLICATION SCHEMA AND RUNTIME READINESS

SELECT
    EXISTS (
        SELECT 1
        FROM pg_roles
        WHERE rolname = 'shioricafe_app_login'
          AND rolcanlogin = TRUE
    ) AS runtime_login_exists,
    EXISTS (
        SELECT 1
        FROM pg_namespace
        WHERE nspname = 'shioricafe_schema'
    ) AS schema_exists,
    EXISTS (
        SELECT 1
        FROM pg_tables
        WHERE schemaname = 'shioricafe_schema'
    ) AS application_tables_exist,
    EXISTS (
        SELECT 1
        FROM pg_auth_members AS m
        JOIN pg_roles AS member
            ON member.oid = m.member
        JOIN pg_roles AS granted
            ON granted.oid = m.roleid
        WHERE member.rolname = 'shioricafe_app_login'
          AND granted.rolname = 'app_runtime_grp'
    ) AS runtime_group_membership_exists;


-- =========================================================
-- 14. RUNTIME OPERATION TESTS
--
-- NOT EXECUTED:
-- No SHIORI runtime login account or application tables
-- were found during the current database inspection.
--
-- Actual allowed and restricted operations must be tested
-- after the approved runtime account and finalized schema
-- are available.
-- =========================================================


-- END 
-- =========================================================
