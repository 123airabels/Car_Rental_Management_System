# Group08 Catering Business Management System – Access Control Plan

**Activity:** COMP018 – Database Administration Activity 4  
**Project:** Catering Business Management System  
**Database:** `grp08_catering_db`  
**Schema:** `public`  
**Database Platform:** PostgreSQL 18.6

## 1. Purpose

This Access Control Plan defines the database roles, permissions, and restrictions for the Group08 Catering Business Management System.

The plan applies Role-Based Access Control (RBAC) and the principle of least privilege. Each database account receives only the permissions needed for its assigned responsibilities to protect sensitive information and prevent unauthorized database operations.

## 2. Access Control Plan

| Login Account | Group Role | Application Role Served | Responsibility | Required Privileges |
|---|---|---|---|---|
| `grp08_catering_admin` | `grp08_catering_admin_grp` | Administrator | Manages and maintains catering business records and system data. | SELECT, INSERT, UPDATE, DELETE on all tables; ALL privileges on all sequences. |
| `grp08_catering_staff` | `grp08_catering_staff_grp` | Staff | Handles reservations, customers, employees, inventory, packages, menu items, payments, and cancellations. | SELECT, INSERT, UPDATE on assigned operational tables; limited customer column access; USAGE and SELECT on assigned sequences. |
| `grp08_catering_customer` | `grp08_catering_customer_grp` | Customer | Views available packages and menu items and manages permitted customer and reservation records. | Limited customer profile access; SELECT on packages and menu items; SELECT and INSERT on assigned transaction tables; USAGE and SELECT on assigned sequences. |
| `grp08_catering_auditor` | `grp08_catering_readonly_grp` | Auditor | Reviews selected business information without modifying records. | SELECT on inventory, menu_item, and package tables only. |

## 3. Role Restrictions and Security Decisions

### 3.1 Administrator Role

**Login Account:** `grp08_catering_admin`  
**Group Role:** `grp08_catering_admin_grp`

The administrator has broad access to manage database records.

**Allowed privileges:**
- SELECT, INSERT, UPDATE, and DELETE on all tables.
- ALL privileges on all nine existing sequences.

**Restrictions:**
- Does not have SUPERUSER privileges.
- Does not have CREATEDB or CREATEROLE privileges.
- Does not have CREATE privilege on the public schema.

### 3.2 Staff Role

**Login Account:** `grp08_catering_staff`  
**Group Role:** `grp08_catering_staff_grp`

The staff role is intended for daily catering business operations.

**Allowed privileges:**
- SELECT, INSERT, and UPDATE on:
  - cancellation
  - employee
  - event_assignment
  - inventory
  - menu_item
  - package
  - payment
  - reservation
  - reservation_menu
- Limited SELECT, INSERT, and UPDATE privileges on permitted customer columns.
- USAGE and SELECT on eight assigned sequences.

**Restrictions:**
- Cannot DELETE records.
- Cannot access the admin table.
- Cannot access the customer password_hash column.

### 3.3 Customer Role

**Login Account:** `grp08_catering_customer`  
**Group Role:** `grp08_catering_customer_grp`

The customer role is limited to customer-related activities.

**Allowed privileges:**
- SELECT on permitted customer profile columns.
- INSERT on customer columns, including password_hash.
- UPDATE only on first_name, last_name, phone, and address.
- SELECT on package and menu_item.
- SELECT and INSERT on reservation, reservation_menu, payment, and cancellation.
- USAGE and SELECT on four assigned sequences.

**Restrictions:**
- Cannot SELECT the password_hash column.
- Cannot UPDATE email.
- Cannot DELETE records.
- Cannot access the admin table.

### 3.4 Auditor Role

**Login Account:** `grp08_catering_auditor`  
**Group Role:** `grp08_catering_readonly_grp`

The auditor is assigned read-only access to selected business information.

**Allowed privileges:**
- SELECT on inventory.
- SELECT on menu_item.
- SELECT on package.

**Restrictions:**
- Cannot INSERT, UPDATE, or DELETE records.
- Cannot access customer, reservation, payment, cancellation, employee, or admin records.
- Does not receive sequence privileges.

## 4. Group Role and Login Role Design

The system separates group roles from login roles to make permission management easier.

**Group roles (NOLOGIN):**
- `grp08_catering_admin_grp`
- `grp08_catering_staff_grp`
- `grp08_catering_customer_grp`
- `grp08_catering_readonly_grp`

**Login roles (LOGIN):**
- `grp08_catering_admin`
- `grp08_catering_staff`
- `grp08_catering_customer`
- `grp08_catering_auditor`

Each login role is assigned to its corresponding group role. Permissions are granted to group roles instead of being individually assigned to every login account.

## 5. Planned Access-Control Verification

| Account | Operation | Expected Result |
|---|---|---|
| Administrator | SELECT records from business tables | Allowed |
| Administrator | INSERT, UPDATE, and DELETE business records | Allowed |
| Administrator | CREATE a table in public schema | Denied |
| Staff | View and update reservation records | Allowed |
| Staff | Insert or update permitted operational records | Allowed |
| Staff | DELETE reservation records | Denied |
| Staff | Access admin table | Denied |
| Staff | View customer password_hash | Denied |
| Customer | View package and menu_item records | Allowed |
| Customer | View permitted customer profile columns | Allowed |
| Customer | Insert a customer record | Allowed |
| Customer | Update permitted profile columns | Allowed |
| Customer | Update email | Denied |
| Customer | View password_hash | Denied |
| Customer | DELETE reservation records | Denied |
| Auditor | View inventory, menu_item, and package records | Allowed |
| Auditor | View reservation or customer records | Denied |
| Auditor | Insert or update business records | Denied |

## 6. Implementation Notes

1. The access control configuration is implemented using PostgreSQL roles and GRANT/REVOKE statements.
2. Public database connection access is revoked, and CONNECT is granted to the four designated group roles.
3. All four group roles receive USAGE on the public schema, while CREATE is restricted.
4. Existing database tables and sequences are owned by the PostgreSQL administrator account.
5. Passwords are configured separately and are not stored in the SQL script.
6. Access-control tests are performed using the designated login accounts.
7. The application currently uses a shared PostgreSQL connection pool. The four Activity 4 login roles have been tested directly at the database level, but separate application connections for each role have not been implemented.
8. The customer role cannot retrieve password_hash. Therefore, the application's existing authentication query must be considered when integrating this restricted role.
9. Default privileges for future tables and sequences are not configured in the current SQL script.

## Related Implementation Files

- `grp08_roles_and_privileges.sql` – PostgreSQL role creation and privilege configuration.
- `Group08_Progress_Documentation` – Activity 4 implementation and testing documentation.
- `evidence/` – screenshots of database configuration, privilege verification, access-control tests, and application connection.