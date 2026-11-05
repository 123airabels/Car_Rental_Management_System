
# CAR RENTAL MANAGEMENT SYSTEM – APPLICATION USER ROLES DOCUMENTATION

## 1. Overview

The Car Rental Management System is an application designed to manage vehicle reservations, rental transactions, customer information, payments, and vehicle maintenance. It has five application user roles: System Administrator, Administrator, Rental Staff, Finance, and Customer. Each role has specific responsibilities, functions, and access restrictions within the application.

## 2. Application User Roles

| Application Role | Responsibilities in the Application | Functions and Data Access | Intended Database Role |
|---|---|---|---|
| **System Administrator** | Manages the overall system, user access, security, and technical configurations. | Create, deactivate and assign roles to all staff accounts (including Admin and System Admin); view the audit log. | `app_runtime_grp` (the application connection) |
| **Administrator** | Manages user accounts, system information, vehicle records, and overall rental operations. | Create, deactivate and re-assign Staff and Finance accounts only; manage vehicles, categories and rates; suspend or reactivate customers; oversee reservations, rentals, cancellations, maintenance, damage records and payments; view reports and the audit log. | `app_runtime_grp` (the application connection) |
| **Rental Staff** | Handles daily rental operations and assists customers with reservations and vehicle-related transactions. | View and manage customer information; verify driver's licenses; review and approve or reject reservations; manage vehicle release and return inspections; record damage incidents and maintenance activities according to assigned permissions. | `app_runtime_grp` |
| **Finance** | Handles financial transactions, payment monitoring, and financial records of the rental business. | View rental charges and payment records; record and verify payments; manage payment statuses and receipts; monitor outstanding balances and damage fees; process and record eligible refunds; prepare and review financial reports. | `app_runtime_grp` |
| **Customer** | Manages personal information, driver's license submissions, and vehicle reservations. | Register and manage personal account information; submit driver's license details and required photos for verification; browse available vehicles; create, view, and cancel reservations; acknowledge rental terms; view personal rental history, payment records, and relevant damage fees. | `app_runtime_grp` |

### Account Emails & Password
#### Admin 
Email: admin@carrental.example
Password: Sample@12345

#### System Admin 
Email: sysadmin@carrental.example
Password: Sample@12345

#### Staff
Email: staff@carrental.example
Password: Sample@12345

#### Finance
Email: finance@carrental.example
Password: Sample@12345

### Intended Database Roles

`app_runtime_grp` is the proposed PostgreSQL group role for routine application database operations. It should have access only to the data and operations required by the application.

`app_admin_grp` is the PostgreSQL group role for authorized database administration (maintenance, schema changes). It is for a DBA login only. The System Administrator and Administrator application roles do not use it: every application role reaches the database through the same `app_runtime_grp` connection, and the backend decides what each user may do. See `Database/db_roles_and_grants.sql`.

These database roles are separate from application login roles. They do not automatically grant PostgreSQL superuser access. The actual database permissions must be configured separately.

## 3. Application Role Restrictions

### System Administrator

- Creates, deactivates and assigns roles to all staff accounts. It is the only role that can create or change Admin and System Admin accounts.
- Can view the audit log.
- Cannot be removed or deactivated if it is the last active System Admin.
- Has no access to bookings, customers, payments, vehicles, damages or maintenance.

### Administrator

- Manages Staff and Finance accounts only. Cannot create, change or deactivate Admin or System Admin accounts.
- Manages vehicles, categories and rates, and can suspend or reactivate customers. Has view-only access to customers' personal information.
- Oversees reservations, rental transactions, cancellations, payments, maintenance and damage records.
- Can view reports, transaction history and the audit log.
- Does not automatically have PostgreSQL superuser access.

### Rental Staff

- Verifies submitted driver's license information before approving eligible reservations.
- Manages reservation approval, rejection, and cancellation according to business rules.
- Records vehicle release and return inspections, including vehicle condition and photo evidence.
- Records damage incidents and maintenance activities according to assigned permissions.
- Access to administrative account settings and restricted financial functions is limited.

### Finance

- Handles and monitors customer payments and rental charges.
- Records and verifies payments and updates payment statuses.
- Manages payment receipts and monitors outstanding balances.
- Reviews and records damage fees (but does not decide customer disputes) and eligible refunds.
- Prepares and reviews financial reports.
- Cannot modify vehicle records, verify driver's licenses, or approve reservations unless additional permissions are assigned.

### Customer

- Manages only their own account and personal information.
- Submits driver's license information and photos for staff verification.
- Browses available vehicles and manages their own reservations.
- Reviews and acknowledges rental terms before proceeding with a rental.
- Views only their own rental history, payment records, and relevant damage-fee information.
- Cannot approve their own reservations, verify their own driver's license, or modify staff-managed vehicle and payment records.

## 4. Access Matrix (as enforced by the application)

| Area | System Admin | Admin | Staff | Finance | Customer |
|---|---|---|---|---|---|
| Staff accounts: create, deactivate, change role | All roles | Staff and Finance only | No | No | No |
| Audit log | View | View | No | No | No |
| Vehicles: add, edit, delete; categories; rates | No | Yes | View | View | Browse |
| Customers: view personal information | No | Yes | Yes | No | Own only |
| Customers: edit personal information | No | No (view only) | Yes | No | Own only |
| Suspend or reactivate a customer | No | Yes | No | No | No |
| Driver's licenses: verify, reject | No | Yes | Yes | No | Submit own |
| Bookings: approve, reject, cancel (the customer's chosen vehicle cannot be swapped); pickup and return; inspections | No | Yes | Yes | View | Own |
| Damage: record incident, set fee | No | Yes | Yes (incident and fee) | Fee only | View own, dispute |
| Damage: decide a dispute | No | Yes | Yes | No | No |
| Maintenance | No | Yes | Yes | No | No |
| Payments: confirm, reject, refund; reports | No | Yes | No | Yes | Pay own |
| Dashboard | No | Full | Operations only | Operations and money | No |

## 5. Summary

The Car Rental Management System uses five application roles to organize access to its main functions. The System Administrator manages system access, configurations, and security; the Administrator oversees the main rental operations; Rental Staff handle customer assistance, license verification, reservations, and vehicle inspections; Finance manages payments, receipts, refunds, and financial records; and Customers manage their accounts, submit driver's license information, reserve vehicles, and view their own rental records. Each role has specific responsibilities and access restrictions to help maintain organized and secure rental operations.