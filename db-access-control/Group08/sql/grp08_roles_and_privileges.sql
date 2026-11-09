-- ============================================================
-- GROUP 08: CATERING BUSINESS MANAGEMENT SYSTEM
-- Database: grp08_catering_db
-- Activity 4: Database Roles and Privileges
--
-- Run as PostgreSQL superuser after schema.sql and seed.sql.
-- No passwords are stored in this file.
-- ============================================================

-- 1. CREATE GROUP ROLES (NOLOGIN)

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'grp08_catering_admin_grp') THEN
        CREATE ROLE grp08_catering_admin_grp NOLOGIN;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'grp08_catering_staff_grp') THEN
        CREATE ROLE grp08_catering_staff_grp NOLOGIN;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'grp08_catering_customer_grp') THEN
        CREATE ROLE grp08_catering_customer_grp NOLOGIN;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'grp08_catering_readonly_grp') THEN
        CREATE ROLE grp08_catering_readonly_grp NOLOGIN;
    END IF;
END
$$;

-- 2. CREATE LOGIN ROLES

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'grp08_catering_admin') THEN
        CREATE ROLE grp08_catering_admin LOGIN;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'grp08_catering_staff') THEN
        CREATE ROLE grp08_catering_staff LOGIN;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'grp08_catering_customer') THEN
        CREATE ROLE grp08_catering_customer LOGIN;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'grp08_catering_auditor') THEN
        CREATE ROLE grp08_catering_auditor LOGIN;
    END IF;
END
$$;

-- 3. ASSIGN LOGIN ROLES TO GROUP ROLES

GRANT grp08_catering_admin_grp TO grp08_catering_admin;
GRANT grp08_catering_staff_grp TO grp08_catering_staff;
GRANT grp08_catering_customer_grp TO grp08_catering_customer;
GRANT grp08_catering_readonly_grp TO grp08_catering_auditor;

-- 4. DATABASE CONNECTION PRIVILEGES

REVOKE CONNECT ON DATABASE grp08_catering_db FROM PUBLIC;

GRANT CONNECT ON DATABASE grp08_catering_db TO
    grp08_catering_admin_grp,
    grp08_catering_staff_grp,
    grp08_catering_customer_grp,
    grp08_catering_readonly_grp;

-- 5. SCHEMA PRIVILEGES

REVOKE CREATE ON SCHEMA public FROM PUBLIC;

GRANT USAGE ON SCHEMA public TO
    grp08_catering_admin_grp,
    grp08_catering_staff_grp,
    grp08_catering_customer_grp,
    grp08_catering_readonly_grp;

-- 6. REMOVE EXISTING PUBLIC TABLE AND SEQUENCE ACCESS

REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM PUBLIC;
REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM PUBLIC;

-- 7. ADMINISTRATOR: FULL TABLE ACCESS

GRANT SELECT, INSERT, UPDATE, DELETE
ON ALL TABLES IN SCHEMA public
TO grp08_catering_admin_grp;

GRANT ALL PRIVILEGES
ON ALL SEQUENCES IN SCHEMA public
TO grp08_catering_admin_grp;

-- 8. STAFF: OPERATIONAL ACCESS
-- No direct access to the admin table.
-- No DELETE privileges.

GRANT SELECT, INSERT, UPDATE ON
    cancellation,
    employee,
    event_assignment,
    inventory,
    menu_item,
    package,
    payment,
    reservation,
    reservation_menu
TO grp08_catering_staff_grp;

-- Staff can view customer contact information,
-- but cannot read or modify password_hash.

GRANT SELECT (
    customer_id,
    first_name,
    last_name,
    email,
    phone,
    address
)
ON customer TO grp08_catering_staff_grp;

GRANT INSERT (
    first_name,
    last_name,
    email,
    phone,
    address
)
ON customer TO grp08_catering_staff_grp;

GRANT UPDATE (
    customer_id,
    first_name,
    last_name,
    email,
    phone,
    address
)
ON customer TO grp08_catering_staff_grp;

-- Sequences needed for staff-created records.

GRANT USAGE, SELECT ON SEQUENCE
    cancellation_cancellation_id_seq,
    customer_customer_id_seq,
    employee_employee_id_seq,
    inventory_inventory_id_seq,
    menu_item_menu_item_id_seq,
    package_package_id_seq,
    payment_payment_id_seq,
    reservation_reservation_id_seq
TO grp08_catering_staff_grp;

-- 9. CUSTOMER: PORTAL ACCESS

-- Customer profile fields (password_hash excluded from SELECT).

GRANT SELECT (
    customer_id,
    first_name,
    last_name,
    email,
    phone,
    address
)
ON customer TO grp08_catering_customer_grp;

-- Registration requires inserting the password hash.
GRANT INSERT (
    first_name,
    last_name,
    email,
    phone,
    address,
    password_hash
)
ON customer TO grp08_catering_customer_grp;

-- Profile updates are limited to these fields.
GRANT UPDATE (
    first_name,
    last_name,
    phone,
    address
)
ON customer TO grp08_catering_customer_grp;

-- Browse available packages and menu items.

GRANT SELECT ON package, menu_item
TO grp08_catering_customer_grp;

-- Reservation and related portal operations.

GRANT SELECT, INSERT ON reservation
TO grp08_catering_customer_grp;

GRANT SELECT, INSERT ON reservation_menu
TO grp08_catering_customer_grp;

GRANT SELECT, INSERT ON payment
TO grp08_catering_customer_grp;

GRANT SELECT, INSERT ON cancellation
TO grp08_catering_customer_grp;

-- Sequences required for customer-created records.

GRANT USAGE, SELECT ON SEQUENCE
    customer_customer_id_seq,
    reservation_reservation_id_seq,
    payment_payment_id_seq,
    cancellation_cancellation_id_seq
TO grp08_catering_customer_grp;

-- 10. AUDITOR: READ-ONLY ACCESS
-- Only approved, non-sensitive business information.

GRANT SELECT ON
    inventory,
    menu_item,
    package
TO grp08_catering_readonly_grp;

-- No INSERT, UPDATE, DELETE, or sequence privileges
-- are granted to the auditor.