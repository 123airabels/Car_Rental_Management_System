# ReserveIT – Access Control Plan

## 1. Project Overview

**Project Name:** ReserveIT – Classroom and Material Reservation System

**Database:** ReserveIT

**DBMS:** PostgreSQL 18

**Application:** Flask (Python)

ReserveIT allows students and faculty to request classrooms and shared materials. Staff review and approve requests, while administrators manage system records.

## 2. Database Accounts and Group Roles

| Login Account | Group Role | Application Role Served |
|---|---|---|
| `reserveit_app` | `app_runtime_grp` | Requester, Staff, Administrator |
| `reserveit_reporter` | `app_readonly_grp` | Report Viewer |
| `reserveit_dba` | `app_admin_grp` | Database Maintainer |
| `reserveit_owner` | No group role | Schema Owner |
| `postgres` | PostgreSQL superuser | Setup only |

The group roles are `NOLOGIN` roles. They hold privileges, while individual login accounts receive access through role membership.

## 3. Access Control Plan

### 3.1 Application Runtime – `app_runtime_grp`

**Account:** `reserveit_app`

**Responsibilities:**
- Handle application database operations.
- Allow requesters to register and submit reservations.
- Allow staff to review and approve reservations.
- Allow authorized users to manage permitted room, material, and penalty records.

**Required Privileges:**
- Connect to the ReserveIT database.
- Use the public schema.
- Read required application records.
- Insert permitted users, reservations, and related records.
- Update only designated columns, such as reservation status and penalty payment information.
- Access required ID sequences.

**Restrictions:**
- Cannot delete protected reservation, user, staff, department, or penalty records.
- Cannot change restricted reservation details or penalty amounts.
- Cannot truncate tables.
- Cannot create or drop tables.
- Cannot create roles or databases.

### 3.2 Read-Only Reporting – `app_readonly_grp`

**Account:** `reserveit_reporter`

**Responsibilities:**
- View reservation and penalty records.
- Access information needed for reports.
- Review permitted user and staff details.

**Required Privileges:**
- Connect to the ReserveIT database.
- Use the public schema.
- Perform SELECT operations on permitted tables and columns.

**Restrictions:**
- Cannot insert, update, or delete records.
- Cannot access restricted email addresses and password hashes.
- Cannot create or modify database objects.

### 3.3 Database Administration – `app_admin_grp`

**Account:** `reserveit_dba`

**Responsibilities:**
- Maintain database records.
- Correct inaccurate or invalid data.
- Perform authorized record management.

**Required Privileges:**
- Connect to the ReserveIT database.
- Use the public schema.
- Perform SELECT, INSERT, UPDATE, and DELETE operations on application tables.
- Access and manage database sequences.

**Restrictions:**
- Cannot change table structures.
- Cannot create or drop tables.
- Cannot create roles or databases.
- Does not have superuser privileges.

### 3.4 Schema Ownership

**Account:** `reserveit_owner`

**Responsibilities:**
- Own the database objects.
- Manage database schema creation and modifications.
- Support database initialization and maintenance.

This account is used for setup and schema-related operations, not regular application access.

### 3.5 PostgreSQL Superuser

**Account:** `postgres`

**Responsibilities:**
- Perform initial database role and account setup.
- Execute privileged setup operations when necessary.

This account is not used by the application during normal operation.

## 4. Principle of Least Privilege

The access control plan follows the principle of least privilege. Each account receives only the permissions needed for its assigned responsibilities.

The application runtime and reporting accounts have different levels of access. The database administrator can manage records but cannot modify the database structure or create privileged accounts.

## 5. Verification and Testing

The project's provided access control tests were executed to verify the assigned permissions.

| Test | Result |
|---|---|
| Application account access tests | Passed |
| Reporting account access tests | Passed |
| Database administrator access tests | Passed |
| Schema owner access tests | Passed |
| Account and role restrictions | Passed |
| Total access control checks | **48/48 Passed** |

The application's smoke tests also completed successfully with **67 checks passed**.

## 6. Additional Database Verification

The SQL verification script was executed successfully against the ReserveIT database.

The following results were confirmed:

- Three group roles were configured as NOLOGIN roles.
- Four individual login accounts were configured without superuser privileges.
- Three login accounts were assigned to their corresponding group roles.
- All three group roles have USAGE privileges on the public schema.
- All eight application tables are owned by `reserveit_owner`.
- The application, reporting, and DBA accounts can connect to the ReserveIT database.

**Verification Status:** Successful – all five SQL verification queries completed without errors.

**Verification Script:** `sql/reserveit_access_verification.sql`

## 7. Conclusion

The ReserveIT database access control implementation successfully separates application operations, reporting access, database administration, and schema ownership. The tests confirmed that permitted operations work and restricted operations are denied as expected.