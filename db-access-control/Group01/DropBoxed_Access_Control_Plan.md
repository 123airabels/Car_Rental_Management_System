# DropBoxed Access Control Plan

**Activity:** COMP 018 – Database Administration, Activity 4
**Project:** DropBoxed (Group 1) – environmental complaint application for household water quality monitoring in San Pedro
**Database:** `dropboxed_db`
**Schema:** `dropboxed`
**Database Platform:** PostgreSQL 18.6

## 1. Purpose

This access control plan defines the PostgreSQL login accounts required by the DropBoxed application, the group roles assigned to those accounts, their responsibilities, and the privileges needed to perform their tasks.

The plan follows the principle of least privilege. Each account receives only the permissions required for its intended responsibility. The planned roles and privileges are aligned with the implemented scripts (`database/03_roles.sql`, `database/04_privileges.sql`) and the access-control tests recorded in the Activity 4 Progress Documentation.

## 2. Access Control Plan

| **Login account** | **Group role** | **Application role served** | **Responsibility** | **Required privileges** |
|---|---|---|---|---|
| `dropboxed_app` | `dropboxed_runtime_grp` | Resident, Staff / Administrator, Visitor | Normal application operations executed by the Node.js/Express backend. | `CONNECT` on `dropboxed_db`; `USAGE` on schema `dropboxed`; `SELECT`, `INSERT`, and column-scoped `UPDATE`/`DELETE` on application tables; no sequence privileges. |
| `dropboxed_report` | `dropboxed_readonly_grp` | Reports viewer / auditor | Inspect records and produce reports without seeing credentials or contact details. | `CONNECT` on `dropboxed_db`; `USAGE` on schema `dropboxed`; `SELECT` on approved tables and columns. |
| `dropboxed_dba` | `dropboxed_dba_grp` | Database administrator | Maintenance, monitoring, and authorized data correction. | `CONNECT` on `dropboxed_db`; `USAGE` on schema `dropboxed`; `SELECT`, `INSERT`, `UPDATE`, `DELETE`, and `MAINTAIN` on tables; `SELECT` on sequences; membership in `pg_monitor`. |
| `dropboxed_backup` | `dropboxed_backup_grp` | Backup operator | Run `pg_dump` backups that can restore the system. | `CONNECT` on `dropboxed_db`; `USAGE` on schema `dropboxed`; `SELECT` on all tables and sequences. |

**Note:** DropBoxed also has application-level roles (Resident, Staff / Administrator, Visitor). These are managed by the application and are separate from the PostgreSQL login roles in this plan. All three reach the database only through `dropboxed_app`.

`postgres` owns the database, the schema, and all objects. It is used only for provisioning, DDL, and restore, and the running application never loads its password.

## 3. Role Restrictions and Security Decisions

### 3.1 Application Runtime — `dropboxed_app`

- Uses `dropboxed_runtime_grp` and has a connection limit of 20.
- Can read records and insert complaints, categories, locations, assignments, status logs, and attachment metadata.
- Column-scoped `UPDATE` only: no `UPDATE` on email columns or on `complaints.submitted_at`.
- Has `DELETE` only on `complaint_categories` and `complaint_assignments`. It has **no** `DELETE` on `complaints`, `users`, `staff`, `attachments`, `locations`, or `status_logs`.
- Has no `UPDATE` or `DELETE` on `status_logs`, so the status history stays append-only (Business Rule 7).
- Has `USAGE` but not `CREATE` on schema `dropboxed`.
- Has **no** privileges on the 8 sequences. Every primary key is `GENERATED ALWAYS AS IDENTITY`, so an `INSERT` advances the sequence without a sequence grant, and `nextval()` is denied.
- Has no `TRUNCATE`, `REFERENCES`, or `TRIGGER` privileges.
- Does not receive `SUPERUSER`, `CREATEDB`, `CREATEROLE`, `REPLICATION`, or `BYPASSRLS`.

### 3.2 Read-only Reporting — `dropboxed_report`

- Uses `dropboxed_readonly_grp`.
- Can read complaints, categories, locations, assignments, status logs, attachments, and calendar/contact-free columns of `users`.
- **Cannot** read `users.password_hash`, `users.email`, `users.phone_number`, or any column of `staff`.
- Is denied `INSERT`, `UPDATE`, `DELETE`, `ANALYZE`, and `CREATE DATABASE`.
- Has no sequence privileges and no `CREATE` on the schema.

### 3.3 Database Administrator — `dropboxed_dba`

