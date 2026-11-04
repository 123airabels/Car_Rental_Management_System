# ByaheRentals

### Car Rental Management System

# PROJECT OVERVIEW
The Car Rental Management System is a database-driven application designed to organize the main operations of a self-drive car rental business. It aims to manage customer information, driver's license verification, vehicle records, rental rates, reservations, payments, vehicle inspections, damage records, maintenance, cancellations, and rental history using PostgreSQL.

The proposed system allows customers to register, browse available vehicles, submit rental requests, and view their booking and payment information. Rental staff can manage vehicle records, verify driver's licenses, review rental requests, record vehicle inspections, process returns, and manage rental-related transactions.

The application uses Java and Spring Boot for the backend and PostgreSQL for data storage. The project is being developed with a focus on relational database design, data integrity, and secure access to records.

# PROJECT OBJECTIVES
Organize customer, vehicle, rental, and payment records in a centralized database.
Maintain relationships and data integrity through primary keys, foreign keys, and other constraints.
Support the development of core car rental management operations.
Provide a foundation for database administration activities, including access control, monitoring, backup, and recovery.

# PREREQUISITES
Before running the application, make sure you have the following installed and configured:
- Java JDK 17 — required to run the Spring Boot application.
- PostgreSQL — the database server must be running.
- Car Rental Database — the `car_rental_db` database must exist, and the required tables must already be created.
- Project Files — make sure you have downloaded or cloned the project repository.


# HOW TO SET UP AND RUN THE CAR RENTAL MANAGEMENT SYSTEM

Follow these steps to set up and run the Car Rental Management System on a local Windows computer. The project consists of three main parts: the PostgreSQL database, the Spring Boot backend application, and the frontend website.

## Step 1: Open the Project Folder

1. Open Windows PowerShell.
2. Navigate to the project folder:

   ```powershell
   cd C:\Users\Aira\OneDrive\Desktop\Car_Rental_Management_System
   ```

3. Check the project folders:

   ```powershell
   dir
   ```

   The project should contain the following folders:

   - `Database`
   - `Application`
   - `Frontend`

## Step 2: Set Up the PostgreSQL Database

1. Make sure the PostgreSQL service is running.
2. Confirm that PostgreSQL is using port `5432`.
3. Navigate to the `Database` folder:

   ```powershell
   cd Database
   ```

4. Check the available database files:

   ```powershell
   dir
   ```

5. If the `car_rental_db` database has not been created yet, create it:

   ```powershell
   psql -U postgres -h localhost -p 5432 -c "CREATE DATABASE car_rental_db;"
   ```
   
   or
   
    ```powershell
   & "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h localhost -p 5432 -c "CREATE DATABASE car_rental_db;"
   ```
    
7. Run the database schema to create the required tables:

   ```powershell
   psql -U postgres -h localhost -p 5432 -d car_rental_db -f schema.sql
   ```

   or

    ```powershell
   & "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h localhost -p 5432 -d car_rental_db -f schema.sql
   ```

9. Insert the sample data:

   ```powershell
   psql -U postgres -h localhost -p 5432 -d car_rental_db -f seed.sql
   ```

   or

   ```powershell
   & "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h localhost -p 5432 -d car_rental_db -f seed.sql
   ```

11. Verify that the database and its tables were created successfully.

**Reminder:** If the database and tables already exist, do not run the schema and seed files again unless you are intentionally setting up or resetting the database. Do not run `reset_seed.sql` unless you intend to reset the sample data.

## Step 3: Configure and Start the Backend Application

1. Open a new PowerShell window.
2. Navigate to the `Application` folder:

   ```powershell
   cd C:\Users\Aira\OneDrive\Desktop\Car_Rental_Management_System\Application
   ```

3. Set the PostgreSQL password using the environment variable:

   ```powershell
   $env:DB_PASSWORD = "your_actual_postgres_password"
   ```

   Replace `your_actual_postgres_password` with the password used for your local PostgreSQL account.

