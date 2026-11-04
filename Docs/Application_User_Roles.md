
# CAR RENTAL MANAGEMENT SYSTEM – APPLICATION USER ROLES DOCUMENTATION

## 1. Overview

The Car Rental Management System is an application designed to manage vehicle reservations, rental transactions, customer information, payments, and vehicle maintenance. It has five application user roles: System Administrator, Administrator, Rental Staff, Finance, and Customer. Each role has specific responsibilities, functions, and access restrictions within the application.

## 2. Application User Roles

| Application Role | Responsibilities in the Application | Functions and Data Access | Intended Database Role |
|---|---|---|---|
| **System Administrator** | Manages the overall system, user access, security, and technical configurations. | Manage application user accounts and role assignments; configure system settings; monitor system activities and audit logs; manage access permissions; oversee system maintenance and technical issues. | `app_admin_grp` |
| **Administrator** | Manages user accounts, system information, vehicle records, and overall rental operations. | Create, view, update, and manage staff accounts; manage vehicles and vehicle categories; oversee reservations, rentals, cancellations, maintenance, and damage records; view reports and audit logs. | `app_admin_grp` |
| **Rental Staff** | Handles daily rental operations and assists customers with reservations and vehicle-related transactions. | View and manage customer information; verify driver's licenses; review and approve or reject reservations; manage vehicle release and return inspections; record damage incidents and maintenance activities according to assigned permissions. | `app_runtime_grp` |
| **Finance** | Handles financial transactions, payment monitoring, and financial records of the rental business. | View rental charges and payment records; record and verify payments; manage payment statuses and receipts; monitor outstanding balances and damage fees; process and record eligible refunds; prepare and review financial reports. | `app_runtime_grp` |
| **Customer** | Manages personal information, driver's license submissions, and vehicle reservations. | Register and manage personal account information; submit driver's license details and required photos for verification; browse available vehicles; create, view, and cancel reservations; acknowledge rental terms; view personal rental history, payment records, and relevant damage fees. | `app_runtime_grp` |

### Intended Database Roles

`app_runtime_grp` is the proposed PostgreSQL group role for routine application database operations. It should have access only to the data and operations required by the application.

`app_admin_grp` is the proposed PostgreSQL group role for authorized administrative database operations. It may be granted additional permissions for managing system records and administrative functions.

These database roles are separate from application login roles. They do not automatically grant PostgreSQL superuser access. The actual database permissions must be configured separately.

## 3. Application Role Restrictions

### System Administrator

- Manages application user accounts, role assignments, and access permissions.
- Configures system settings and monitors system activities.
- Can view audit logs and monitor important system actions.
- Handles system maintenance and technical issues.
- Does not normally handle daily rental transactions or customer payments.

### Administrator

- Manages staff accounts, vehicles, and vehicle categories.
- Oversees reservations, rental transactions, cancellations, and maintenance records.
- Can view damage reports, transaction history, and audit logs.
- Has access to administrative functions based on assigned permissions.
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
- Reviews and records damage fees and eligible refunds.
- Prepares and reviews financial reports.
- Cannot modify vehicle records, verify driver's licenses, or approve reservations unless additional permissions are assigned.

### Customer

- Manages only their own account and personal information.
- Submits driver's license information and photos for staff verification.
- Browses available vehicles and manages their own reservations.
- Reviews and acknowledges rental terms before proceeding with a rental.
- Views only their own rental history, payment records, and relevant damage-fee information.
- Cannot approve their own reservations, verify their own driver's license, or modify staff-managed vehicle and payment records.

## 4. Summary

The Car Rental Management System uses five application roles to organize access to its main functions. The System Administrator manages system access, configurations, and security; the Administrator oversees the main rental operations; Rental Staff handle customer assistance, license verification, reservations, and vehicle inspections; Finance manages payments, receipts, refunds, and financial records; and Customers manage their accounts, submit driver's license information, reserve vehicles, and view their own rental records. Each role has specific responsibilities and access restrictions to help maintain organized and secure rental operations.
