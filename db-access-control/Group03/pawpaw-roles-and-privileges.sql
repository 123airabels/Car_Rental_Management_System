# Pawpaw Database Roles and Privileges
**Course:** COMP 018 – Database Administration  
**Activity:** Activity 4 – Database Users, Roles, Privileges, and Access Control  
**Project:** Pawpaw Clinic  
**Database:** `group3_pawpaw`  
**Schema:** `public`  
**Database Platform:** PostgreSQL 18.6

---

```sql
-- =========================================================
-- Pawpaw Database Roles and Privileges
-- COMP 018 - Activity 4
-- Database: group3_pawpaw
-- Schema: public
-- =========================================================


-- =========================================================
-- 1. CREATE GROUP ROLES
-- =========================================================

CREATE ROLE app_admin_grp NOLOGIN;
CREATE ROLE app_runtime_grp NOLOGIN;


-- =========================================================
-- 2. CREATE LOGIN ROLES
-- =========================================================

CREATE ROLE pawpaw_dba LOGIN;
CREATE ROLE pawpaw_app LOGIN;


-- =========================================================
-- 3. ROLE MEMBERSHIPS
-- =========================================================

GRANT app_admin_grp TO pawpaw_dba;
GRANT app_runtime_grp TO pawpaw_app;


-- =========================================================
-- 4. DATABASE AND SCHEMA PRIVILEGES
-- =========================================================

-- Restrict CONNECT privilege on the Pawpaw database.
REVOKE CONNECT ON DATABASE group3_pawpaw FROM PUBLIC;

-- Allow assigned group roles to connect.
GRANT CONNECT ON DATABASE group3_pawpaw
TO app_admin_grp,
   app_runtime_grp;


-- =========================================================
-- 5. ADMIN GROUP PRIVILEGES
-- Run while connected to group3_pawpaw.
-- =========================================================

-- Allow schema access and object creation.
GRANT USAGE, CREATE ON SCHEMA public
TO app_admin_grp;

-- Grant all privileges on existing tables.
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public
TO app_admin_grp;

-- Grant all privileges on existing sequences.
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public
TO app_admin_grp;


-- =========================================================
-- 6. RUNTIME GROUP PRIVILEGES
-- =========================================================

-- Allow access to the public schema without CREATE privilege.
GRANT USAGE ON SCHEMA public
TO app_runtime_grp;

-- Allow normal application CRUD operations.
GRANT SELECT, INSERT, UPDATE, DELETE
ON ALL TABLES IN SCHEMA public
TO app_runtime_grp;

-- Allow generated ID operations using sequences.
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public
TO app_runtime_grp;


-- =========================================================
-- 7. DEFAULT PRIVILEGES FOR FUTURE OBJECTS
-- Creator role: postgres
-- Schema: public
-- =========================================================

-- Future tables created by postgres.
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
GRANT ALL PRIVILEGES ON TABLES TO app_admin_grp;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE
ON TABLES TO app_runtime_grp;


-- Future sequences created by postgres.
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
GRANT ALL PRIVILEGES ON SEQUENCES TO app_admin_grp;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
GRANT USAGE, SELECT ON SEQUENCES TO app_runtime_grp;


-- =========================================================
-- 8. VERIFICATION QUERIES
-- Run these separately when checking the setup.
-- =========================================================


-- 8.1 Check role login and administrative attributes

SELECT
    rolname,
    rolcanlogin,
    rolsuper,
    rolcreatedb,
    rolcreaterole,
    rolreplication,
    rolbypassrls
FROM pg_roles
WHERE rolname IN (
    'app_admin_grp',
    'app_runtime_grp',
    'pawpaw_dba',
    'pawpaw_app'
)
ORDER BY rolname;


-- 8.2 Check role memberships

SELECT
    member_role.rolname AS login_role,
    parent_role.rolname AS group_role
FROM pg_auth_members AS m
JOIN pg_roles AS parent_role
    ON parent_role.oid = m.roleid
JOIN pg_roles AS member_role
    ON member_role.oid = m.member
WHERE member_role.rolname IN (
    'pawpaw_dba',
    'pawpaw_app'
)
ORDER BY member_role.rolname;


-- 8.3 Check database and schema privileges
-- Checks the effective privileges of the login accounts.

SELECT
    rolname,
    has_database_privilege(
        rolname,
        'group3_pawpaw',
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
    ) AS can_create_objects
FROM pg_roles
WHERE rolname IN ('pawpaw_dba', 'pawpaw_app')
ORDER BY rolname;


-- 8.4 Check existing table privileges by group role

SELECT
    grantee,
    table_name,
    privilege_type
FROM information_schema.role_table_grants
WHERE grantee IN (
    'app_admin_grp',
    'app_runtime_grp'
)
AND table_schema = 'public'
ORDER BY grantee, table_name, privilege_type;


-- 8.5 Check effective table privileges of login accounts

SELECT
    table_name,
    has_table_privilege(
        'pawpaw_app',
        format('%I.%I', table_schema, table_name),
        'SELECT'
    ) AS app_select,
    has_table_privilege(
        'pawpaw_app',
        format('%I.%I', table_schema, table_name),
        'INSERT'
    ) AS app_insert,
    has_table_privilege(
        'pawpaw_app',
        format('%I.%I', table_schema, table_name),
        'UPDATE'
    ) AS app_update,
    has_table_privilege(
        'pawpaw_app',
        format('%I.%I', table_schema, table_name),
        'DELETE'
    ) AS app_delete,
    (
        has_table_privilege(
            'pawpaw_dba',
            format('%I.%I', table_schema, table_name),
            'SELECT'
        )
        AND has_table_privilege(
            'pawpaw_dba',
            format('%I.%I', table_schema, table_name),
            'INSERT'
        )
        AND has_table_privilege(
            'pawpaw_dba',
            format('%I.%I', table_schema, table_name),
            'UPDATE'
        )
        AND has_table_privilege(
            'pawpaw_dba',
            format('%I.%I', table_schema, table_name),
            'DELETE'
        )
    ) AS dba_crud
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
ORDER BY table_name;


-- 8.6 Check sequence privileges by group role

SELECT
    sequence_schema,
    sequence_name,
    has_sequence_privilege(
        'app_admin_grp',
        format('%I.%I', sequence_schema, sequence_name),
        'USAGE'
    ) AS admin_usage,
    has_sequence_privilege(
        'app_runtime_grp',
        format('%I.%I', sequence_schema, sequence_name),
        'USAGE'
    ) AS runtime_usage
FROM information_schema.sequences
WHERE sequence_schema = 'public'
ORDER BY sequence_name;


-- 8.7 Check effective sequence privileges of login accounts

SELECT
    c.relname AS sequence_name,
    has_sequence_privilege(
        'pawpaw_app', c.oid, 'USAGE'
    ) AS app_usage,
    has_sequence_privilege(
        'pawpaw_app', c.oid, 'SELECT'
    ) AS app_select,
    has_sequence_privilege(
        'pawpaw_dba', c.oid, 'USAGE'
    ) AS dba_usage,
    has_sequence_privilege(
        'pawpaw_dba', c.oid, 'SELECT'
    ) AS dba_select,
    has_sequence_privilege(
        'pawpaw_dba', c.oid, 'UPDATE'
    ) AS dba_update
FROM pg_class AS c
JOIN pg_namespace AS n
    ON n.oid = c.relnamespace
WHERE c.relkind = 'S'
  AND n.nspname = 'public'
ORDER BY c.relname;


-- 8.8 Check PUBLIC database privileges

SELECT
    d.datname,
    a.privilege_type,
    a.is_grantable
FROM pg_database AS d
CROSS JOIN LATERAL aclexplode(
    COALESCE(d.datacl, acldefault('d', d.datdba))
) AS a
WHERE d.datname = 'group3_pawpaw'
  AND a.grantee = 0
ORDER BY a.privilege_type;


-- 8.9 Check total tables in the public schema

SELECT
    COUNT(*) AS total_tables
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE';


-- 8.10 Check total sequences in the public schema

SELECT
    COUNT(*) AS total_sequences
FROM pg_class AS c
JOIN pg_namespace AS n
    ON n.oid = c.relnamespace
WHERE c.relkind = 'S'
  AND n.nspname = 'public';


-- =========================================================
-- END OF PAWPAW DATABASE ROLES AND PRIVILEGES
-- =========================================================
```
