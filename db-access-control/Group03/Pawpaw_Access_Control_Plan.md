# Pawpaw Clinic Access Control Plan

**Activity:** COMP 018 – Database Administration, Activity 4  
**Project:** Pawpaw Clinic  
**Database:** `group3_pawpaw`  
**Schema:** `public`  
**Database Platform:** PostgreSQL 18.6

## 1. Purpose

This access control plan defines the PostgreSQL login accounts required by the Pawpaw Clinic application, the group roles assigned to those accounts, their responsibilities, and the privileges needed to perform their tasks.

The plan follows the principle of least privilege. Each account receives only the permissions required for its intended responsibility. The planned roles and privileges are aligned with the PostgreSQL configuration and access-control tests documented in the Activity 4 Progress Documentation.

## 2. Access Control Plan

| **Login account** | **Group role** | **Application role served** | **Responsibility** | **Required privileges** |
|---|---|---|---|---|
| `pawpaw_dba` | `app_admin_grp` | Database Administrator | Administer and maintain the Pawpaw Clinic database, manage database objects, and perform authorized database maintenance tasks. | `CONNECT` on `group3_pawpaw`; `USAGE` and `CREATE` on schema `public`; all privileges on existing tables and sequences. |
| `pawpaw_app` | `app_runtime_grp` | Application Runtime | Allow the Pawpaw Clinic backend to perform normal application operations, including reading, inserting, updating, and deleting application records when required by application workflows. | `CONNECT` on `group3_pawpaw`; `USAGE` on schema `public`; `SELECT`, `INSERT`, `UPDATE`, and `DELETE` on existing tables; `USAGE` and `SELECT` on sequences. |

**Note:** The application may have its own user roles, such as Admin, Staff, Veterinarian, and Pet Owner. These are application-level roles and are separate from the PostgreSQL login roles listed in this plan.

No separate PostgreSQL auditor or read-only login account is included because no separate auditor/read-only role was created for the current database configuration.

## 3. Role Restrictions and Security Decisions

### 3.1 Database Administrator — `pawpaw_dba`

- Uses the `app_admin_grp` group role.
- Receives administrative privileges needed to manage database objects.
- Has `USAGE` and `CREATE` privileges on the `public` schema.
- Has all privileges on existing tables and sequences in the `public` schema.
- Does not receive the `SUPERUSER`, `CREATEDB`, or `CREATEROLE` attributes.
- Existing table and sequence ownership remains with `postgres`; granting privileges does not transfer ownership.
- Default privileges are configured for future tables and sequences created by the `postgres` role in the `public` schema.

### 3.2 Application Runtime — `pawpaw_app`

- Uses the `app_runtime_grp` group role.
- Can read, insert, update, and delete records as required by normal application operations.
- Has `USAGE` on the `public` schema but does not have `CREATE` permission on that schema.
- Has `USAGE` and `SELECT` privileges on existing sequences to support operations that generate identifiers.
- Does not receive the `SUPERUSER`, `CREATEDB`, or `CREATEROLE` attributes.
- Must not use a PostgreSQL superuser account for ordinary application access.
- Default privileges are configured for future tables and sequences created by the `postgres` role in the `public` schema.

### 3.3 Auditor / Read-only User

- No separate auditor or read-only group role was created.
- No auditor login account was created or tested.
- Therefore, no auditor-specific table, column, or sequence privileges are included in this plan.

## 4. Group Role and Login Role Design

Group roles are created with the `NOLOGIN` attribute so they serve as privilege containers rather than accounts used for direct authentication. Individual login roles are created with the `LOGIN` attribute and assigned to their corresponding group roles.

| **Group role** | **Login role** | **Login capability** |
|---|---|---|
| `app_admin_grp` | `pawpaw_dba` | `LOGIN` |
| `app_runtime_grp` | `pawpaw_app` | `LOGIN` |

Passwords are configured locally and must not be included in this plan, SQL scripts, screenshots, or the project repository.

## 5. Planned Access-Control Verification

The following tests are used to verify that the configured privileges match the plan.

| **Account** | **Operation** | **Expected result** |
|---|---|---|
| `pawpaw_app` | Connect to `group3_pawpaw` | Allowed |
| `pawpaw_app` | Read an application record | Allowed |
| `pawpaw_app` | Insert a record | Allowed |
| `pawpaw_app` | Update a record | Allowed |
| `pawpaw_app` | Delete an eligible, unreferenced record | Allowed |
| `pawpaw_app` | Create a table in schema `public` | Denied |
| `pawpaw_app` | Create a database | Denied |
| `pawpaw_app` | Create a PostgreSQL role | Denied |
| `pawpaw_dba` | Connect to `group3_pawpaw` | Allowed |
| `pawpaw_dba` | Read, insert, update, and delete table records | Allowed |
| `pawpaw_dba` | Create a table in schema `public` | Allowed |
| `pawpaw_dba` | Create a database | Denied |
| `pawpaw_dba` | Create a PostgreSQL role | Denied |

The delete test is subject to database constraints. A record referenced by another table may not be deleted because of foreign-key restrictions. The successful runtime delete test used an eligible record and was rolled back after verification.

Actual test results and supporting screenshots are recorded separately in the Activity 4 Progress Documentation.

## 6. Implementation Notes

- Database-level privileges are configured for `group3_pawpaw`.
- Schema and object-level privileges are configured while connected to the target database.
- `CONNECT` permission allows a role to connect to the database but does not automatically grant access to tables.
- `USAGE` on the `public` schema allows a role to access objects in the schema when it also has the required object-level privileges.
- The runtime group role receives `USAGE` on the schema but not `CREATE`.
- The admin group role receives `USAGE` and `CREATE` on the schema.
- Runtime table privileges include `SELECT`, `INSERT`, `UPDATE`, and `DELETE`.
- Sequence privileges are included to support application operations that use generated identifiers.
- Default privileges are configured for future tables and sequences created by the `postgres` role in the `public` schema. These default privileges do not automatically apply to objects created by other roles.
- `CONNECT` was revoked from `PUBLIC` on `group3_pawpaw`; the two configured group roles were granted `CONNECT`.
- Other databases in the same PostgreSQL instance may have their own `PUBLIC` connection settings. Those settings are outside the scope of this Pawpaw Clinic access-control configuration.
- The plan describes the intended access structure. The SQL script and verification evidence document the implementation and test results.

---

**Related implementation files**

- SQL script: `roles-and-privileges.sql`
- Progress documentation: `progress-documentation.pdf`
- Evidence folder: `evidence/`
