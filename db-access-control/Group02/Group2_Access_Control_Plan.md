# Group 2 – High School Event Management System
# Database Access Control Plan

## 1. Project Overview

**Project Name:** High School Event Management System

**Database:** `grp02_event_management_db`

**Schema:** `school_event_management_db`

**DBMS:** PostgreSQL 18

**Application:** React 19 and Spring Boot 4.1

The High School Event Management System manages school events, participants, attendance, event assignments, schedules, and venues. The application uses PostgreSQL to store and manage its records.

## 2. Database Accounts and Group Roles

| Login Account | Group Role | Application Role Served |
|---|---|---|
| `event_app` | No group role | Application Runtime |
| `event_owner` | No group role | Schema Owner / Migration Account |
| `postgres` | PostgreSQL superuser | Database Administrator / Setup Only |

The following NOLOGIN group roles also exist in PostgreSQL:

- `app_runtime_grp`
- `app_admin_grp`

However, these group roles are not assigned to `event_app` or `event_owner`. No Group 2 schema or table privileges were found for these group roles.

The application also has the roles **Super Admin** and **Event Organizer**. These are application-level roles and are separate from PostgreSQL database accounts.

## 3. Access Control Plan

### 3.1 Application Runtime – `event_app`

**Account:** `event_app`

**Responsibilities:**

- Connect the application to the PostgreSQL database.
- Retrieve records needed by the application.
- Perform authorized database operations.
- Insert and update records required by the system.
- Access the required database sequences.

**Assigned Privileges:**

- CONNECT to the `grp02_event_management_db` database.
- USAGE on the `school_event_management_db` schema.
- SELECT on authorized application tables.
- INSERT on authorized application tables.
- UPDATE on authorized application tables.
- DELETE on authorized application tables.
- USAGE on all nine application sequences.
- TEMP privilege for creating temporary tables.

**Restrictions:**

- Cannot create databases.
- Cannot create PostgreSQL roles.
- Cannot create tables in the application schema.
- Cannot TRUNCATE application tables.
- Cannot modify the `role` table.
- Cannot DELETE records from `user_account` and `audit_log`.
- Cannot INSERT or UPDATE records in the `role` table.

### 3.2 Schema Ownership – `event_owner`

**Account:** `event_owner`

**Responsibilities:**

- Own the application schema.
- Own application tables and sequences.
- Manage schema objects and database migrations.
- Support database initialization and maintenance.

**Assigned Privileges:**

- CONNECT to the `grp02_event_management_db` database.
- USAGE and CREATE on the `school_event_management_db` schema.
- Broad privileges over its owned application tables.
- Ownership of the nine application sequences.
- TEMP privilege for creating temporary tables.

**Restrictions:**

- Does not have the CREATEDB attribute.
- Does not have superuser privileges.
- Is not a member of `app_admin_grp` or `app_runtime_grp`.

This account is intended for schema-related operations and migrations rather than ordinary application runtime access.

### 3.3 Database Administration – `postgres`

**Account:** `postgres`

**Responsibilities:**

- Provision and administer the PostgreSQL database environment.
- Manage database-level configuration.
- Perform privileged database setup operations when necessary.

**Privileges:**

- PostgreSQL superuser privileges.
- Database and role administration capabilities.

This account is not intended for normal application runtime operations.

### 3.4 Existing Group Roles

**Group Roles:**

- `app_runtime_grp`
- `app_admin_grp`

Both are configured as NOLOGIN roles.

During inspection, neither role had Group 2 table or schema privileges, and neither `event_app` nor `event_owner` was a member of these roles.

**Recommendation:** Assigning common application permissions through a dedicated NOLOGIN group role may simplify future privilege management. This was not part of the existing Group 2 configuration verified during the activity.

## 4. Principle of Least Privilege

The inspection evaluated the database accounts based on their actual permissions and responsibilities.

The `event_app` account is a non-superuser login with limited database access. It can perform authorized application operations but cannot create database roles, create application schema objects, or truncate application tables.

The `event_owner` account is responsible for schema ownership and object management, while `postgres` retains database superuser responsibilities.

The inspection also identified that the runtime account can connect to some unrelated databases because of PostgreSQL's default PUBLIC connection privileges. This is a security finding that requires further review and authorization before making changes.

## 5. Verification and Testing

The access-control tests were executed using the `event_app` account.

| Test | Result |
|---|---|
| Application account database connection | Passed |
| Read records from an authorized table | Passed |
| Insert an authorized test record | Passed; transaction rolled back |
| Verify rollback | Passed; no test record remained |
| Create a table in the application schema | Denied as expected |
| TRUNCATE an application table | Denied as expected |
| Create a PostgreSQL role | Denied as expected |
| Create a temporary table | Passed |
| Database creation privilege check | No CREATE DATABASE privilege |
| Schema privilege check | USAGE allowed; CREATE denied |
| Table privilege inspection | Completed |
| Sequence privilege inspection | Completed |
| Role membership inspection | Completed |
| Other database connection inspection | Completed |

Additional tests confirmed that the runtime account cannot INSERT, UPDATE, or DELETE records in the `role` table, while permitted INSERT, UPDATE, and DELETE operations on the `event` table were successful.

The evidence screenshots are stored in the Group 2 evidence folders.

## 6. Additional Database Verification

The PostgreSQL inspection confirmed the following:

- The database `grp02_event_management_db` is owned by `postgres`.
- The schema `school_event_management_db` is owned by `event_owner`.
- Ten tables were identified in the application schema.
- Nine sequences were identified and owned by `event_owner`.
- `event_app` and `event_owner` are non-superuser login accounts.
- Both accounts can connect to the target database.
- Both accounts have TEMP privileges.
- `event_app` has USAGE but not CREATE on the application schema.
- `event_owner` has USAGE and CREATE on the application schema.
- `event_app` has direct table privileges and USAGE on the nine sequences.
- No Group 2 default privileges were found in the inspected schema.
- No membership of the Group 2 login accounts in `app_runtime_grp` or `app_admin_grp` was found.

**Verification Status:** Database objects, role memberships, privileges, and runtime account restrictions were inspected and tested.

**Evidence Folder:** `db-access-control/Group02/evidence/`

## 7. Conclusion

The Group 2 High School Event Management System database access inspection documented the existing PostgreSQL accounts, group roles, database ownership, schema permissions, table privileges, and sequence privileges.

The verification tests confirmed that the `event_app` account can perform authorized application operations while restricted operations are denied.

The inspection also identified areas for improvement, particularly the use of direct table grants instead of group-role membership and the default PUBLIC database connection privileges.

The documented findings and recommendations provide a basis for maintaining and improving the database's access-control configuration according to the principle of least privilege.
