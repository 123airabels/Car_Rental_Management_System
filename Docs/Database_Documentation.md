# CAR RENTAL MANAGEMENT SYSTEM — DATABASE DOCUMENTATION

## 1. Overview

The Car Rental Management System is a database-driven application designed to support car rental operations. Its PostgreSQL database stores customer accounts, driver's license information, staff accounts, vehicle records, rental rates, rental transactions, rental terms, vehicle inspections, inspection photos, damage incidents, damage fees, payments, maintenance records, cancellations, and staff activities.

The database helps organize rental information and maintain relationships between customers, vehicles, bookings, payments, and related records.

## 2. Database Information

| Item | Details |
|---|---|
| Database management system | PostgreSQL |
| Database name | `car_rental_db` |
| Schema | `public` |
| PostgreSQL version | 18.6 |
| Database type | Relational database |
| Schema definition | `Database/schema.sql` |
| Sample data file | `Database/seed.sql` |
| Tables defined | 16 |

The database schema defines the tables, primary keys, foreign keys, unique constraints, check constraints, indexes, and default values used by the system.

## 3. Database Tables

| Table | Primary Key | Purpose |
|---|---|---|
| `customer` | `customer_id` | Stores customer profile, contact, login, and account status information. |
| `driver_license` | `license_id` | Stores driver's license details, uploaded license photos, and verification status. |
| `staff` | `staff_id` | Stores staff account information, roles, and account status. |
| `vehicle_category` | `category_id` | Stores vehicle categories, such as Sedan, SUV, Van, and Pickup. |
| `rental_rate` | `rate_id` | Stores rental rates for vehicle categories and rental periods. |
| `vehicle` | `vehicle_id` | Stores vehicle details, plate numbers, category, and operational status. |
| `rental` | `rental_id` | Stores rental schedules, customer and vehicle references, rental costs, and rental status. |
| `rental_terms` | `terms_id` | Stores the rental terms associated with a rental and the customer's acknowledgment. |
| `vehicle_inspection` | `inspection_id` | Records vehicle condition, mileage, fuel level, and inspection details. |
| `inspection_photo` | `photo_id` | Stores photo references and descriptions associated with vehicle inspections. |
| `damage_incident` | `damage_id` | Records damage or incidents identified during vehicle inspections. |
| `damage_fee` | `damage_fee_id` | Stores assessed damage charges, dispute details, and fee resolution information. |
| `payment` | `payment_id` | Records rental or damage-fee payments, payment status, and receipt references. |
| `vehicle_maintenance` | `maintenance_id` | Records vehicle maintenance activities, costs, and maintenance status. |
| `audit_log` | `audit_id` | Records important actions performed by staff members. |
| `cancellation` | `cancellation_id` | Records rental cancellations, cancellation reasons, and refund information. |

## 4. Database Relationships and Foreign Keys