4. Start the Spring Boot application:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

5. Wait for the application to finish starting. Check the terminal for messages indicating that Spring Boot and the embedded Tomcat server have started successfully.

6. Keep this terminal open while using the application. Closing the terminal or stopping the process will stop the backend.

The backend application uses port `8080` by default.

**Important:** If the terminal reports that port `8080` is already in use, the backend may already be running. Do not start another instance. Use the existing running application or stop it before restarting.

## Step 4: Start the Frontend Website

1. Open another new PowerShell window.
2. Navigate to the `Frontend` folder:

   ```powershell
   cd C:\Users\Aira\OneDrive\Desktop\Car_Rental_Management_System\Frontend
   ```

3. Start the local frontend server:

   ```powershell
   python -m http.server 5500
   ```

4. Keep this terminal open while using the website.

The frontend website uses port `5500` by default.

## Step 5: Open and Test the Application

Open a web browser and visit:

- Frontend website: http://localhost:5500
- Backend application: http://localhost:8080

Use the frontend website to test the system. Confirm that the frontend can communicate with the backend and that the backend can connect to the PostgreSQL database.

Test the main workflow, including:

1. Customer registration and login.
2. Viewing available vehicles.
3. Submitting a reservation.
4. Reviewing and approving or rejecting a reservation.
5. Submitting and verifying payment.
6. Recording vehicle pickup and return.

## Step 6: Stop the Application

When you finish testing:

1. Return to the terminal running the frontend server.
2. Press `Ctrl + C` to stop the frontend server.
3. Return to the terminal running Spring Boot.
4. Press `Ctrl + C` to stop the backend application.

Stopping the backend or frontend does not automatically stop the PostgreSQL service.

## Additional Reminders

- PostgreSQL is configured to use `localhost:5432`.
- The database name is `car_rental_db`.
- The backend reads the PostgreSQL password from the `DB_PASSWORD` environment variable.
- The application uses `spring.jpa.hibernate.ddl-auto=none`. Hibernate will not automatically create missing database tables.
- Each developer must configure their own local PostgreSQL password.
- If the application fails to start, refer to `docs/troubleshooting.md` for common errors and possible solutions.


WEB FRONT END AND API (ADDED)
The `Frontend` folder holds the Byahe Rentals website (HTML, Tailwind, JavaScript). The Spring Boot app in `Application` now exposes the REST API it uses, reading and writing the existing tables through JDBC (the database schema is unchanged).

To run both:
1. Start PostgreSQL with `car_rental_db` created from Database/schema.sql, then Database/seed.sql (vehicle types, rates, sample vehicles, sample staff/customer accounts, sample license).
   To wipe old data and reload the seed later: psql -U postgres -d car_rental_db -f Database/reset_seed.sql (run from inside the Database folder).
   Sample accounts use the password Sample@12345 (development only).
2. In `Application`, set DB_PASSWORD (and optionally JWT_SECRET) and run `mvnw.cmd spring-boot:run`.
3. In `Frontend`, run `python -m http.server 5500` and open http://localhost:5500.

Staff login (created automatically on first run if the staff table is empty): admin@byahe.test / ChangeMe123!  Change this password. Customers register on the site.

