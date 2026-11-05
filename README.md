# ByaheRentals
### Car Rental Management System

# PROJECT OVERVIEW

The Car Rental Management System is a database-driven application designed to organize the main operations of a self-drive car rental business. It aims to manage customer information, driver's license verification, vehicle records, rental rates, reservations, payments, vehicle inspections, damage records, maintenance, cancellations, and rental history using PostgreSQL.

The proposed system allows customers to register, browse available vehicles, submit rental requests, and view their booking and payment information. Rental staff can manage vehicle records, verify driver's licenses, review rental requests, record vehicle inspections, process returns, and manage rental-related transactions.

The application uses Java and Spring Boot for the backend and PostgreSQL for data storage. The project is being developed with a focus on relational database design, data integrity, and secure access to records.

# PROJECT OBJECTIVES

- Organize customer, vehicle, rental, and payment records in a centralized database.
- Maintain relationships and data integrity through primary keys, foreign keys, and other constraints.
- Support the development of core car rental management operations.
- Provide a foundation for database administration activities, including access control, monitoring, backup, and recovery.

# PREREQUISITES

Before running the application, make sure you have the following installed and configured:

- **Java JDK 17** – required to run the Spring Boot application.
- **PostgreSQL 18** – the local database server.
- **pgAdmin 4** – used to manage the PostgreSQL database and roles.
- **Git or GitHub Desktop** – used to download or clone the project repository.
- **Python** – required only if the frontend is started using a separate local server.
- **Car Rental Database** – the `car_rental_db` database must be created and configured before running the application.

# HOW TO SET UP AND RUN THE CAR RENTAL MANAGEMENT SYSTEM

Follow these instructions to set up and run the Car Rental Management System on a local Windows computer.

Each student must perform the database setup on their own computer. The project uses a local PostgreSQL database, which means changes made to one student's local database will not automatically appear in the databases of other group members.

The project consists of three main parts:

1. PostgreSQL database
2. Spring Boot backend application
3. Frontend website

---

# STEP 1: CLONE THE PROJECT REPOSITORY

Download a local copy of the group's latest GitHub repository using either of the following methods.

### Option A: Using GitHub Desktop

1. Open GitHub Desktop and sign in to your GitHub account.
2. Click **File > Clone Repository**.
3. Select the **URL** tab.
4. Copy and paste the GitHub repository URL.
5. Choose the local folder where you want to save the project.
6. Click **Clone**.
7. Once cloning is complete, open the project folder in Visual Studio Code or File Explorer.

### Option B: Using Git Bash

Open Git Bash and navigate to the location where you want to save the project:

```bash
cd ~/OneDrive/Desktop
```

Clone the repository:

```bash
git clone YOUR_GITHUB_REPOSITORY_URL
```

Navigate to the cloned project folder:

```bash
cd Car_Rental_Management_System
```

Check the project folders:

```bash
ls
```

The project should contain the following folders:

```text
Car_Rental_Management_System/
├── Application/
├── Database/
├── Docs/
├── Frontend/
└── README.md
```

---

# STEP 2: OPEN THE PROJECT FOLDER

Open Windows PowerShell.

Navigate to the cloned project folder. Replace `YOUR_USERNAME` with your Windows account folder name.

```powershell
cd "C:\Users\YOUR_USERNAME\OneDrive\Desktop\Car_Rental_Management_System"
```

Check the project folders:

```powershell
dir
```

Make sure that the `Database`, `Application`, and `Frontend` folders are present.

**Important:** The folder path may be different depending on where you saved the project.

---

# STEP 3: SET UP THE POSTGRESQL DATABASE

Each student must configure their own local PostgreSQL database.

The PostgreSQL administrator account (`postgres`) will be used for initial database setup, schema creation, migration execution, and database role configuration.

The application itself will use a separate restricted account named `app_runtime`.

### 3.1 Start PostgreSQL

1. Open Windows Services.
2. Find the PostgreSQL service.
3. Make sure its status is **Running**.
4. Confirm that PostgreSQL is configured to use port `5432`.

You can also check the PostgreSQL connection using PowerShell:

```powershell
psql --version
```