| Parent Table | Related Table | Foreign Key | Relationship / Rule |
|---|---|---|---|
| `customer` | `driver_license` | `driver_license.customer_id` | One customer can have multiple driver's license records; each license record belongs to one customer. |
| `staff` | `driver_license` | `driver_license.verified_by_staff_id` | One staff member can verify multiple licenses; the verifier reference is optional. |
| `vehicle_category` | `vehicle` | `vehicle.category_id` | One category can contain multiple vehicles; each vehicle belongs to one category. |
| `vehicle_category` | `rental_rate` | `rental_rate.category_id` | One category can have multiple rental rates; each rate belongs to one category. |
| `customer` | `rental` | `rental.customer_id` | One customer can have multiple rentals; each rental belongs to one customer. |
| `vehicle` | `rental` | `rental.vehicle_id` | One vehicle can be associated with multiple rentals over time; each rental references one vehicle. |
| `rental_rate` | `rental` | `rental.rate_id` | One rental rate can be used by multiple rentals; each rental references one rate. |
| `rental` | `rental_terms` | `rental_terms.rental_id` | Each rental can have at most one rental-terms record because `rental_id` is unique in `rental_terms`. |
| `rental` | `vehicle_inspection` | `vehicle_inspection.rental_id` | One rental can have multiple inspection records; each inspection belongs to one rental. |
| `staff` | `vehicle_inspection` | `vehicle_inspection.performed_by_staff_id` | One staff member can perform multiple inspections; the staff performer reference is optional. |
| `customer` | `vehicle_inspection` | `vehicle_inspection.performed_by_customer_id` | One customer can be associated with multiple inspections; the customer performer reference is optional. |
| `vehicle_inspection` | `inspection_photo` | `inspection_photo.inspection_id` | One inspection can have multiple photos; each photo belongs to one inspection. |
| `customer` | `inspection_photo` | `inspection_photo.uploaded_by_customer_id` | One customer can upload multiple inspection photos; the uploader reference is optional. |
| `staff` | `inspection_photo` | `inspection_photo.uploaded_by_staff_id` | One staff member can upload multiple inspection photos; the uploader reference is optional. |
| `vehicle_inspection` | `damage_incident` | `damage_incident.inspection_id` | One inspection can have multiple damage incidents; each damage incident belongs to one inspection. |
| `damage_incident` | `damage_fee` | `damage_fee.damage_id` | A damage incident can have at most one damage-fee record because `damage_id` is unique in `damage_fee`. |
| `rental` | `payment` | `payment.rental_id` | One rental can have multiple payments; a payment may reference a rental or a damage fee. |
| `damage_fee` | `payment` | `payment.damage_fee_id` | One damage fee can have multiple payments; a payment may reference a damage fee or a rental. |
| `staff` | `payment` | `payment.received_by` | One staff member can be associated with multiple payments received or recorded; the staff reference is optional. |
| `vehicle` | `vehicle_maintenance` | `vehicle_maintenance.vehicle_id` | One vehicle can have multiple maintenance records; each maintenance record belongs to one vehicle. |
| `staff` | `audit_log` | `audit_log.staff_id` | One staff member can have multiple audit entries; each audit entry references one staff member. |
| `rental` | `cancellation` | `cancellation.rental_id` | Each rental can have at most one cancellation record because `rental_id` is unique in `cancellation`. |
| `customer` | `cancellation` | `cancellation.cancelled_by_customer_id` | One customer can be associated with multiple cancellations; the customer reference is optional. |
| `staff` | `cancellation` | `cancellation.cancelled_by_staff_id` | One staff member can be associated with multiple cancellations; the staff reference is optional. |

The `payment` table can reference either a rental or a damage-fee record. A check constraint requires exactly one of these references to be supplied. The database also uses check constraints to require exactly one actor for a cancellation, one performer for an inspection, and one uploader for an inspection photo.

## 5. Keys and Unique Constraints

- **Primary keys:** Each table has a primary key that uniquely identifies its records.
- **Identity / sequence-generated keys:** The integer primary keys are assigned values using PostgreSQL sequences.
- **Unique fields:** `customer.email`, `staff.email`, `vehicle_category.category_name`, `vehicle.plate_number`, and `payment.payment_code` are unique when supplied.
- **One-to-one restrictions:** `rental_terms.rental_id`, `damage_fee.damage_id`, and `cancellation.rental_id` are unique, allowing at most one related record for each referenced rental or damage incident.
- **Foreign keys:** Foreign key constraints connect records across the tables and help prevent references to records that do not exist.

## 6. Validation and Integrity Constraints