CUSTOMER MANAGEMENT (ADDED)
Registration (with email confirmation), customer profile, contact information, account status and rental history.
Pages: Frontend/profile.html (a customer own profile + history), Frontend/customers.html (staff: search the list) and Frontend/customer.html (staff: one customer - edit contact details, change status, rental history).
GET  /api/account/profile                  logged-in customer: own profile
PUT  /api/account/profile                  logged-in customer: edit name, phone, address (email is the login and is not editable)
GET  /api/admin/customers?q=               Staff, Admin, System Admin: list and search
GET  /api/admin/customers/{id}             Staff, Admin, System Admin: profile, license status, rental totals
PUT  /api/admin/customers/{id}             Staff, Admin, System Admin: edit contact details (audit-logged)
GET  /api/admin/customers/{id}/rentals     Staff, Admin, System Admin: full rental history
POST /api/admin/customers/{id}/status      Admin, System Admin only: Active / Suspended
Finance no longer has access to /api/admin/customers/**.

VEHICLE MANAGEMENT (ADDED)
Pages: Frontend/admin.html (fleet list with search, status filter and counts), Frontend/vehicle.html (add, view, edit, delete one vehicle, plus its rental and maintenance history).
Fields: plate number, make, model, year, color, mileage, fuel type, seating capacity, status (Available / Maintenance / Retired; Rented is set by the rental flow), type and rate adjustment.
GET    /api/admin/vehicles                 all staff roles: list with every field
GET    /api/admin/vehicles/{id}            all staff roles: one vehicle
GET    /api/admin/vehicles/{id}/rentals    Staff, Admin, System Admin: rental history of one vehicle
POST   /api/admin/vehicles                 Admin, System Admin: add
PUT    /api/admin/vehicles/{id}            Admin, System Admin: edit
DELETE /api/admin/vehicles/{id}            Admin, System Admin: delete. Refused if the vehicle is rented or has rental/maintenance history; set it to Retired instead (kept in the records, hidden from customers).

CAR DETAILS (ADDED)
Clicking a car on vehicles.html opens its own page, Frontend/car.html: type, year, seats, fuel, color, daily rate, what to know before booking, and a booking box (dates, availability, estimated total, Reserve). Plate number and mileage are not shown to customers.
GET  /api/vehicles/{id}?start=&end=        public: customer-facing details of one car

API summary
POST /api/auth/register, /api/auth/login   public
GET  /api/vehicles?category=&start=&end=    public
POST /api/rentals                           logged-in customer
GET  /api/rentals/me                        logged-in customer
POST /api/rentals/{id}/cancel               owner (Pending/Approved only)
GET  /api/admin/vehicles                    staff/admin



FULL BACKEND (ADDED)
All 16 tables now have API endpoints. New settings are at the bottom of application.properties (upload folder, late-fee rule).
Photos (licenses, inspections) are saved in the folder set by app.upload-dir (default: an "uploads" folder next to where you start the app). Back it up with the database.
Roles (staff.role): Staff, Finance, Admin, System Admin. Customers have their own table and the role CUSTOMER.
Send the token as:  Authorization: Bearer <token>   (from POST /api/auth/login)

Everyone logged in
GET  /api/account/me                      POST /api/account/password {currentPassword,newPassword}

Customer
POST /api/license (multipart: licenseNumber, licenseType, issueDate, expirationDate, front, back)
GET  /api/license/me                      GET /api/license/{id}/photo/{front|back}
GET|POST /api/rentals/{id}/terms[/accept] GET /api/rentals/{id}/balance
POST /api/payments {rentalId | damageFeeId, amount, method, referenceNumber}     GET /api/payments/me
GET  /api/rentals/{id}/inspections        POST /api/inspections/{id}/notes {notes}
POST /api/inspections/{id}/photos (multipart: photo, description)    GET /api/inspection-photos/{photoId}
GET  /api/damage-fees/me                  POST /api/damage-fees/{id}/dispute {reason}
GET  /api/categories (public)

Staff, Admin, System Admin (Finance can read)
GET  /api/admin/rentals?status=           GET /api/admin/rentals/{id}
POST /api/admin/rentals/{id}/approve | reject {reason} | cancel {reason, refundAmount}
POST /api/admin/rentals/{id}/pickup {mileage, fuelLevel, conditionNotes, pickupFee}
POST /api/admin/rentals/{id}/return {mileage, fuelLevel, conditionNotes, returnFee}   (works out the late fee)
GET  /api/admin/licenses?status=Pending   POST /api/admin/licenses/{id}/verify | reject {reason}
POST /api/admin/inspections/{id}/damages {damageType, description, locationOnVehicle, severity}
PUT  /api/admin/damages/{id}/fee {amount} POST /api/admin/damages/{id}/resolve
GET  /api/admin/damages | /api/admin/damage-fees
GET  /api/admin/maintenance               POST /api/admin/maintenance, /{id}/start, /{id}/complete, /{id}/cancel
GET  /api/admin/customers?q=              POST /api/admin/customers/{id}/status {status: Active|Suspended}

Finance, Admin, System Admin
GET  /api/admin/payments?status=          POST /api/admin/payments (counter payment, marked Paid)
POST /api/admin/payments/{id}/confirm | reject | refund
GET  /api/admin/reports/summary

Admin, System Admin
GET|POST /api/admin/staff                 POST /api/admin/staff/{id}/status {status: Active|Inactive}
POST /api/admin/categories, /api/admin/rates, /api/admin/vehicles      PUT /api/admin/vehicles/{id}
GET  /api/admin/audit?limit=

Notes
- Rental flow: Pending -> Approved (needs a verified license valid through the return date) -> Active (pickup) -> Completed (return). Or Rejected / Cancelled.
- Payments are recorded by the customer as Pending and checked by Finance; there is no live payment gateway. Emails/SMS are only written to the log (see support/LoggingNotificationService).
- Every staff action is written to audit_log. Customer actions are not, because audit_log.staff_id is required.
- The front end does not have pages for the new endpoints yet.


EMAIL CONFIRMATION (BREVO API) AND SECURITY (ADDED)
New customers are created with account_status = 'Pending' and cannot log in until they open the confirmation link emailed to them (valid for 24 hours). The email is sent once, when the account is created, in the background (sign-up does not wait for it). Existing customers and all staff are not affected, and the database schema is unchanged.

Set up the email API (one time, free: 300 emails/day, no domain needed)
1. Create a free account at brevo.com.
2. Add and verify a sender: Settings > Senders, domains & dedicated IPs > Senders > Add a sender. Use an email you can open (for example your Gmail); Brevo emails you a code to verify it.
3. Create an API key: SMTP & API > API Keys > Generate a new API key. Copy it (it is shown once).
4. In the terminal where you start the backend (PowerShell), before running mvnw:
     $env:DB_PASSWORD   = "your_postgres_password"
     $env:JWT_SECRET    = "any-random-text-at-least-32-characters-long"
     $env:BREVO_API_KEY = "the_api_key_from_brevo"
     $env:MAIL_FROM     = "the_sender_email_you_verified"
     .\mvnw.cmd spring-boot:run
5. The link in the email points to http://127.0.0.1:5500/Frontend (VS Code Live Server opened on the project root). If you serve the Frontend folder directly (python -m http.server 5500), set $env:FRONTEND_URL = "http://localhost:5500".
6. When the backend starts it prints one "MAIL:" line:
     MAIL: Brevo API key is valid ...        -> emails will be sent
     MAIL: BREVO_API_KEY and/or MAIL_FROM is not set ... -> nothing is sent; the link is printed in the terminal
     MAIL: Brevo rejected the API key ...    -> wrong key
   If sending fails when someone signs up, the confirmation link is also printed so you can still open it (local testing only; remove that log line before real deployment).
Mail sent from a free address such as gmail.com may land in Spam. For real use, verify your own domain in Brevo.
Never commit BREVO_API_KEY or JWT_SECRET to GitHub.

New endpoints (public)
POST /api/auth/register            now returns {message}; no token until the email is confirmed
POST /api/auth/verify-email {token}
Front end: verify.html (the page the email link opens).

Security changes
- Login lockout: 5 wrong passwords for one email (or 30 from one IP) in 15 minutes returns HTTP 429. Sign-up (counted per account created) and change-password are rate limited too (in memory, reset on restart).
- Password rule: 8 to 72 characters with at least one letter and one number (sign-up and change password).
- JWT secret: the public default was removed. With no JWT_SECRET a random one is generated on each start (everyone is logged out on restart). A secret shorter than 32 characters stops the app.
- Wrong email and wrong password give the same message and take similar time; the unknown-email case does a dummy password check so emails cannot be probed by timing.
- Confirmation tokens are signed and expire; they carry no role, so they can never be used as a login token. An old link cannot re-open a suspended account.
- API response headers: Content-Security-Policy (default-src 'none'), no-referrer, plus Spring's defaults (nosniff, frame deny, no-cache).
- Front end: text from the server is escaped before it is put on the page (stops script injection). Finance and System Admin can now open the staff page.
- Still to do before a real deployment: serve over HTTPS, move the token from localStorage to an HttpOnly cookie, and change the seeded admin password.


The confirmation email is sent once, when the account is created. There is no resend. If it fails or the person loses it, staff can activate the account: POST /api/admin/customers/{id}/status {status: Active}.


VEHICLE AVAILABILITY AND BOOKING MANAGEMENT (ADDED)
Pages
- Frontend/car.html        customer: pick dates, see "available / not available (and why)", list of dates already taken, reserve.
- Frontend/dashboard.html  customer: My rentals, with View / change and Cancel.
- Frontend/booking.html    customer: one booking - view status, change dates (live availability check), cancel.
- Frontend/bookings.html   staff: all bookings with search and status filter; New booking (for a customer), Edit, Approve, Reject, Cancel.
                           Finance can view but not change bookings.

The rules live in one place (support/BookingService.java) and are used by every endpoint below:
- A booking holds the car while it is Pending, Approved or Active. Rejected, Cancelled and Completed bookings free it.
- Dates are half-open: a car returned on the 10th can be picked up by someone else on the 10th.
- A car is also unavailable if it is Retired, in maintenance, or has Pending maintenance scheduled inside the dates.
- A car that is out on rent CAN be booked for dates after its current rental is due back.
- The vehicle row is locked while a booking is created, changed or approved, so two people can't both get the same car.
- Approving a booking re-checks all of the above (the car may have gone into maintenance since the request was made).

Customer endpoints (logged-in customer, own bookings only)
POST /api/rentals {vehicleId,start,end}              create (starts Pending)
GET  /api/rentals/me                                 list
GET  /api/rentals/{id}                               one booking (includes the reason if rejected/cancelled)
GET  /api/rentals/{id}/availability?start=&end=      would these new dates work? (the booking's own dates don't count as a clash)
PUT  /api/rentals/{id} {start,end}                   change dates (Pending/Approved). An Approved booking goes back to Pending.
POST /api/rentals/{id}/cancel                        cancel (Pending/Approved)

Staff endpoints (Staff, Admin, System Admin; Finance can read)
GET  /api/admin/rentals?status=&vehicleId=&q=        list and search (q = customer name, email or plate)
GET  /api/admin/rentals/{id}                         one booking
GET  /api/admin/rentals/{id}/availability?vehicleId=&start=&end=
POST /api/admin/rentals {customerId,vehicleId,start,end,approve}   book for a customer; approve=true approves at once
PUT  /api/admin/rentals/{id} {vehicleId,start,end}   change car and/or dates (Pending/Approved; Approved stays Approved if the license still covers it)
POST /api/admin/rentals/{id}/approve | reject {reason} | cancel {reason, refundAmount}   (unchanged, approve now re-checks conflicts)

Public
GET  /api/vehicles?category=&start=&end=             with dates, hides cars booked, in maintenance or due for maintenance in that window
GET  /api/vehicles/{id}?start=&end=                  availableForDates true/false
GET  /api/vehicles/{id}/availability?start=&end=     {available, reason, booked:[{start,end,kind}]} - taken dates only, no customer details

DATABASE SAFETY NET (OPTIONAL BUT RECOMMENDED)
Database/booking_conflict_guard.sql adds a PostgreSQL exclusion constraint so the database itself refuses two active bookings for one car on
overlapping dates. Run it once after schema.sql (needs a superuser for CREATE EXTENSION btree_gist):
  psql -U postgres -d car_rental_db -f booking_conflict_guard.sql
It first lists any existing clashes; there must be none before the constraint can be added. If it ever fires, the API answers 409 with a friendly message.
reset_seed.sql keeps working with the constraint in place.

Other changes in this version
- Customers who are Suspended can no longer create or change bookings (previously only the login was blocked).
- The "My rentals" link now shows for customers only; staff use Bookings.
- A car whose status is Rented now shows a Reserve button when dates are chosen (it used to be blocked outright).


RENTAL TRANSACTIONS AND TERMS (ADDED)
Each rental now has one complete transaction record: booking dates, scheduled and actual pickup and return, status, estimated and final cost,
the terms the customer accepted, and cancellation (or rejection) details. Nothing new is stored in the database (the schema is unchanged):
the record is assembled from rental, vehicle_inspection, rental_terms, cancellation, payment and the rate tables.

Terms and conditions
- A customer must tick "I accept the rental terms" when reserving (car.html and the Reserve window on vehicles.html).
  POST /api/rentals now needs "acceptTerms": true and stores the version, the exact wording and the time in rental_terms.
- A booking made by staff on a customer's behalf has no acceptance yet. The customer sees a banner on booking.html and accepts there
  (POST /api/rentals/{id}/terms/accept). Staff can see "Not yet accepted" on the record.
- The wording is sample text in TermsController (VERSION, TEXT). Replace it with the real terms and bump VERSION; old rentals keep the text they accepted.
GET /api/terms                                public: the current terms

Transaction record
GET /api/rentals/{id}/record                  customer: own rental (plate number hidden)
GET /api/admin/rentals/{id}/record            Staff, Finance, Admin, System Admin: any rental
Contains: booked / approved times, customer, vehicle, dates and length, daily rate, estimated cost, pickup/return fees, late fee, final cost,
amount paid, pickup and return (scheduled vs actual time, mileage, fuel, condition notes), terms accepted (version, time, text),
and cancellation (Customer or staff name, reason, time, refund eligibility, amount and status). Rejections show here too.
Pages: Frontend/booking.html (customer: record at the bottom), Frontend/transaction.html (staff: from the "Record" link on bookings.html, printable).

VEHICLE INSPECTION AND DAMAGE MANAGEMENT (ADDED)
Pages:
  Frontend/inspection.html?id=<rental id>  ONE page per rental showing Before rental (pickup) and After rental (return) side by side:
      mileage, fuel level, condition notes, photos, customer notes, distance driven, fuel change, and any damage found with its fee,
      dispute and decision. Staff record the pickup/return inspection here (this hands the car over / takes it back), upload photos,
      report damage and set fees. Customers add photos and notes, dispute a fee, and pay it. Linked from My rentals and Bookings.
  Frontend/damages.html                    Staff: all damage fees (filter by status), damage still waiting for a fee, and the form to decide a dispute.
Database: run Database/damage_dispute_resolution.sql once on an existing database (adds damage_fee.resolution_notes; schema.sql already has it).
New / changed endpoints:
  POST /api/admin/damage-fees/{id}/resolve-dispute   Staff, Admin, System Admin: decision UPHOLD / ADJUST (new lower amount) / WAIVE, with notes the customer can read
  Fee flow: Assessed -> Disputed (customer, once per fee) -> Assessed or Waived (staff decision) -> Paid.
  A fee under dispute can't be edited; a damage can't be marked resolved until its fee is paid or waived.
  GET /api/rentals/{id}/inspections and GET /api/admin/damage-fees now also return disputeReason / resolutionNotes (and feePaid).

Customer side: Frontend/my-damages.html ("Damage fees" in the customer menu) lists the customer's own damage fees with status, dispute reason, staff decision, a Pay form, a Dispute button and a link to the inspection photos.
The app now adds damage_fee.resolution_notes itself at startup, so the manual migration is only a fallback.

RENTAL HISTORY, AUDIT LOGS AND ADMIN DASHBOARD (ADDED)
Pages:
  Frontend/overview.html       Staff dashboard (staff log in here now). Needs-attention counters (approvals, pickups, returns due/overdue, licenses, payments to
                               confirm, disputed fees, maintenance), money figures and 6-month revenue chart (Finance/Admin only), bookings per month, fleet and
                               rental status charts, cars out on rent, and recent admin activity (Admin only).
  Frontend/transactions.html   Transaction history for all staff roles: Rentals tab, plus Payments tab (payments, refunds, damage-fee payments) for Finance/Admin.
                               Search, status and date filters, totals, and Download CSV.
  Frontend/audit.html          Admin / System Admin: audit log with filters (action, record type, staff, date range, text), links to the record, load more, CSV.
Endpoints:
  GET /api/admin/dashboard     all staff roles (money only for Finance/Admin, activity only for Admin)
  GET /api/admin/audit?limit=&offset=&action=&table=&staffId=&q=&from=&to=     Admin, System Admin
  GET /api/admin/audit/options                                                  Admin, System Admin
Audit log now also records vehicle status changes made by handing over (Available -> Rented) and returning (Rented -> Available) a car.
Already logged before: booking create/edit/approve/reject/cancel, pickup, return, payment record/confirm/reject/refund, license verify/reject, vehicle add/edit/delete,
maintenance, damage reports, fees and dispute decisions, staff and customer account changes. Customer self-service actions are not logged because audit_log requires a staff id.

PAYMENT CODES AND RECEIPTS
Every payment gets a unique payment code (for example PAY-7K3QX9) when it is created. The customer sees it on the Payments page right after paying and keeps it in their payment history. They give the code to the staff.
Finance/Admin staff open Transactions > Payments, type the code into "Customer paying? Enter their payment code", check the details, and press "Confirm payment & issue receipt". The receipt (receipt number, payment code, amount, who received it) opens for printing and is also available to the customer.
Database: run Database/payment_codes.sql once on an existing database (adds payment.payment_code and payment.received_by; schema.sql already has them).

DRIVER'S LICENSE UPLOAD AND REVIEW (FRONT END)
The license endpoints were already in the API (see LicenseController); these pages use them.
Customer:  Frontend/profile.html, "Driver's license" card.
  - Upload form: license number, type, issue and expiration dates, photo of the front and back (JPG or PNG, 5 MB each, with preview).
  - Shows the current status (Pending / Verified / Rejected), the details and the two photos. A rejection reason is shown to the customer.
  - The form appears only when the server would accept a new submission: no license yet, the last one was rejected, or the verified one has expired.
Staff (Staff, Admin, System Admin; Finance can't review):  Frontend/licenses.html, "Licenses" in the menu.
  - Tabs: Pending / Verified / Rejected / All, with counts. ?status=PENDING and ?customerId=N are supported (the dashboard tile and the customer page link here).
  - Review dialog: details plus front and back photos (click a photo to open it full size), Verify, or Reject with a required reason (quick-pick reasons provided).
  - A pending license whose expiration date has passed can only be rejected.
  - Verify and reject are written to the audit log and the customer is notified (existing backend behaviour).
Photos are protected: the browser fetches them with the log-in token (API.blobUrl in js/api.js), so they can't be opened by anyone without access.
js/api.js now also sends FormData (API.upload) for file uploads.
Note: the sample license in seed.sql points at uploads/sample/..., which doesn't exist on disk, so its photos show "Photo not available".
Put real images at <upload-dir>/uploads/sample/license_front.jpg and license_back.jpg if you want the sample to display them.