- Uses `dropboxed_dba_grp`, which is a member of the built-in `pg_monitor` role, so it can read `pg_stat_activity` (including other sessions' query text) without superuser rights.
- Has `SELECT`, `INSERT`, `UPDATE`, `DELETE`, and `MAINTAIN` on all tables, so it can correct data and run `VACUUM`/`ANALYZE`.
- Has `SELECT` on all sequences.
- Has `MAINTAIN` instead of ownership: it cannot `DROP` or `ALTER` tables, and `TRUNCATE` is denied. `MAINTAIN` requires PostgreSQL 17 or newer.
- Does not receive `SUPERUSER`, `CREATEDB`, `CREATEROLE`, `REPLICATION`, or `BYPASSRLS`.

### 3.4 Backup Operator — `dropboxed_backup`

- Uses `dropboxed_backup_grp`.
- Has `SELECT` on all tables and sequences, so a `pg_dump` is complete.
- Can read `password_hash` because a dump that omits a column cannot restore the system; dump files must therefore be stored in a protected folder.
- Is denied `UPDATE`, `DELETE`, `CREATE ROLE`, and DDL.

## 4. Group Role and Login Role Design

Group roles are created with `NOLOGIN` so they act as privilege containers. Each login role has `LOGIN` and is a member of exactly one group role. Login roles hold no direct grants; all privileges are attached to the group roles.

| **Group role** | **Login role** | **Login capability** | **Direct grants** |
|---|---|---|---|
| `dropboxed_runtime_grp` | `dropboxed_app` | `LOGIN`, connection limit 20 | none |
| `dropboxed_readonly_grp` | `dropboxed_report` | `LOGIN` | none |
| `dropboxed_dba_grp` | `dropboxed_dba` | `LOGIN` | none |
| `dropboxed_backup_grp` | `dropboxed_backup` | `LOGIN` | none |

Role names carry the `dropboxed_` prefix because PostgreSQL roles are cluster-wide and other projects share the server. The `app_runtime_grp` / `app_admin_grp` style roles used by other projects are not reused.

Passwords are configured locally and must not be included in this plan, SQL scripts, screenshots, or the project repository. They live in `.env` and `.env.setup`, which are ignored by Git.

## 5. Database and Schema Privileges

- `CONNECT` and `TEMP` are revoked from `PUBLIC` on `dropboxed_db`.
- `CONNECT` is granted only to the four group roles, so a login with no group membership cannot connect. `TEMP` is granted to no group.
- `USAGE` on schema `dropboxed` is granted to all four group roles; **none** receives `CREATE`.
- Access from `PUBLIC` is revoked on both the `dropboxed` schema and the `public` schema. DropBoxed stores nothing in `public`.

## 6. Table and Column Privileges

S = `SELECT`, I = `INSERT`, U = `UPDATE`, D = `DELETE`, M = `MAINTAIN`. Columns in parentheses limit the privilege to those columns.

| Table | Runtime | Read-only | Backup | DBA |
|---|---|---|---|---|
| `users` | S, I, U (full_name, phone_number, password_hash, account_status) | S (user_id, full_name, account_status, created_at) | S | S, I, U, D, M |
| `staff` | S, I, U (password_hash, account_status) | S (staff_id, full_name, account_status, created_at) | S | S, I, U, D, M |
| `complaint_categories` | S, I, D, U (category_name, description) | S | S | S, I, U, D, M |
| `locations` | S, I | S | S | S, I, U, D, M |
| `complaints` | S, I, U (status, resolved_at, is_flagged, flag_reason) | S | S | S, I, U, D, M |
| `complaint_assignments` | S, I, D | S | S | S, I, U, D, M |
| `status_logs` | S, I | S | S | S, I, U, D, M |
| `attachments` | S, I | S | S | S, I, U, D, M |

Deliberate omissions for the runtime account: `DELETE` on `complaints`, `users`, and `staff` (complaints are never deleted — Business Rule 11; accounts are deactivated instead — Business Rule 12); `UPDATE`/`DELETE` on `status_logs`; `DELETE` on `attachments`; `UPDATE`/`DELETE` on `locations`; `UPDATE` on email columns and `complaints.submitted_at`; and `TRUNCATE`, `REFERENCES`, `TRIGGER`.

## 7. Sequence Privileges

- The runtime and read-only groups have **no** privileges on the 8 sequences.
- The backup and DBA groups have `SELECT` on all 8 sequences.
- All 8 sequences are owned by `postgres`.
- A design that used `SERIAL` columns would need a `USAGE` grant instead; `GENERATED ALWAYS AS IDENTITY` does not.

## 8. Default Privileges

`ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA dropboxed` is configured for the **backup** and **DBA** groups only:

- New tables and sequences are readable by the backup group automatically, so backups always include them.
- New tables receive `SELECT`, `INSERT`, `UPDATE`, `DELETE`, `MAINTAIN` for the DBA group, and new sequences are readable by it.
- The runtime and read-only groups receive nothing by default; each new table needs a deliberate grant in `04_privileges.sql`.

## 9. Planned Access-Control Verification

| **Account** | **Operation** | **Expected result** |
|---|---|---|
| `dropboxed_app` | Connect to `dropboxed_db` | Allowed |
| `dropboxed_app` | `SELECT` from `complaints` | Allowed |
| `dropboxed_app` | `INSERT` into `complaint_categories` | Allowed |
| `dropboxed_app` | `UPDATE` complaint status / flag fields | Allowed |
| `dropboxed_app` | `DELETE` from `complaint_assignments`, `complaint_categories` | Allowed |
| `dropboxed_app` | `DELETE` from `complaints`, `users`, `attachments` | Denied |
| `dropboxed_app` | `UPDATE`/`DELETE` on `status_logs` | Denied |
| `dropboxed_app` | `UPDATE users.email`, `UPDATE complaints.submitted_at` | Denied |
| `dropboxed_app` | `TRUNCATE`, `DROP TABLE`, `CREATE TABLE` in `dropboxed` or `public` | Denied |
| `dropboxed_app` | `nextval()` on an application sequence | Denied |
| `dropboxed_app` | `CREATE DATABASE`, `CREATE ROLE` | Denied |
| `dropboxed_app` | Connect to an unrelated database whose `PUBLIC` access was closed | Denied |
| `dropboxed_report` | `SELECT` complaint counts; `SELECT user_id, full_name` | Allowed |
| `dropboxed_report` | `SELECT password_hash`, `SELECT email`, `SELECT * FROM staff` | Denied |
| `dropboxed_report` | `INSERT`, `UPDATE`, `DELETE`, `ANALYZE`, `CREATE DATABASE` | Denied |
| `dropboxed_backup` | `SELECT` on all tables, `SELECT last_value` from sequences, `LOCK TABLE ... ACCESS SHARE` | Allowed |
| `dropboxed_backup` | `UPDATE`, `DELETE`, `CREATE ROLE` | Denied |
| `dropboxed_dba` | `SELECT`, `UPDATE`, `ANALYZE`, `VACUUM`, `SELECT` on `pg_stat_activity` | Allowed |
| `dropboxed_dba` | `DROP TABLE`, `TRUNCATE`, `CREATE DATABASE`, `CREATE ROLE` | Denied |
| outsider (no group membership) | Connect to `dropboxed_db` | Denied |

Each statement runs inside a transaction that is rolled back, so no application data changes. The recorded result is 48 tests with 0 unexpected outcomes on PostgreSQL 18.6.

Additional attribute checks:

| **Check** | **Expected result** |
|---|---|
| The four login roles have `LOGIN`; the four group roles have `NOLOGIN` | Confirmed |
| Each login role is a member of exactly one group role | Confirmed |
| No `dropboxed_` role has `SUPERUSER`, `CREATEDB`, `CREATEROLE`, `REPLICATION`, `BYPASSRLS` | Confirmed |
| `PUBLIC` has no `CONNECT` or `TEMP` on `dropboxed_db` | Confirmed |
| Runtime has `USAGE` but not `CREATE` on `dropboxed` | Confirmed |
| Runtime has no sequence privilege, yet `INSERT` still works | Confirmed |
| Database and all objects owned by `postgres` | Confirmed |
| Default privileges apply to a newly created table (backup/DBA only) | Confirmed |

## 10. Implementation Notes

- Database-level privileges are configured for `dropboxed_db`; schema and object-level privileges are configured while connected to that database.
- `CONNECT` allows a login to reach the database but grants no table access on its own.
- `USAGE` on schema `dropboxed` allows access to objects once the object-level privilege is also held.
- Privileges are attached to group roles; login roles hold no direct grants, so changing a group changes every member at once.
- `04_privileges.sql` begins by removing earlier grants, so it is always the complete statement of who can do what, and it uses `current_database()` so it also works on a restored test database.
- Scripts are safe to run again, and no password is stored in them; passwords reach the server as SCRAM verifiers.
- Roles are cluster-wide and are not part of a single-database dump, so `03_roles.sql` runs before a restore and `04_privileges.sql` after it.

**Known limitations**

- PostgreSQL grants `CONNECT` to `PUBLIC` on every new database; only `dropboxed_db` is closed because the scripts must not alter other projects. On a shared server, `pg_hba.conf` rules can restrict this further.
- Staff and administrators share one application role, so any staff member can create or deactivate another staff account. A separate administrator role would need a change to the `staff` table.
- The backup account can read `password_hash`, so dumps contain them and must be protected.
- Default privileges apply only to objects created by `postgres`; objects created by other roles are not covered.

---

**Related implementation files**

- Roles script: `database/03_roles.sql`
- Privileges script: `database/04_privileges.sql`
- Verification script: `scripts/verify-privileges.js`
- Access-control rationale: `docs/db-roles-setup.md`
- Application and database role mapping: `docs/user-roles.md`
- Documentation: `DropBoxed_Activity4_Documentation.pdf`
