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

## Step 1: Clone the Project Repository

Download a local copy of the project repository from GitHub by following either of the methods below.

### Option A: Using GitHub Desktop

1. Open GitHub Desktop and sign in to your GitHub account.
2. Click **File > Clone Repository**.
3. Select the **URL** tab.
4. Copy and paste the GitHub repository URL.
5. Choose the local folder where you want to save the project.
6. Click **Clone**.
7. Once cloning is complete, click **Open in Visual Studio Code** or open the project folder manually.

### Option B: Using Git Bash

1. Open Git Bash.
2. Navigate to the location where you want to save the project:

   ```bash
   cd ~/OneDrive/Desktop
   ```

3. Clone the repository:

   ```bash
   git clone YOUR_GITHUB_REPOSITORY_URL
   ```

4. Navigate to the cloned project folder:

   ```bash
   cd Car_Rental_Management_System
   ```

5. Check the project folders:

   ```bash
   ls
   ```

   The project should contain the following folders:
   - `Database`
   - `Application`
   - `Frontend`

## Step 2: Open the Project Folder

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

## Step 3: Set Up the PostgreSQL Database

1. Make sure the PostgreSQL service is running.
2. Confirm that PostgreSQL is using port `5432`.
3. Navigate to the `Database` folder:

   ```powershell
   cd C:\Users\Aira\OneDrive\Desktop\Car_Rental_Management_System\Database
   ```

4. Check the available database files:

   ```powershell
   dir
   ```

5. If the `car_rental_db` database has not been created yet, create it:

   ```powershell
   psql -U postgres -h localhost -p 5432 -c "CREATE DATABASE car_rental_db;"
   ```

   Or use the full PostgreSQL executable path:

   ```powershell
   & "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h localhost -p 5432 -c "CREATE DATABASE car_rental_db;"
   ```

6. Run the database schema to create the required tables:

   ```powershell
   psql -U postgres -h localhost -p 5432 -d car_rental_db -f schema.sql
   ```

7. Insert the sample data:

   ```powershell
   psql -U postgres -h localhost -p 5432 -d car_rental_db -f seed.sql
   ```

8. Verify that the database and its tables were created successfully.

**Reminder:**
- If the database and tables already exist, do not run the schema and seed files again unless you are intentionally setting up or resetting the database.
- Do not run `reset_seed.sql` unless you intend to reset the sample data.
- Each developer must use their own local PostgreSQL password.

## Step 4: Configure and Start the Backend Application

1. Open a new PowerShell window.
2. Navigate to the `Application` folder:

   ```powershell
   cd C:\Users\Aira\OneDrive\Desktop\Car_Rental_Management_System\Application
   ```

### 4.1 Configure the Environment Variables

Before starting the Spring Boot application, configure the required environment variables in PowerShell.

**A. PostgreSQL Database Password**

Set the password for your local PostgreSQL account:

```powershell
$env:DB_PASSWORD = "YOUR_ACTUAL_POSTGRES_PASSWORD"
```

Replace the placeholder with the password you use for your PostgreSQL `postgres` account.

**B. Gmail Email Configuration**

Set the Gmail account that the application will use to send emails:

```powershell
$env:MAIL_USERNAME = "YOUR_GMAIL_ADDRESS"
$env:MAIL_PASSWORD = "YOUR_GMAIL_APP_PASSWORD"
```

For `MAIL_PASSWORD`, use a Google App Password instead of your regular Gmail password.

To generate a Google App Password:

1. Open your Google Account.
2. Go to **Security**.
3. Enable **2-Step Verification** if it is not yet enabled.
4. Find the **App passwords** option.
5. Generate a new App Password for the application.
6. Use the generated password as the value of `MAIL_PASSWORD`.

**Important:** Never share your Gmail password or App Password. Do not upload these credentials to GitHub.

**C. Frontend URL and CORS Configuration**

If the frontend is running separately through port `5500`, configure:

```powershell
$env:FRONTEND_URL = "http://localhost:5500"
$env:CORS_ORIGINS = "http://localhost:5500"
```

These variables identify the frontend address used by the application and the frontend origin allowed to communicate with the backend.

If the frontend is served directly by Spring Boot on port `8080`, use the corresponding frontend origin instead:

```powershell
$env:FRONTEND_URL = "http://localhost:8080"
$env:CORS_ORIGINS = "http://localhost:8080"
```

Use the configuration that matches how the frontend is being accessed.

**D. JWT Secret Configuration**