| Table | Constraint / Validation |
|---|---|
| `customer` | First name, last name, email, phone number, password hash, and account status are required. Email must be unique. Account status defaults to `Active`. |
| `staff` | First name, last name, email, password hash, role, and account status are required. Email must be unique. Account status defaults to `Active`. |
| `vehicle_category` | Category name and category status are required. Category name must be unique. Category status defaults to `Active`. |
| `rental_rate` | Category, rental period type, and rate amount are required. Rate amount must be zero or greater. Rate status defaults to `Active`. |
| `vehicle` | Category, plate number, make, model, unit rate adjustment, and status are required. Plate number must be unique. Year must be at least 1900 when supplied; seating capacity must be greater than zero when supplied; mileage cannot be negative. |
| `driver_license` | Customer, license number, license type, issue date, expiration date, and both license photos are required. Expiration date must be later than issue date. Verification status defaults to `Pending`. |
| `rental` | Customer, vehicle, pickup and return date/time, and rental rate are required. Return date/time must be later than pickup date/time. Actual return date/time cannot be earlier than pickup date/time. Late fee, pickup fee, return fee, estimated cost, and final cost cannot be negative when supplied. |
| `rental_terms` | Rental, terms version, and terms text are required. If terms are accepted, an acceptance date/time must be provided. |
| `vehicle_inspection` | Rental, inspection type, and inspection date/time are required. Mileage cannot be negative when supplied. Exactly one performer—staff or customer—must be specified. |
| `inspection_photo` | Inspection, photo path, and upload date/time are required. Exactly one uploader—staff or customer—must be specified. |
| `damage_incident` | Inspection, description, report date/time, and status are required. Status defaults to `Open`. |
| `damage_fee` | Damage incident, amount, assessment date, and fee status are required. Amount must be zero or greater. Fee status defaults to `Pending Assessment`. |
| `payment` | Amount, payment date, and payment status are required. Amount must be greater than zero. Exactly one payment target—rental or damage fee—must be specified. Payment status defaults to `Pending`. |
| `vehicle_maintenance` | Vehicle, maintenance date, and status are required. Cost cannot be negative when supplied. Completion date cannot be earlier than maintenance date. Status defaults to `Pending`. |
| `audit_log` | Staff, action type, table name, and action date/time are required. Action date/time defaults to the current timestamp. |
| `cancellation` | Rental, cancellation reason, cancellation date/time, and refund eligibility are required. Exactly one cancellation actor—customer or staff—must be specified. Refund amount cannot be negative when supplied. Refund eligibility defaults to `false`. |

The schema uses `NOT NULL`, `UNIQUE`, `CHECK`, and `DEFAULT` constraints to help maintain valid and consistent data. Some values, such as account roles, rental statuses, payment methods, and verification statuses, are stored as character fields; the supplied schema does not define check constraints restricting all of these fields to a fixed list of values.

## 7. Relevant Database Objects and Defaults

| Object / Feature | Description |
|---|---|
| Primary and foreign key constraints | Enforce record identity and relationships between tables. |
| Check constraints | Validate dates, amounts, costs, mileage, seating capacity, and required actor/target combinations. |
| Unique constraints | Prevent duplicate values in designated fields and enforce selected one-to-one relationships. |
| PostgreSQL sequences | Generate integer identifier values for the primary keys. |
| Default timestamps | Several created, reported, uploaded, assessed, and action timestamps default to `CURRENT_TIMESTAMP`. |
| Default statuses | Initial statuses are provided for customer accounts, staff accounts, vehicle categories, rental rates, vehicles, rentals, driver's license verification, damage incidents, damage fees, payments, and maintenance records. |
| Rental and payment indexes | Indexes support searches by rental customer, vehicle, dates, and status, and by payment status. |
| License and maintenance indexes | Indexes support searches by license customer/status and maintenance vehicle. |
| Damage-fee index | An index supports searching damage fees by fee status. |
| Audit indexes | Indexes support searching audit records by action date/time and staff member. |

The supplied schema defines tables, constraints, sequences, and indexes. It does not define views, triggers, stored procedures, or user-defined functions.

## 8. Database Setup and Migration Process

The database is initialized using the project's schema SQL file. The schema defines the tables and database constraints required by the Car Rental Management System.

| Component | File / Command | Function |
|---|---|---|
| Database schema | `Database/schema.sql` | Defines the database tables, relationships, constraints, and indexes. |
| Sample data | `Database/seed.sql` | Contains sample records used for testing and demonstration, if provided in the project. |
| Database management system | PostgreSQL | Stores and manages the system's relational data. |

The schema can be executed through a PostgreSQL database tool such as pgAdmin's Query Tool or through the PostgreSQL command-line utility. The database must be created and the schema must be applied before the application can use the tables.

## 9. Summary

The Car Rental Management System database uses PostgreSQL and contains 16 related tables that support customer accounts, driver's license verification, vehicle management, rental transactions, payments, inspections, damage records, maintenance, cancellations, and staff activities. Primary keys, foreign keys, unique constraints, check constraints, defaults, and indexes help organize the records and maintain data integrity. The database provides the relational structure needed to support the system's main car rental processes.
