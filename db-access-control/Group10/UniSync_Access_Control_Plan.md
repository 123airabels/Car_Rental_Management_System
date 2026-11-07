# UniSync Access Control Plan

**Activity:** COMP 018 – Database Administration, Activity 4  
**Project:** UniSync  
**Database:** `grp10_unisyncdb`  
**Schema:** `public`  
**Database Platform:** PostgreSQL 18.6

## 1. Purpose

This access control plan defines the database accounts required by the UniSync application, the group roles assigned to those accounts, their responsibilities, and the privileges needed to perform their tasks.

The plan follows the principle of least privilege. Each account receives only the permissions required for its intended responsibility. The planned roles and privileges were used as the basis for the PostgreSQL role configuration and access-control tests documented in the Activity 4 progress report.

## 2. Access Control Plan

| Login account | Group role | Application role served | Responsibility | Required privileges |
|---|---|---|---|---|
| `unisync_dba` | `unisync_admin_grp` | Database Administrator | Administer and maintain the UniSync database, manage database objects, and perform authorized maintenance tasks. | `CONNECT` on `grp10_unisyncdb`; `USAGE` and `CREATE` on schema `public`; all applicable privileges on existing tables and sequences. |
| `unisync_app` | `unisync_runtime_grp` | Application Runtime | Allow the UniSync application to perform the database operations needed for normal application workflows, such as account registration, login, and event booking. | `CONNECT` on `grp10_unisyncdb`; `USAGE` on schema `public`; `SELECT`, `INSERT`, and `UPDATE` on required tables; `USAGE` and `SELECT` on required sequences. |
| `unisync_auditor` | `unisync_readonly_grp` | Auditor / Read-only User | View approved database records for inspection and reporting without modifying stored data or accessing sensitive account information. | `CONNECT` on `grp10_unisyncdb`; `USAGE` on schema `public`; `SELECT` on the approved tables and only the approved columns of the `account` table. |

## 3. Role Restrictions and Security Decisions

### 3.1 Database Administrator — `unisync_dba`

- Uses the `unisync_admin_grp` group role.
- Receives administrative privileges needed for database maintenance.
- Has `CREATE` permission on the `public` schema.
- Does not receive `SUPERUSER`, `CREATEDB`, or `CREATEROLE` attributes as part of the documented configuration.
- Existing table ownership remains with `postgres`; granting table privileges does not transfer ownership.

### 3.2 Application Runtime — `unisync_app`

- Uses the `unisync_runtime_grp` group role.
- Can read, insert, and update records required by the tested application workflows.
- Cannot directly delete records.
- Does not have `CREATE` permission on the `public` schema.
- Does not receive database-creation or role-management privileges.
- Must not use a PostgreSQL superuser account for ordinary application access.

### 3.3 Auditor / Read-only User — `unisync_auditor`

- Uses the `unisync_readonly_grp` group role.
- Can read approved records from the following tables:
  - `organization`
  - `membership`
  - `event`
  - `venue`
  - `event_schedule`
  - `registration`
  - `notification`
- Access to the `account` table is limited to these columns:
  - `account_id`
  - `account_name`
  - `email`
  - `avatar_url`
  - `account_status`
- Cannot access `password_hash`.
- Cannot insert, update, or delete records.
- Does not have sequence usage or schema creation privileges.

## 4. Group Role and Login Role Design

Group roles are created with the `NOLOGIN` attribute so they serve as privilege containers rather than accounts used for direct authentication. Individual login roles are created with the `LOGIN` attribute and assigned to their corresponding group roles.

| Group role | Login role | Login capability |
|---|---|---|
| `unisync_admin_grp` | `unisync_dba` | `LOGIN` |
| `unisync_runtime_grp` | `unisync_app` | `LOGIN` |
| `unisync_readonly_grp` | `unisync_auditor` | `LOGIN` |

Passwords are configured locally and must not be included in this plan, SQL scripts, screenshots, or the repository.

## 5. Planned Access-Control Verification

The following tests are used to verify that the configured privileges match the plan.

| Account | Operation | Expected result |
|---|---|---|
| `unisync_app` | Read authorized application records | Allowed |
| `unisync_app` | Insert a required application record | Allowed |
| `unisync_app` | Update an authorized application record | Allowed |
| `unisync_app` | Delete a record | Denied |
| `unisync_app` | Create a database or PostgreSQL role | Denied |
| `unisync_auditor` | Read approved tables and account columns | Allowed |
| `unisync_auditor` | Read `account.password_hash` | Denied |
| `unisync_auditor` | Insert, update, or delete records | Denied |
| `unisync_dba` | Connect and perform authorized administrative operations | Allowed |
| `unisync_app` | Connect to an unrelated database without permission | Denied |

Actual test results and supporting screenshots are recorded separately in the Activity 4 Progress Documentation.

## 6. Implementation Notes

- Database-level privileges are configured for `grp10_unisyncdb`.
- Schema and object-level privileges are configured while connected to the target database.
- `CONNECT` permission alone does not grant access to tables.
- `USAGE` on the `public` schema does not automatically grant table or sequence privileges.
- Sequence privileges are included where required by application operations that generate identifiers.
- Default privileges are configured for future tables and sequences created by the `postgres` role in the `public` schema. These default privileges do not automatically apply to objects created by other roles.
- The plan describes the intended access structure; the SQL script and verification evidence document the actual implementation and test results.

---

**Related implementation files**

- SQL script: `unisync-roles-and-privileges.sql`
- Progress documentation: `PDF Progress Documentation.pdf`
- Evidence folder: `evidence/`
