# APPLICATION USER ROLES DOCUMENTATION

## Purpose

This document describes the people who use the Car Rental Management System and the actions each role is intended to perform. These are application-level roles. They are different from PostgreSQL roles, which control database access.

The roles below follow the planned system architecture documented in Activity 2. Their actual permissions must be enforced by the application and verified during development.

## Application Roles and Responsibilities

| **Application role** | **Responsibilities** | **Functions and data access** | **Intended database role** |
|---|---|---|---|
| **Customer** | Register an account, manage their profile, submit driver's license details, browse vehicles, request rentals, accept rental terms, review vehicle condition, submit inspection notes or photos, view their own rental history, cancel eligible bookings, and view payment and receipt records. | Create and read on own records; update own profile; read available vehicles and own transactions. | `app_runtime_grp` |
| **Staff** | Manage customer records, verify driver's licenses, manage vehicles and rates, review rental requests, approve or reject requests, conduct inspections, record damage, manage returns, and update vehicle status. | Create and read on operational records; update assigned records; manage vehicle and rental statuses. | `app_runtime_grp` |
| **Finance** | Review rental payments, payment status, receipt information, damage fees, refunds, and outstanding charges. | Read financial records; create and update payment, receipt, refund, and damage fee records. | `app_runtime_grp` |
| **Admin** | Manage application users and staff access, oversee system operations, review records and reports, and monitor important activities. | Create, read, update, and manage application users; read reports and audit logs. | `app_admin_grp` |
| **System Admin** | Manage application configuration, technical settings, and system-level administration. | Manage technical configuration and application settings; access only the database operations required for system maintenance. | `app_admin_grp` |

## Authentication and Authorization

- Users should authenticate through the application. The backend should verify their application role and authorize each requested operation.
- Customers must not be able to view or modify another customer's private records. Staff and Finance users should access only the information required for their responsibilities. Administrative access should be restricted to authorized users.
- The frontend alone must not be relied upon to enforce permissions. Access checks must also be performed by the backend.

## Relationship to PostgreSQL Roles

- Application roles describe what a person may do in the system. PostgreSQL roles describe what a database account may do directly in the database.
- Ordinary application users do not need individual PostgreSQL login accounts. The application normally connects through a restricted runtime account. The backend then enforces the permissions of Customer, Staff, Finance, Admin, and System Admin.
- For example, a Customer may be allowed to view only their own rentals through the application even though the runtime database account has the technical ability to execute the required rental queries.

## Implementation Status

- These roles are based on the planned architecture in Activity 2. They should not be considered fully implemented until authentication, role checks, and access restrictions have been developed and tested.
