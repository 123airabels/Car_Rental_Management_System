# Group07 Access Control Plan

**Activity:** COMP 018 – Database Administration, Activity 4  
**Project:** Applicant Management System (AMS)  
**Database:** `grp07_applicantdb`  
**Schema:** `public`  
**Database Platform:** PostgreSQL 18.6

## 1. Purpose

This access control plan defines the database accounts required by the Applicant Management System, the group roles assigned to those accounts, their responsibilities, and the privileges needed to perform their tasks.

The plan follows the principle of least privilege. Each account receives only the permissions required for its intended responsibility. The planned roles and privileges serve as the basis for PostgreSQL role configuration and access-control testing documented in the Activity 4 Progress Documentation.

## 2. Access Control Plan

| Login account | Group role | Application role served | Responsibility | Required privileges |
|---|---|---|---|---|
| `grp07_ams_admin` | `grp07_app_admin_grp` | Administrator | Manage applicant records, applications, interviews, requirements, system statuses, user accounts, and other administrative database operations. | `CONNECT` on `grp07_applicantdb`; `USAGE` on schema `public`; `SELECT`, `INSERT`, `UPDATE`, and `DELETE` on all existing tables; all privileges on existing sequences. |
| `grp07_ams_app_user` | `grp07_app_runtime_grp` | Staff / Application Runtime | Allow the PHP application and authorized Staff users to perform normal applicant and application management operations, including authentication. | `CONNECT` on `grp07_applicantdb`; `USAGE` on schema `public`; `SELECT`, `INSERT`, `UPDATE`, and `DELETE` on operational tables; `SELECT` on `application_status` and `users`; column-level `UPDATE` on `users.password_hash`; `USAGE` and `SELECT` on required sequences. |
| `grp07_ams_viewer` | `grp07_app_readonly_grp` | Viewer | View approved applicant and application records without modifying stored data or accessing user authentication information. | `CONNECT` on `grp07_applicantdb`; `USAGE` on schema `public`; `SELECT` on approved tables, excluding `users`. No write or sequence privileges. |

## 3. Role Restrictions and Security Decisions

### 3.1 Administrator — `grp07_ams_admin`

- Uses the `grp07_app_admin_grp` group role.
- Receives `SELECT`, `INSERT`, `UPDATE`, and `DELETE` privileges on all 9 existing tables.
- Receives all privileges on the 8 existing sequences.
- Has `CONNECT` permission on `grp07_applicantdb`.
- Has `USAGE` permission on schema `public`.
- Does not receive `SUPERUSER`, `CREATEDB`, or `CREATEROLE` attributes.
- Does not receive `CREATE` permission on schema `public`.
- Existing table ownership remains with `postgres`; granting table privileges does not transfer ownership.

### 3.2 Staff / Application Runtime — `grp07_ams_app_user`

- Uses the `grp07_app_runtime_grp` group role.
- Can perform `SELECT`, `INSERT`, `UPDATE`, and `DELETE` operations on the following operational tables:
  - `applicant`
  - `applicant_requirement`
  - `application`
  - `department`
  - `interview`
  - `position`
  - `requirement`
- Can only perform `SELECT` on `application_status`.
- Can perform `SELECT` on `users` for authentication.
- Can only update the `password_hash` column in `users`, supporting password rehashing when required by the application.
- Cannot update other user-account columns, such as `role`.
- Cannot insert, update, or delete records in `application_status` or perform general updates on `users`.
- Receives `USAGE` and `SELECT` privileges on the existing sequences.
- Does not have `CREATE` permission on schema `public`.
- Does not receive database-creation or PostgreSQL role-management privileges.

### 3.3 Viewer — `grp07_ams_viewer`

- Uses the `grp07_app_readonly_grp` group role.
- Can read approved records from the following tables:
  - `applicant`
  - `applicant_requirement`
  - `application`
  - `application_status`
  - `department`
  - `interview`
  - `position`
  - `requirement`
- Cannot access the `users` table.
- Cannot insert, update, or delete records.
- Does not receive sequence usage privileges.
- Does not have `CREATE` permission on schema `public`.

## 4. Group Role and Login Role Design

Group roles are created with the `NOLOGIN` attribute so they serve as privilege containers rather than accounts used for direct authentication. Individual login roles are created with the `LOGIN` attribute and assigned to their corresponding group roles.

| Group role | Login role | Login capability |
|---|---|---|
| `grp07_app_admin_grp` | `grp07_ams_admin` | `LOGIN` |
| `grp07_app_runtime_grp` | `grp07_ams_app_user` | `LOGIN` |
| `grp07_app_readonly_grp` | `grp07_ams_viewer` | `LOGIN` |

The PHP application uses `grp07_ams_app_user` for its database connection. Application-level authorization must also enforce the Administrator, Staff, and Viewer restrictions.

Passwords are configured locally and must not be included in this plan, SQL scripts, screenshots, or the repository.

## 5. Planned Access-Control Verification

The following tests are used to verify that the configured privileges match the plan.

| Account | Operation | Expected result |
|---|---|---|
| `grp07_ams_admin` | Connect to the database and read applicant records | Allowed |
| `grp07_ams_admin` | Read user records | Allowed |
| `grp07_ams_admin` | Insert, update, and delete authorized records | Allowed |
| `grp07_ams_admin` | Create objects in schema `public` | Denied |
| `grp07_ams_app_user` | Read applicant and application records | Allowed |
| `grp07_ams_app_user` | Insert, update, and delete operational records | Allowed |
| `grp07_ams_app_user` | Read `application_status` and `users` | Allowed |
| `grp07_ams_app_user` | Update `users.password_hash` | Allowed |
| `grp07_ams_app_user` | Update other columns in `users`, such as `role` | Denied |
| `grp07_ams_app_user` | Insert, update, or delete records in `application_status` | Denied |
| `grp07_ams_viewer` | Read approved tables | Allowed |
| `grp07_ams_viewer` | Insert, update, or delete records | Denied |
| `grp07_ams_viewer` | Read the `users` table | Denied |

Actual test results and supporting screenshots are recorded separately in the Activity 4 Progress Documentation.

## 6. Implementation Notes

- Database-level privileges are configured for `grp07_applicantdb`.
- Schema and object-level privileges are configured while connected to the target database.
- `CONNECT` permission alone does not grant access to tables.
- `USAGE` on the `public` schema does not automatically grant table or sequence privileges.
- Sequence privileges are assigned to the Administrator and Runtime groups to support operations that generate identifiers.
- Runtime access to `users` is restricted to the permissions needed for authentication and password rehashing.
- Viewer access excludes `users` to prevent exposure of authentication information.
- Default privileges are configured for future tables and sequences created by the `postgres` role in schema `public`.
- Future tables receive Administrator CRUD privileges. Future sequences receive all privileges for the Administrator and `USAGE`, `SELECT` for Runtime.
- Default privileges do not automatically apply to objects created by other roles.
- The plan describes the intended access structure; the SQL script and verification evidence document the actual implementation and test results.

---

**Related implementation files**

- SQL script: `grp07_roles_and_privileges.sql`
- Progress documentation: `Group07_Progress_Documentation.md`
- Evidence folder: `evidence/`