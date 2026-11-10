# Batumbakal Gym Access Control Plan

**Activity:** COMP 018 – Database Administration, Activity 4  
**Group:** Group 09  
**Project:** Batumbakal Gym  
**Database:** `grp99_gymdb`  
**Schema:** `public`  
**Database Platform:** PostgreSQL 18.6

## 1. Purpose

This access control plan documents the existing database accounts, roles, and privileges of the Batumbakal Gym application.

It identifies the permissions assigned to the existing accounts and describes the access restrictions verified during database inspection and testing. The plan also supports the principle of least privilege by identifying areas where database access can be improved.

## 2. Database Accounts and Roles

| **Account / Role** | **Login Attribute** | **Observed Responsibility / Usage** | **Observed Privileges** |
|---|---|---|---|
| `postgres` | LOGIN | Database owner and application database account. | Superuser privileges, including CONNECT, CREATE, and TEMPORARY on `grp99_gymdb`. |
| `activity4_test_role` | NOLOGIN | Used for restricted access-control testing. | CONNECT on the database and USAGE on the `public` schema. No application table or sequence privileges. |

The roles `gym_app` and `gym_readonly` were also checked during inspection but were not found in the database.

## 3. Existing Privilege Configuration

### 3.1 Database Privileges

The `postgres` account has the following privileges on `grp99_gymdb`:

- CONNECT
- CREATE
- TEMPORARY

The `activity4_test_role` has CONNECT permission but does not have CREATE or TEMPORARY permission.

### 3.2 Schema Privileges

The `activity4_test_role` has USAGE permission on the `public` schema but does not have CREATE permission.

### 3.3 Table Privileges

The `postgres` account has SELECT, INSERT, UPDATE, and DELETE privileges on all eight application tables.

The `activity4_test_role` does not have these privileges on the application tables.

### 3.4 Sequence Privileges

The `activity4_test_role` does not have USAGE or SELECT privileges on the seven database sequences.

## 4. Role Restrictions and Security Findings

### 4.1 Database Administrator and Application Account — `postgres`

- Has the LOGIN attribute.
- Is a PostgreSQL superuser.
- Owns the `grp99_gymdb` database.
- Has CONNECT, CREATE, and TEMPORARY privileges on the database.
- Has SELECT, INSERT, UPDATE, and DELETE privileges on all eight application tables.
- Is currently used by the application to connect to the database.

### 4.2 Restricted Testing Role — `activity4_test_role`

- Has the NOLOGIN attribute.
- Has CONNECT permission on `grp99_gymdb`.
- Has USAGE but not CREATE permission on the `public` schema.
- Does not have SELECT, INSERT, UPDATE, or DELETE privileges on the eight application tables.
- Does not have USAGE or SELECT privileges on the seven sequences.
- Was used to verify restricted database access.

## 5. Access-Control Verification

The following operations were tested using the existing `activity4_test_role`:

| **Account / Role** | **Operation** | **Expected Result** | **Actual Result** |
|---|---|---|---|
| `activity4_test_role` | Connect to database | Allowed | Successful |
| `activity4_test_role` | SELECT from `members` | Denied | Permission denied |
| `activity4_test_role` | INSERT into `members` | Denied | Permission denied |
| `activity4_test_role` | UPDATE `members` | Denied | Permission denied |
| `activity4_test_role` | DELETE from `members` | Denied | Permission denied |

The UPDATE and DELETE tests used `WHERE 1=0` to avoid changing existing records. The INSERT test used `DEFAULT VALUES` and was denied before inserting a record.

## 6. Implementation and Security Notes

- The database uses the `public` schema.
- The database owner and application database account are both `postgres`.
- The application currently connects using a PostgreSQL superuser account.
- The `activity4_test_role` has restricted access to application tables and sequences.
- The roles `gym_app` and `gym_readonly` were not found during inspection.
- No role memberships involving the inspected application and test roles were found.
- No customized default-privilege entries were found for the global scope and `public` schema.
- A dedicated application database account with limited privileges would reduce unnecessary administrative access.

---

**Related Implementation Files**

- SQL Script: `roles-and-privileges.sql`
- Progress Documentation: `Progress Documentation.pdf`
- Evidence Folder: `Evidence/`
