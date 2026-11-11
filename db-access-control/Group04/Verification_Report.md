# Group04 – HealthBridge Clinic Management System
## Database Access Control Verification Report

### 1. Testing Environment

| Item | Details |
|---|---|
| Project | HealthBridge Clinic Management System |
| Actual Group | Group04 |
| Database | `grp03_projectdb` |
| Database Server | PostgreSQL 18.6 |
| Schema | `public` |
| Application Backend | PHP |
| Web Server | Apache (XAMPP) |
| Runtime Database Account | `hb_app` |

### 2. Database Setup Verification

The database was created successfully, and the project SQL files were executed in the required order.

Verified database objects:

- 11 base tables
- 2 views
- 3 PostgreSQL roles

The database contains the expected clinic management objects, including patients, doctors, appointments, medical notes, prescriptions, billing, password reset records, and deleted records.

### 3. Role Verification

| Role | Login | Inheritance | Result |
|---|---|---|---|
| `hb_admin` | No | Yes | Passed |
| `hb_staff` | No | Yes | Passed |
| `hb_app` | Yes | Yes | Passed |

Membership verification confirmed that `hb_app` inherits permissions from `hb_staff`.

### 4. Allowed Operations

| Operation | Result |
|---|---|
| SELECT from patients | Passed |
| SELECT from appointments | Passed |
| SELECT from doctors | Passed |
| SELECT from billing | Passed |
| SELECT from appointment_details | Passed after correction |
| SELECT from visit_records | Passed after correction |
| Use patients sequence | Passed |
| Verify hb_admin table privileges | Passed |
| Verify hb_admin sequence privileges | Passed |

### 5. Denied Operations

| Operation | Expected Result | Actual Result |
|---|---|---|
| Create a table | Denied | Passed |
| Delete password reset records | Denied | Passed |
| Create a database | Denied | Passed |
| Create a PostgreSQL role | Denied | Passed |

All tested restricted operations were denied as expected.

### 6. PHP Application Connection Test

The project was copied to the XAMPP `htdocs` directory and accessed through Apache.

The following were verified:

- Patient login page loaded successfully.
- PHP PostgreSQL extension `pdo_pgsql` was enabled.
- The `get_doctors.php` endpoint connected successfully to PostgreSQL.
- The API returned `success: true`.
- Three doctor records were retrieved successfully.

The application uses `hb_app` as its database account.

### 7. Issues and Resolutions

**Missing view permissions**

The initial application account could not access `appointment_details` and `visit_records`.

Resolution: SELECT permissions were granted to `hb_staff`. Both view queries succeeded after the correction.

**Shared database account**

The application uses `hb_app` for multiple application roles. PHP performs individual role checks, but PostgreSQL sees the same database account for these operations.

Recommendation: Consider stronger database-level separation for sensitive operations.

**Administrative ownership**

The `hb_admin` role has full privileges on the tested tables and sequences but does not own those objects.

Recommendation: Review object ownership if administrative DDL operations are required.

**Group number discrepancy**

The PDF identifies the project as Group04, while the repository and database use the `GRP03` prefix. Activity 4 files are organized under the Group04 folder.

### 8. Evidence

The following screenshots were collected:

1. `[01]_grp03_database_objects.png`
2. `[02]_grp03_roles_memberships.png`
3. `[03]_grp03_allowed_access.png`
4. `[04]_grp03_denied_access.png`
5. `[05]_grp03_admin_privileges.png`
6. `[06]_grp03_admin_default_privileges.png`
7. `[07]_grp03_php_database_connection.png`

All screenshots are stored in the `evidence` folder.

### 9. Overall Result

**Database Setup:** Success with issues

**Access Control:** Privileges tested

The database setup, role configuration, allowed and denied permission tests, and PHP-to-PostgreSQL connection test were successful. The missing view permissions were corrected and documented. Additional security improvements are recommended for shared application credentials and administrative object ownership.