If `psql` is not recognized, use its full executable path:

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" --version
```

### 3.2 Navigate to the Database Folder

In PowerShell:

```powershell
cd "C:\Users\YOUR_USERNAME\OneDrive\Desktop\Car_Rental_Management_System\Database"
```

Check the available database files:

```powershell
dir
```

Make sure that the required SQL files are present, including:

- `schema.sql`
- `seed.sql`
- `db_roles_and_grants.sql`
- `payment_proof.sql`
- `payment_codes.sql`
- `damage_dispute_resolution.sql`
- `inspection_checklist.sql`

### 3.3 Create the Database

If the `car_rental_db` database has not been created yet, execute:

```powershell
psql -U postgres -h localhost -p 5432 -c "CREATE DATABASE car_rental_db;"
```

If `psql` is not recognized, use:

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h localhost -p 5432 -c "CREATE DATABASE car_rental_db;"
```

Enter your own PostgreSQL administrator password when prompted.

**Note:** Do not create another database if `car_rental_db` already exists.

### 3.4 Create the Database Tables

Run the schema file to create the required tables, relationships, constraints, and sequences:

```powershell
psql -U postgres -h localhost -p 5432 -d car_rental_db -f schema.sql
```

This must be executed using the PostgreSQL administrator account or another authorized database owner.

### 3.5 Insert the Sample Data

After successfully creating the database structure, insert the sample records:

```powershell
psql -U postgres -h localhost -p 5432 -d car_rental_db -f seed.sql
```

The sample data provides initial records that can be used when testing the application.

### 3.6 Apply the Required Database Migrations

Some additional database columns and tables are maintained through separate SQL migration files.

These must be executed before starting the Spring Boot application because the application runtime account does not have permission to modify the database structure.

Execute the following files in order:

**1. Payment proof**

```powershell
psql -U postgres -h localhost -p 5432 -d car_rental_db -f payment_proof.sql
```

**2. Payment codes**

```powershell
psql -U postgres -h localhost -p 5432 -d car_rental_db -f payment_codes.sql
```

**3. Damage dispute resolution**

```powershell
psql -U postgres -h localhost -p 5432 -d car_rental_db -f damage_dispute_resolution.sql
```

**4. Inspection checklist**

```powershell
psql -U postgres -h localhost -p 5432 -d car_rental_db -f inspection_checklist.sql
```

Wait for each script to finish successfully before executing the next one.

These scripts add the required database structures used by the application.

### Important Database Setup Reminders

- Run `schema.sql` only when creating a new database.
- Run `seed.sql` only when initial sample data is needed.
- Do not run `reset_seed.sql` unless you intentionally want to reset the sample data.
- Run the migration scripts as the database owner or an authorized administrator.
- Do not execute the initial schema and seed scripts repeatedly on an existing database.

---

# STEP 4: CONFIGURE DATABASE ROLES AND PERMISSIONS

The project uses separate PostgreSQL accounts for database administration and application access.

This follows the principle of least privilege, which means that the application receives only the permissions required for its normal operations.

### 4.1 Understand the Database Accounts

There are two main types of accounts involved in the setup.

**A. PostgreSQL Administrator Account (`postgres`)**

The `postgres` account is the database administrator account used during the initial setup.

It is responsible for:

- Creating the `car_rental_db` database.
- Creating the database tables and relationships.
- Running database migration scripts.
- Creating and configuring database roles.
- Managing database permissions.

The `postgres` account should not be used by the Spring Boot application.

**B. Application Runtime Account (`app_runtime`)**

The `app_runtime` account is a separate PostgreSQL login used by the Spring Boot application.

It is responsible for:

- Reading application records.
- Inserting new records.
- Updating existing records.
- Performing permitted vehicle deletion operations.

It has restricted database permissions and does not have superuser privileges or permission to create permanent database objects.

### 4.2 Configure the PostgreSQL Group Roles

After creating the database, tables, sample data, and required migrations, configure the database roles.

Make sure you are inside the `Database` folder in PowerShell.

Execute:

```powershell
psql -U postgres -h localhost -p 5432 -d car_rental_db -f db_roles_and_grants.sql
```

This script creates and configures the following PostgreSQL group roles:

| Group Role | Purpose |
|---|---|
| `app_runtime_grp` | Provides the permissions required by the running application. |
| `app_readonly_grp` | Provides restricted read-only access for reporting and inspection. |
| `app_admin_grp` | Provides additional database administration privileges on application objects. |

The script also configures:

