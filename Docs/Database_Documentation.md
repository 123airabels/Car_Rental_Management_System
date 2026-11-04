# 1. Database Overview
The Car Rental Management System uses PostgreSQL as its relational database management system. The database stores information related to customers, driver's license verification, vehicles, rental rates, reservations, rental transactions, payments, inspections, damage incidents, maintenance, cancellations, and staff activities.
| Item | Value |
|---|---|
| Database name | car_rental_db |
| Schema | public |
| PostgreSQL version | 18.6 |
| Database type | Relational database |
| Schema file | Database/schema.sql |
| Sample data file | Database/seed.sql |

# 2. Database Tables
| Table | Purpose |
|---|---|
| customer | Stores customer profile and account information. |
| driver_license | Stores driver's license details and verification status. |
| staff | Stores staff account information and application role. |
| vehicle_category | Stores vehicle categories, such as Sedan, SUV, Van, and Pickup. |
| rental_rate Stores base rental rates for vehicle categories and rental periods. |
| vehicle | Stores vehicle details, plate numbers, and operational status. |
| rental | Stores rental requests, schedules, costs, and rental status. |
| rental_terms | Stores the rental terms presented to and acknowledged by a customer. |
| vehicle_inspection | Records vehicle condition before and after a rental. |
| inspection_photo | Stores references to photos associated with an inspection. |
| damage_incident | Records damage or incidents identified during inspections. |
| damage_fee | Stores assessed damage charges and their status. |
| payment | Records rental or damage-fee payments and receipt references. |
| vehicle_maintenance | Records maintenance activities for vehicles. |
| audit_log | Records important actions performed by authorized staff. |
| cancellation | Records cancellations and related refund information. |

# 3. Relationships and Keys
The database uses primary keys to uniquely identify records and foreign keys to connect related records.
Examples of important relationships include:
- One customer can have multiple rental records.
- One vehicle category can contain multiple vehicles.
- One vehicle category can have multiple rental rates.
- One vehicle can be associated with multiple rental records over time.
- One rental can have related terms, inspections, payments, and cancellation information.
- One inspection can have multiple photos and damage incidents.
- A damage incident can have a related damage-fee record.
- One vehicle can have multiple maintenance records.
- Staff records can be associated with license verification, audit logs, and inspections.
The rental table connects customers and vehicles through individual rental transactions. Foreign keys help prevent records from referencing related records that do not exist.

# 4. Constraints and Integrity Rules
The schema uses or plans to use the following database constraints:
| Constraints | Use |
|---|---|
| PRIMARY KEY | Uniquely identifies records. | 
| FOREIGN KEY | Maintains valid relationships between tables. |
| NOT NULL | Requires essential information. |
| UNIQUE | Prevents duplicate values in fields such as email addresses and vehicle plate numbers. | 
| CHECK | Enforces valid values, such as nonnegative fees and a return date/time later than the pickup date/time. |
| DEFAULT | Supplies common initial values, such as timestamps and default statuses. |