Set a random secret used for JSON Web Token (JWT) authentication:

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String((1..48 | % { Get-Random -Max 256 }))
```

This generates a random secret for the current PowerShell session.

**Note:** Keep the same JWT secret when restarting the application if you want existing login tokens to remain valid. Generating a new secret invalidates tokens created using the previous secret.

**E. Seed Admin Password**

Set the password used when the application creates its seeded administrator account, if applicable:

```powershell
$env:SEED_ADMIN_PASSWORD = "YOUR_CHOSEN_ADMIN_PASSWORD"
```

Choose a strong password and keep it private.

**Note:** This variable may only apply when the administrator account is initially created by the application's seeding process. Changing it does not necessarily change the password of an existing administrator account.

### 4.2 Start the Spring Boot Application

After configuring the environment variables, run:

```powershell
.\mvnw.cmd spring-boot:run
```

Wait for the application to finish starting.

Check the terminal for messages indicating that the application and embedded Tomcat server have started successfully, such as:

```text
Tomcat started on port 8080
Started CarRentalAppApplication
```

Keep this terminal open while using the application. Closing the terminal or stopping the process will stop the backend.

The backend application uses port `8080` by default.

**Important:**
- If the terminal reports that port `8080` is already in use, the backend may already be running. Do not start another instance.
- If the application reports `password authentication failed for user "postgres"`, check whether `DB_PASSWORD` matches your actual PostgreSQL password.
- If the application reports `JWT_SECRET is not set`, configure the JWT environment variable before restarting.
- If Gmail authentication fails, check your Gmail address and App Password.

## Step 5: Start the Frontend Website

The frontend can be accessed in two ways, depending on the project's configuration.

### Option A: Through Spring Boot

If the backend terminal shows that it has mapped the frontend's `index.html` as its welcome page, you can access the frontend directly through:

```text
http://localhost:8080
```

In this setup, you do not need to start another frontend server using Python.

### Option B: Through a Separate Python Server

If the frontend is not being served by Spring Boot, open another PowerShell window.

1. Navigate to the `Frontend` folder:

   ```powershell
   cd C:\Users\Aira\OneDrive\Desktop\Car_Rental_Management_System\Frontend
   ```

2. Start the local frontend server:

   ```powershell
   python -m http.server 5500
   ```

3. Keep this terminal open while using the website.

The frontend website uses port `5500` by default.

Access it through:

```text
http://localhost:5500
```

## Step 6: Open and Test the Application

Open a web browser and visit the appropriate frontend address:

- Frontend served by Spring Boot: `http://localhost:8080`
- Frontend served separately: `http://localhost:5500`
- Backend application: `http://localhost:8080`

Use the frontend website to test the system. Confirm that the frontend can communicate with the backend and that the backend can connect to the PostgreSQL database.

Test the main workflow, including:

1. Customer registration and login.
2. Viewing available vehicles.
3. Submitting a reservation.
4. Reviewing and approving or rejecting a reservation.
5. Submitting and verifying payment.
6. Recording vehicle pickup and return.

## Step 7: Optional – Make the Application Accessible Through Cloudflare Tunnel

Cloudflare Tunnel can provide a public URL for accessing your locally running application without directly exposing your computer's IP address.

**A. Install Cloudflared**

1. Open a new PowerShell window.
2. Install Cloudflared using Windows Package Manager:

   ```powershell
   winget install Cloudflare.cloudflared
   ```

3. Wait for the installation to finish.

**B. Expose the Backend Application**

1. Make sure the Spring Boot application is running on port `8080`.
2. Open another PowerShell window.
3. Run:

   ```powershell
   cloudflared tunnel --url http://localhost:8080
   ```

4. Wait for Cloudflared to generate a public URL.

   It will look similar to:

   ```text
   https://example-name.trycloudflare.com
   ```

5. Copy the generated URL and open it in your browser to check whether the application is accessible.

**C. If the Frontend Uses a Separate Python Server**

If the frontend is running on port `5500`, open another PowerShell window and run:

```powershell
cloudflared tunnel --url http://localhost:5500
```

Cloudflare will generate another public URL for the frontend.

If you use separate public URLs for the frontend and backend, update the application's frontend URL, CORS configuration, and API base URL as required by the project configuration. Restart the backend after changing its environment variables.

**Important Reminders:**
- Keep the backend and any separate frontend server running while using the tunnel.
- Keep the Cloudflared terminal open.
- Quick Tunnel URLs are temporary and may change when a new tunnel is started.
- Do not share the public URL with unauthorized users.
- A public tunnel does not replace authentication or other application security measures.
- Do not expose a database port through the tunnel.

## Step 8: Stop the Application

When you finish testing:

1. Return to the terminal running the frontend server, if you started one.
2. Press `Ctrl + C` to stop the frontend server.
3. Return to the terminal running Spring Boot.
4. Press `Ctrl + C` to stop the backend application.
5. If Cloudflare Tunnel is running, return to its terminal and press `Ctrl + C`.

Stopping the backend or frontend does not automatically stop the PostgreSQL service.

## Additional Reminders and Troubleshooting

- PostgreSQL is configured to use `localhost:5432`.
- The database name is `car_rental_db`.
- The backend reads the PostgreSQL password from the `DB_PASSWORD` environment variable.
- The application uses `spring.jpa.hibernate.ddl-auto=none`. Hibernate will not automatically create missing database tables.
- Environment variables set using `$env:VARIABLE_NAME` are only available in the current PowerShell session. If you open a new PowerShell window, set them again before starting the backend.
- Each developer must configure their own local PostgreSQL password.
- Do not commit passwords, App Passwords, JWT secrets, or other private credentials to GitHub.
- If the application fails to start, refer to `docs/troubleshooting.md` for common errors and possible solutions.