- Database connection privileges.
- Schema access permissions.
- Table permissions.
- Sequence permissions.
- Restrictions on modifying or deleting audit log records.
- Permissions for deleting vehicle records.
- Default privileges for newly created database objects.

**Important:** These PostgreSQL roles are different from the application roles used inside the website.

Customer, Staff, Finance, Admin, and System Admin are application-level roles enforced by the Spring Boot backend.

### 4.3 Create the Application Runtime Login

The `db_roles_and_grants.sql` file creates the group roles but does not automatically create the `app_runtime` login account.

Therefore, each student must create their own local application login.

Open **pgAdmin 4**.

1. Connect to your local PostgreSQL server.
2. Expand **Servers > PostgreSQL 18 > Databases**.
3. Select `car_rental_db`.
4. Open **Tools > Query Tool**.
5. Execute:

```sql
CREATE ROLE app_runtime
LOGIN
IN ROLE app_runtime_grp;
```

This creates the `app_runtime` login and makes it a member of `app_runtime_grp`.

### 4.4 Set the Application Runtime Password

After creating the account:

1. Expand **Login/Group Roles** in pgAdmin.
2. Find `app_runtime`.
3. Right-click it and select **Properties**.
4. Open the **Definition** tab.
5. Enter a strong password.
6. Save the changes.

Each student must create and use their own local password.

**Important:**

- Do not use the `postgres` administrator password.
- Do not share your `app_runtime` password with other student.
- Do not upload database passwords to GitHub.
- If `app_runtime` already exists, do not create it again. Verify its membership and update its password if necessary.

### 4.5 Verify the Runtime Account

To verify that the account was created successfully, execute the following query in pgAdmin:

```sql
SELECT
    r.rolname AS role_name,
    r.rolsuper AS is_superuser,
    r.rolcreatedb AS can_create_database,
    r.rolcreaterole AS can_create_roles,
    r.rolcanlogin AS can_login
FROM pg_roles r
WHERE r.rolname IN (
    'app_runtime',
    'app_runtime_grp',
    'app_readonly_grp',
    'app_admin_grp'
)
ORDER BY r.rolname;
```

The `app_runtime` account should have the following attributes:

| Attribute | Expected Result |
|---|---|
| Superuser | false |
| Create Database | false |
| Create Roles | false |
| Can Login | true |

The three group roles should not have login privileges.

You can also verify membership using:

```sql
SELECT
    member_role.rolname AS member_role,
    group_role.rolname AS group_role
FROM pg_auth_members m
JOIN pg_roles member_role
    ON member_role.oid = m.member
JOIN pg_roles group_role
    ON group_role.oid = m.roleid
WHERE member_role.rolname = 'app_runtime';
```

The result should show that `app_runtime` is a member of `app_runtime_grp`.

**Note:** The `app_admin_grp` role is a database-level group role. It is not the same as the System Admin account inside the website.

---

# STEP 5: CONFIGURE AND START THE BACKEND APPLICATION

After completing the database setup, configure the Spring Boot application.

Open a new Windows PowerShell window.

Navigate to the Application folder:

```powershell
cd "C:\Users\YOUR_USERNAME\OneDrive\Desktop\Car_Rental_Management_System\Application"
```

### 5.1 Configure the Environment Variables

Before starting Spring Boot, configure the required environment variables.

#### A. PostgreSQL Database Password

Set the password of your local `app_runtime` account:

```powershell
$env:DB_PASSWORD = "YOUR_APP_RUNTIME_PASSWORD"
```

Replace the placeholder with the password you configured in pgAdmin.

**Important:** The application uses `app_runtime`, not `postgres`.

#### B. Gmail Email Configuration

If you want to use the application's email functionality, configure the Gmail account used to send emails:

```powershell
$env:MAIL_USERNAME = "YOUR_GMAIL_ADDRESS"
$env:MAIL_PASSWORD = "YOUR_GMAIL_APP_PASSWORD"
```

For `MAIL_PASSWORD`, use a Google App Password instead of your regular Gmail password.

To generate a Google App Password:

1. Open your Google Account.
2. Go to Security.
3. Enable 2-Step Verification if it is not yet enabled.
4. Find the App passwords option.
5. Generate a new App Password for the application.
6. Use the generated password as the value of `MAIL_PASSWORD`.

#### C. Frontend URL and CORS Configuration

If the frontend is running separately through port `5500`, configure:

