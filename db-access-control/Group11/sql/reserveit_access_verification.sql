
-- =========================================================
-- ReserveIT - Access Control Verification
-- COMP 018 - Activity 4
-- Database: ReserveIT
-- Purpose: Verify roles, memberships, and table ownership
-- =========================================================

-- 1. Check login and group roles
SELECT
    rolname,
    rolcanlogin,
    rolsuper,
    rolcreatedb,
    rolcreaterole
FROM pg_roles
WHERE rolname IN (
    'app_runtime_grp',
    'app_readonly_grp',
    'app_admin_grp',
    'reserveit_owner',
    'reserveit_app',
    'reserveit_reporter',
    'reserveit_dba'
)
ORDER BY rolname;

-- 2. Check login-to-group-role memberships
SELECT
    member.rolname AS login_role,
    granted.rolname AS group_role
FROM pg_auth_members m
JOIN pg_roles granted ON granted.oid = m.roleid
JOIN pg_roles member ON member.oid = m.member
WHERE member.rolname IN (
    'reserveit_app',
    'reserveit_reporter',
    'reserveit_dba'
)
ORDER BY login_role;

-- 3. Check public schema privileges
SELECT
    'app_runtime_grp' AS role_name,
    has_schema_privilege('app_runtime_grp', 'public', 'USAGE') AS can_use_schema
UNION ALL
SELECT
    'app_readonly_grp',
    has_schema_privilege('app_readonly_grp', 'public', 'USAGE')
UNION ALL
SELECT
    'app_admin_grp',
    has_schema_privilege('app_admin_grp', 'public', 'USAGE');

-- 4. Check table ownership
SELECT
    schemaname,
    tablename,
    tableowner
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;

-- 5. Check database connection privileges
SELECT
    datname,
    has_database_privilege(
        'reserveit_app', datname, 'CONNECT'
    ) AS app_can_connect,
    has_database_privilege(
        'reserveit_reporter', datname, 'CONNECT'
    ) AS reporter_can_connect,
    has_database_privilege(
        'reserveit_dba', datname, 'CONNECT'
    ) AS dba_can_connect
FROM pg_database
WHERE datname = 'ReserveIT';
