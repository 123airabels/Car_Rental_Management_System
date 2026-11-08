
-- ============================================================
-- Group07 - Applicant Management System
-- Database: grp07_applicantdb
-- Activity 4: Database Roles and Privileges
-- Execute as PostgreSQL superuser (postgres).
-- The six Group07 roles must already exist.
-- ============================================================

-- 1. DATABASE ACCESS
REVOKE CONNECT ON DATABASE grp07_applicantdb FROM PUBLIC;

GRANT CONNECT ON DATABASE grp07_applicantdb
TO grp07_app_admin_grp,
   grp07_app_runtime_grp,
   grp07_app_readonly_grp;

-- 2. SCHEMA ACCESS
GRANT USAGE ON SCHEMA public
TO grp07_app_admin_grp,
   grp07_app_runtime_grp,
   grp07_app_readonly_grp;

-- 3. ROLE MEMBERSHIPS
GRANT grp07_app_admin_grp TO grp07_ams_admin;
GRANT grp07_app_runtime_grp TO grp07_ams_app_user;
GRANT grp07_app_readonly_grp TO grp07_ams_viewer;

-- 4. RESET EXISTING GROUP TABLE PRIVILEGES
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public
FROM grp07_app_admin_grp,
     grp07_app_runtime_grp,
     grp07_app_readonly_grp;

-- Also reset the runtime column-level permission before
-- granting the specific password_hash update permission.
REVOKE UPDATE (password_hash) ON TABLE public.users
FROM grp07_app_runtime_grp;

-- 5. ADMINISTRATOR: FULL CRUD ON ALL TABLES
GRANT SELECT, INSERT, UPDATE, DELETE
ON TABLE
    public.applicant,
    public.applicant_requirement,
    public.application,
    public.application_status,
    public.department,
    public.interview,
    public.position,
    public.requirement,
    public.users
TO grp07_app_admin_grp;

-- 6. APPLICATION RUNTIME: CRUD ON OPERATIONAL TABLES
GRANT SELECT, INSERT, UPDATE, DELETE
ON TABLE
    public.applicant,
    public.applicant_requirement,
    public.application,
    public.department,
    public.interview,
    public.position,
    public.requirement
TO grp07_app_runtime_grp;

-- Runtime can only read application status.
GRANT SELECT ON TABLE public.application_status
TO grp07_app_runtime_grp;

-- Runtime can read user records for authentication.
GRANT SELECT ON TABLE public.users
TO grp07_app_runtime_grp;

-- Runtime can update only password_hash for password rehashing.
GRANT UPDATE (password_hash) ON TABLE public.users
TO grp07_app_runtime_grp;

-- 7. READ-ONLY VIEWER: SELECT ON APPROVED TABLES
GRANT SELECT
ON TABLE
    public.applicant,
    public.applicant_requirement,
    public.application,
    public.application_status,
    public.department,
    public.interview,
    public.position,
    public.requirement
TO grp07_app_readonly_grp;

-- Viewer has no access to users.
REVOKE ALL PRIVILEGES ON TABLE public.users
FROM grp07_app_readonly_grp;

-- 8. RESET AND GRANT SEQUENCE PRIVILEGES
REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public
FROM grp07_app_admin_grp,
     grp07_app_runtime_grp,
     grp07_app_readonly_grp;

GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public
TO grp07_app_admin_grp;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public
TO grp07_app_runtime_grp;

-- 9. DEFAULT PRIVILEGES FOR FUTURE TABLES
ALTER DEFAULT PRIVILEGES FOR ROLE postgres
IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE
ON TABLES TO grp07_app_admin_grp;

-- 10. DEFAULT PRIVILEGES FOR FUTURE SEQUENCES
ALTER DEFAULT PRIVILEGES FOR ROLE postgres
IN SCHEMA public
GRANT ALL PRIVILEGES
ON SEQUENCES TO grp07_app_admin_grp;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres
IN SCHEMA public
GRANT USAGE, SELECT
ON SEQUENCES TO grp07_app_runtime_grp;