```powershell
$env:FRONTEND_URL = "http://localhost:5500"
$env:CORS_ORIGINS = "http://localhost:5500"
```

If the frontend is served directly by Spring Boot on port `8080`, use:

```powershell
$env:FRONTEND_URL = "http://localhost:8080"
$env:CORS_ORIGINS = "http://localhost:8080"
```

Use the configuration that matches how the frontend is being accessed.

#### D. JWT Secret Configuration

Set a random secret used for JSON Web Token (JWT) authentication:

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String((1..48 | % { Get-Random -Max 256 }))
```

This generates a random secret for the current PowerShell session.

Keep the same JWT secret when restarting the application if you want existing login tokens to remain valid. Generating a new secret invalidates tokens created using the previous secret.

#### E. Seed Admin Password

Set the password used when the application creates its initial administrator account, if applicable:

```powershell
$env:SEED_ADMIN_PASSWORD = "YOUR_CHOSEN_ADMIN_PASSWORD"
```

Choose a strong password and keep it private.

This setting may only apply when the administrator account is initially created. Changing it does not necessarily change the password of an existing administrator account.

### 5.2 Start the Spring Boot Application

After configuring the environment variables, run:

```powershell
.\mvnw.cmd spring-boot:run
```

Wait for the application to finish starting.

Look for messages indicating that the application and embedded Tomcat server have started successfully, such as:

```text
Tomcat started on port 8080
Started CarRentalAppApplication
```

Keep the terminal open while using the application. Closing the terminal or stopping the process will stop the backend.

The backend application uses port `8080` by default.

### Important Backend Reminders

- The application connects using the `app_runtime` account.
- Do not change the Spring Boot database username to `postgres`.
- Make sure `DB_PASSWORD` matches the password of the local `app_runtime` account.
- The application uses `spring.jpa.hibernate.ddl-auto=none`. Hibernate will not automatically create missing database tables.
- Required database migrations must be completed before starting the application.

### Troubleshooting

**If the terminal reports password authentication failed for user `app_runtime`:**

Check whether `DB_PASSWORD` matches the password configured for your local `app_runtime` account.

**If the terminal reports password authentication failed for user `postgres`:**

Check the Spring Boot database configuration. The application should use `app_runtime`, not `postgres`.

**If the terminal reports that port 8080 is already in use:**

The backend may already be running. Check the existing process before starting another instance.

**If the application reports that `JWT_SECRET` is not set:**

Configure the JWT environment variable before restarting the application.

**If Gmail authentication fails:**

Check the Gmail address and App Password.

**If the application reports missing columns or tables:**

Verify that all required database migration scripts were executed successfully using the database administrator account.

---

# STEP 6: START THE FRONTEND WEBSITE

The frontend can be accessed in two ways, depending on the project's configuration.

### Option A: Through Spring Boot

If the backend terminal shows that it has mapped the frontend's `index.html` as its welcome page, access the frontend directly through:

```text
http://localhost:8080
```

In this setup, you do not need to start another frontend server using Python.

### Option B: Through a Separate Python Server

If the frontend is not being served by Spring Boot, open another PowerShell window.

Navigate to the Frontend folder:

```powershell
cd "C:\Users\YOUR_USERNAME\OneDrive\Desktop\Car_Rental_Management_System\Frontend"
```

Start the local frontend server:

```powershell
python -m http.server 5500
```

Keep this terminal open while using the website.

The frontend website uses port `5500` by default.

Access it through:

```text
http://localhost:5500
```

---

# STEP 7: OPEN AND TEST THE APPLICATION

Open a web browser and visit the appropriate frontend address.

If the frontend is served by Spring Boot:

```text
http://localhost:8080
```

If the frontend uses a separate Python server:

```text
http://localhost:5500
```

The backend application runs on:

```text
http://localhost:8080
```

Confirm that the frontend can communicate with the backend and that the backend can connect to the PostgreSQL database.

Test the main application workflow, including:

1. Customer registration and login.
2. Viewing available vehicles.
3. Submitting a reservation.
4. Reviewing and approving or rejecting a reservation.
5. Submitting and verifying payment.
6. Recording vehicle pickup and return.

Also verify that the application can retrieve, insert, and update database records successfully.

---

# STEP 8: OPTIONAL – MAKE THE APPLICATION ACCESSIBLE THROUGH CLOUDFLARE TUNNEL

Cloudflare Tunnel can provide a public URL for accessing your locally running application without directly exposing your computer's IP address.

### A. Install Cloudflared

Open a new PowerShell window.

Install Cloudflared using Windows Package Manager:

```powershell
winget install Cloudflare.cloudflared
```

Wait for the installation to finish.

### B. Expose the Backend Application

Make sure the Spring Boot application is running on port `8080`.

Open another PowerShell window.

Run:

```powershell
cloudflared tunnel --url http://localhost:8080
```

Wait for Cloudflared to generate a public URL.

It will look similar to:

```text
https://example-name.trycloudflare.com
```

Copy the generated URL and open it in your browser to check whether the application is accessible.

### C. If the Frontend Uses a Separate Python Server

If the frontend is running on port `5500`, open another PowerShell window and run:

```powershell
cloudflared tunnel --url http://localhost:5500
```

Cloudflare will generate another public URL for the frontend.

If you use separate public URLs for the frontend and backend, update the application's frontend URL, CORS configuration, and API base URL as required by the project configuration.

Restart the backend after changing its environment variables.

### Important Cloudflare Reminders

- Keep the backend and any separate frontend server running while using the tunnel.
- Keep the Cloudflared terminal open.
- Quick Tunnel URLs are temporary and may change when a new tunnel is started.
- Do not share the public URL with unauthorized users.
- A public tunnel does not replace authentication or other application security measures.
- Do not expose the PostgreSQL database port through the tunnel.
- Only use a public tunnel for testing when the application has appropriate authentication and security protections.

---

# STEP 9: STOP THE APPLICATION

When you finish testing:

1. Return to the terminal running the frontend server, if you started one.
2. Press `Ctrl + C` to stop the frontend server.
3. Return to the terminal running Spring Boot.
4. Press `Ctrl + C` to stop the backend application.
5. If Cloudflare Tunnel is running, return to its terminal and press `Ctrl + C`.

Stopping the backend or frontend does not automatically stop the PostgreSQL service.

---

# ADDITIONAL REMINDERS AND TROUBLESHOOTING

### Database Configuration

| Configuration | Value |
|---|---|
| Database Management System | PostgreSQL |
| Database Name | `car_rental_db` |
| Host | `localhost` |
| Port | `5432` |
| Database Administrator | `postgres` |
| Application Login | `app_runtime` |
| Application Group Role | `app_runtime_grp` |

### Database Security

The project uses separate PostgreSQL accounts to provide restricted application access.

- `postgres` is used for initial database setup and administrative operations.
- `app_runtime` is used by the Spring Boot application.
- `app_runtime_grp` provides the application's required read and write permissions.
- `app_readonly_grp` provides restricted read-only access.
- `app_admin_grp` provides additional database administration privileges on application objects.

The application runtime account is not a superuser and does not have permission to create schemas or permanent database objects.

### Environment Variables

- The backend reads the PostgreSQL password from `DB_PASSWORD`.
- Gmail credentials are configured through `MAIL_USERNAME` and `MAIL_PASSWORD`.
- JWT authentication uses `JWT_SECRET`.
- The frontend address and allowed origin are configured through `FRONTEND_URL` and `CORS_ORIGINS`.
- The initial application administrator password may be configured through `SEED_ADMIN_PASSWORD`.

Environment variables set using `$env:VARIABLE_NAME` are only available in the current PowerShell session.

If you open a new PowerShell window, set the required environment variables again before starting the backend.

### Important Security Reminders

- Never commit passwords, Gmail App Passwords, JWT secrets, or other private credentials to GitHub.
- Each developer must configure their own local PostgreSQL administrator and application runtime passwords.
- Do not use the PostgreSQL administrator account as the application's database login.
- Do not grant superuser privileges to `app_runtime`.
- Do not grant unnecessary database permissions to the application.
- Do not expose the PostgreSQL port through a public tunnel.

### Local Database Reminder

Each developer has their own local `car_rental_db` database.

Changes made to one developer's database will not automatically appear in another developer's database.

If the database structure or sample data changes, the updated SQL scripts or database backup must be shared with the other developers.

If the application fails to start, refer to `docs/troubleshooting.md` for common errors and possible solutions.

---

# END OF SETUP GUIDE
- Do not commit passwords, App Passwords, JWT secrets, or other private credentials to GitHub.
- If the application fails to start, refer to `docs/troubleshooting.md` for common errors and possible solutions.
