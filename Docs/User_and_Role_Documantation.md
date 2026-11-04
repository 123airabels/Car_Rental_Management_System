# USER AND ROLE SETUP DOCUMENTATION

## Purpose

This document defines the intended PostgreSQL roles and privileges for the Car Rental Management System. The goal is to follow the principle of least privilege: every database account should receive only the permissions required for its assigned tasks.

## Intended PostgreSQL Group Roles

| **PostgreSQL group role** | **Intended purpose** |
|---|---|
| `app_runtime_grp` | Provides the minimum table and sequence privileges required by the running application. |
| `app_readonly_grp` | Provides read-only access to approved tables or views for authorized review and reporting. |
| `app_admin_grp` | Provides additional, explicitly approved administrative database privileges when necessary. It must not be a superuser role. |

## Login Accounts

A PostgreSQL group role can be created as a `NOLOGIN` role and assigned to one or more login accounts.

| **Intended account** | **Purpose** | **Intended membership** |
|---|---|---|
| Application runtime account | Used by Spring Boot to connect to PostgreSQL. | `app_runtime_grp` |
| Read-only account | Used for authorized read-only database inspection or reporting. | `app_readonly_grp` |
| Database administrator account | Used by an authorized administrator for approved database maintenance and administration. | `app_admin_grp`, if required |

## Privilege Plan

### Application Runtime

The application runtime account should receive only the privileges needed for implemented application operations, such as:

- **CONNECT** to the project database.
- **USAGE** on the required schema.
- Appropriate **SELECT**, **INSERT**, **UPDATE**, and, only where needed, **DELETE** privileges on relevant tables.
- Required sequence privileges for generated identifiers.

Do not automatically grant every privilege on every table. Review which tables and operations the application actually needs.

### Read-Only Access

The read-only role should receive:

- **CONNECT** to the project database.
- **USAGE** on the approved schema.
- **SELECT** on approved tables or views only.

It should not receive permission to insert, update, delete, create tables, or modify database objects.

### Administrative Database Access

- Any additional administrative privileges should be granted only when justified by an administrative task. Routine application requests must not connect as a PostgreSQL superuser.
- Database ownership, role creation, schema changes, backups, and other maintenance tasks should be handled by an authorized account with the necessary privileges.

## Application Roles Versus Database Roles

- Customer, Staff, Finance, Admin, and System Admin are application roles. They are not automatically separate PostgreSQL login accounts.
- The Spring Boot backend enforces the application user's permissions. PostgreSQL privileges limit what the database account used by the application can do.
- For example, a Customer should be restricted by application logic to their own rental records. A shared runtime account alone does not distinguish individual customers, so backend authorization is necessary.
- The Admin and System Admin application roles must not automatically receive PostgreSQL superuser privileges.

## Security Requirements

- Never commit actual database passwords.
- Use environment variables or another approved secret-management method.
- Do not use a superuser account for routine application connections.
- Avoid granting **CREATEDB**, **CREATEROLE**, or broad schema privileges without a justified requirement.
- Review default privileges and unnecessary PUBLIC access.
- Test both permitted and denied operations using the intended accounts.

## Verification and Status

- During Activity 4, record the actual role names, memberships, grants, and test results.
- Verify that authorized operations succeed and unauthorized operations fail.
