
-- =========================================================
-- UniSync Database Roles and Privileges
-- COMP 018 - Activity 4
-- Database: grp10_unisyncdb
-- =========================================================


-- =========================================================
-- 1. CREATE GROUP ROLES
-- =========================================================

CREATE ROLE unisync_admin_grp NOLOGIN;
CREATE ROLE unisync_runtime_grp NOLOGIN;
CREATE ROLE unisync_readonly_grp NOLOGIN;


-- =========================================================
-- 2. CREATE LOGIN ROLES
-- =========================================================

CREATE ROLE unisync_app LOGIN;
CREATE ROLE unisync_dba LOGIN;
CREATE ROLE unisync_auditor LOGIN;


-- =========================================================
-- 3. ROLE MEMBERSHIPS
-- =========================================================

GRANT unisync_admin_grp TO unisync_dba;
GRANT unisync_runtime_grp TO unisync_app;
GRANT unisync_readonly_grp TO unisync_auditor;


-- =========================================================
-- 4. DATABASE AND SCHEMA PRIVILEGES
-- =========================================================

GRANT CONNECT ON DATABASE grp10_unisyncdb
TO unisync_admin_grp,
   unisync_runtime_grp,
   unisync_readonly_grp;

REVOKE CREATE ON SCHEMA public FROM PUBLIC;

GRANT USAGE ON SCHEMA public
TO unisync_admin_grp,
   unisync_runtime_grp,
   unisync_readonly_grp;

GRANT CREATE ON SCHEMA public
TO unisync_admin_grp;


-- =========================================================
-- 5. ADMIN GROUP PRIVILEGES
-- =========================================================

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public
TO unisync_admin_grp;

GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public
TO unisync_admin_grp;


-- =========================================================
-- 6. RUNTIME GROUP PRIVILEGES
-- =========================================================

GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public
TO unisync_runtime_grp;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public
TO unisync_runtime_grp;


-- =========================================================
-- 7. READ-ONLY GROUP PRIVILEGES
-- =========================================================

-- Approved tables
GRANT SELECT ON
    organization,
    membership,
    event,
    venue,
    event_schedule,
    registration,
    notification
TO unisync_readonly_grp;

-- Approved account columns only.
-- Sensitive password_hash column is excluded.
GRANT SELECT (
    account_id,
    account_name,
    email,
    avatar_url,
    account_status
)
ON account
TO unisync_readonly_grp;


-- =========================================================
-- 8. DEFAULT PRIVILEGES FOR FUTURE OBJECTS
-- Creator role: postgres
-- Schema: public
-- =========================================================

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
GRANT ALL PRIVILEGES ON TABLES TO unisync_admin_grp;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
GRANT ALL PRIVILEGES ON SEQUENCES TO unisync_admin_grp;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
GRANT SELECT, INSERT, UPDATE ON TABLES TO unisync_runtime_grp;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
GRANT USAGE, SELECT ON SEQUENCES TO unisync_runtime_grp;


-- =========================================================
-- 9. VERIFICATION QUERIES
-- Run these separately when checking the setup.
-- =========================================================

-- 9.1 Check role login and administrative attributes
SELECT
    rolname,
    rolcanlogin,
    rolsuper,
    rolcreatedb,
    rolcreaterole
FROM pg_roles
WHERE rolname IN (
    'unisync_admin_grp',
    'unisync_runtime_grp',
    'unisync_readonly_grp',
    'unisync_app',
    'unisync_dba',
    'unisync_auditor'
)
ORDER BY rolname;


-- 9.2 Check role memberships
SELECT
    member.rolname AS login_role,
    granted.rolname AS group_role
FROM pg_auth_members AS m
JOIN pg_roles AS granted ON granted.oid = m.roleid
JOIN pg_roles AS member ON member.oid = m.member
WHERE granted.rolname IN (
    'unisync_admin_grp',
    'unisync_runtime_grp',
    'unisync_readonly_grp'
)
ORDER BY login_role;


-- 9.3 Check database and schema privileges
SELECT
    rolname,
    has_database_privilege(
        rolname,
        'grp10_unisyncdb',
        'CONNECT'
    ) AS can_connect,
    has_schema_privilege(
        rolname,
        'public',
        'USAGE'
    ) AS can_use_schema,
    has_schema_privilege(
        rolname,
        'public',
        'CREATE'
    ) AS can_create_in_schema
FROM pg_roles
WHERE rolname IN (
    'unisync_admin_grp',
    'unisync_runtime_grp',
    'unisync_readonly_grp'
)
ORDER BY rolname;


-- 9.4 Check table privileges
SELECT
    grantee,
    table_name,
    privilege_type
FROM information_schema.role_table_grants
WHERE grantee IN (
    'unisync_admin_grp',
    'unisync_runtime_grp',
    'unisync_readonly_grp'
)
ORDER BY grantee, table_name, privilege_type;


-- 9.5 Check approved account column privileges
SELECT
    grantee,
    table_name,
    column_name,
    privilege_type
FROM information_schema.column_privileges
WHERE grantee = 'unisync_readonly_grp'
  AND table_name = 'account'
ORDER BY column_name;


-- 9.6 Check sequence privileges
SELECT
    sequence_name,
    has_sequence_privilege(
        'unisync_admin_grp',
        sequence_name,
        'USAGE'
    ) AS admin_usage,
    has_sequence_privilege(
        'unisync_runtime_grp',
        sequence_name,
        'USAGE'
    ) AS runtime_usage,
    has_sequence_privilege(
        'unisync_readonly_grp',
        sequence_name,
        'USAGE'
    ) AS auditor_usage
FROM information_schema.sequences
WHERE sequence_schema = 'public'
ORDER BY sequence_name;
