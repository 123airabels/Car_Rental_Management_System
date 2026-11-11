# SHIORI Café
# Database Access Control Plan

## 1. Project Overview

**Project Name:** SHIORI Café

**Database:** grp10_shioricafedb

**Schema:** shioricafe_schema

**DBMS:** PostgreSQL 18

**Application:** PHP 8.2+ with PDO PostgreSQL

The SHIORI Café system manages customer accounts, reservations, orders, payments, menu information, and café operations. The application uses PostgreSQL to store and manage its records.

## 2. Database Accounts and Group Roles

The following database account was used during the inspection:

| Login Account | Group Role | Purpose |
|---|---|---|
| postgres | PostgreSQL superuser | Database Administrator |

The following NOLOGIN group roles exist in PostgreSQL:

- app_runtime_grp
- app_admin_grp
- activity4_test_role

The existing group roles are separate from the SHIORI Café application roles.

The application has the roles Guest, Customer, Cashier, and Manager. These are application-level roles and are separate from PostgreSQL database accounts.

## 3. Access Control Plan

### 3.1 Database Administration – postgres

**Account:** postgres

**Responsibilities:**

- Connect to the SHIORI Café database.
- Inspect database information and ownership.
- Inspect PostgreSQL accounts and group roles.
- Manage database-level configuration.
- Perform database administration tasks.

**Privileges:**

- PostgreSQL superuser privileges.
- Database and role administration capabilities.

This account is used for database administration.

### 3.2 Existing Group Roles

**Group Roles:**

- app_runtime_grp
- app_admin_grp
- activity4_test_role

These roles are configured as NOLOGIN roles in PostgreSQL.

During inspection, no SHIORI-specific role memberships or schema and table privileges were found for these roles.

## 4. Principle of Least Privilege

The database inspection identified the existing PostgreSQL accounts, group roles, and available privileges.

The postgres account handles database administration, while the existing group roles provide separate permission groups within the PostgreSQL environment.

The inspection also confirmed that no SHIORI-specific database accounts or role memberships were present.

## 5. Verification and Testing

The database inspection was performed using the postgres account.

| Test / Inspection | Result |
|---|---|
| Database connection | Passed |
| Database existence check | Passed |
| Database owner inspection | Completed |
| Database ACL inspection | Completed |
| Existing PostgreSQL group role inspection | Completed |
| SHIORI login account inspection | Completed |
| SHIORI role membership inspection | Completed |
| Application schema inspection | Completed |
| Application table inspection | Completed |
| Application sequence inspection | Completed |
| Schema privilege inspection | Completed |
| Table privilege inspection | Completed |
| Sequence privilege inspection | Completed |

## 6. Additional Database Verification

The PostgreSQL inspection confirmed the following:

- The database grp10_shioricafedb exists.
- The database is owned by postgres.
- The database datacl is NULL.
- The schema shioricafe_schema was not found.
- No SHIORI-specific login accounts or group roles were found.
- The existing general PostgreSQL group roles include app_runtime_grp, app_admin_grp, and activity4_test_role.
- No SHIORI-specific role memberships were found.
- No application tables or sequences were found under the target schema.
- No schema, table, or sequence privileges were found under the target schema.
- Other project-related PostgreSQL accounts and roles exist in the same PostgreSQL environment.

**Verification Status:** Database information, ownership, existing roles, schema, tables, sequences, and available privileges were inspected.

**Evidence Folder:** Evidence/

## 7. Conclusion

The SHIORI Café Database Access Control Plan documents the existing PostgreSQL database configuration, accounts, group roles, and privilege inspection results.

The inspection confirmed that the database grp10_shioricafedb exists and is owned by postgres. The existing PostgreSQL group roles were also identified, along with the current schema and privilege information.

The inspection provides documentation of the database access configuration and its existing role organization.
