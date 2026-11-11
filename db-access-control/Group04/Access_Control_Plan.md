# Group04 – HealthBridge Clinic Management System
## Database Access Control Plan

### 1. Project Overview

The HealthBridge Clinic Management System (HBCMS) is a web-based clinic management application developed using HTML, CSS, JavaScript, PHP, and PostgreSQL.

The database used for this project is `grp03_projectdb`, with the `public` schema. Although the repository and database use the `GRP03` naming prefix, the project documentation identifies the actual group as **Group04**.

The database was successfully set up using PostgreSQL 18.6 for this Activity 4 verification.

### 2. Access Control Objectives

The objectives of the database access control implementation are to:

- Apply the principle of least privilege.
- Separate administrative and day-to-day database permissions.
- Prevent unauthorized database and schema modifications.
- Restrict access to sensitive patient and staff information.
- Verify permitted and restricted database operations.
- Ensure that the PHP application connects using a dedicated application account instead of the PostgreSQL superuser.

### 3. Database Roles

| Database Role | Role Type | Purpose |
|---|---|---|
| `hb_admin` | NOLOGIN | Administrative group role with full table and sequence privileges. |
| `hb_staff` | NOLOGIN | Group role for application operations and required data management. |
| `hb_app` | LOGIN | Application database account that inherits permissions from `hb_staff`. |

The application connects to PostgreSQL using `hb_app`. Patient, staff, doctor, and admin application accounts are handled by the PHP application and do not have separate PostgreSQL login accounts.

### 4. Permission Assignments

**hb_admin**
- Full privileges on existing tables in the public schema.
- Full privileges on existing sequences.
- Default full table privileges for future tables created by `postgres` in the public schema.

**hb_staff**
- SELECT, INSERT, UPDATE, and DELETE on patients, appointments, medical notes, prescriptions, and deleted records.
- SELECT on doctors and specialties.
- SELECT and UPDATE on staff users.
- SELECT, INSERT, and UPDATE on patient and staff password reset tables.
- SELECT, INSERT, and UPDATE on billing.
- SELECT access to `appointment_details` and `visit_records`.
- Required sequence privileges for application operations.

**hb_app**
- LOGIN access to the database.
- Membership in `hb_staff`.
- Inherits the permissions assigned to `hb_staff`.

### 5. Database and Schema Permissions

The roles were granted:

- CONNECT on `grp03_projectdb`.
- USAGE on the `public` schema.

The application account does not have permission to create tables or other schema objects.

### 6. Verification Results

The following tests were performed:

| Test | Expected Result | Actual Result |
|---|---|---|
| Read patient records | Allowed | Passed |
| Read appointment records | Allowed | Passed |
| Read doctor records | Allowed | Passed |
| Read billing records | Allowed | Passed |
| Read appointment details view | Allowed | Passed after permission correction |
| Read visit records view | Allowed | Passed after permission correction |
| Use patient sequence | Allowed | Passed |
| Create a table | Denied | Passed |
| Delete password reset records | Denied | Passed |
| Create a database | Denied | Passed |
| Create a PostgreSQL role | Denied | Passed |

The `hb_admin` role was also verified to have SELECT, INSERT, DELETE, and sequence USAGE privileges on the tested database objects.

### 7. Issues Found and Resolutions

**Issue 1: Missing view permissions**

The original role configuration did not grant SELECT permission on `appointment_details` and `visit_records` to `hb_staff`.

Resolution: SELECT privileges were granted to `hb_staff`. Both views were successfully accessed afterward through `hb_app`.

**Issue 2: Shared application database account**

The application uses one database login, `hb_app`, which inherits the permissions of `hb_staff`. Individual application roles are enforced by PHP session and authorization checks rather than separate PostgreSQL accounts.

Recommendation: Consider separate database roles or restricted database functions for more sensitive operations if stronger database-level separation is required.

**Issue 3: Administrative role ownership**

The `hb_admin` role has full privileges on the tested tables and sequences, but it is not the owner of those objects. Therefore, owner-only operations such as altering or dropping existing objects are not automatically available.

Recommendation: Review ownership requirements if full database administration is expected.

**Issue 4: Group number naming discrepancy**

The project documentation identifies the project as Group04, while its repository and database use the `GRP03` prefix. The Activity 4 deliverables are therefore stored under `db-access-control/Group04/`.

### 8. PHP and PostgreSQL Integration

The project was copied to the XAMPP `htdocs` directory and opened through Apache.

The PHP PostgreSQL connection was verified using the `get_doctors.php` endpoint. The API returned `success: true` and successfully retrieved three doctor records from the database.

The application configuration uses the dedicated `hb_app` account instead of the PostgreSQL superuser.

### 9. Conclusion

The Group04 HealthBridge database was successfully configured and tested. The application database account can perform the required operations while unauthorized schema creation, database creation, role creation, and password reset record deletion are denied.

The missing view permissions were identified and corrected. The remaining security considerations, particularly shared application credentials and administrative object ownership, are documented for further improvement.