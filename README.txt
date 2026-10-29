PROJECT OVERVIEW
The Car Rental Management System is a database-driven application designed to organize the main operations of a self-drive car rental business. It aims to manage customer information, driver's license verification, vehicle records, rental rates, reservations, payments, vehicle inspections, damage records, maintenance, cancellations, and rental history using PostgreSQL.

The proposed system allows customers to register, browse available vehicles, submit rental requests, and view their booking and payment information. Rental staff can manage vehicle records, verify driver's licenses, review rental requests, record vehicle inspections, process returns, and manage rental-related transactions.

The application uses Java and Spring Boot for the backend and PostgreSQL for data storage. The project is being developed with a focus on relational database design, data integrity, and secure access to records.

PROJECT OBJECTIVES
Organize customer, vehicle, rental, and payment records in a centralized database.
Maintain relationships and data integrity through primary keys, foreign keys, and other constraints.
Support the development of core car rental management operations.
Provide a foundation for database administration activities, including access control, monitoring, backup, and recovery.

PREREQUISITES
Before running the application, make sure you have the following installed and configured:
- Java JDK 17 — required to run the Spring Boot application.
- PostgreSQL — the database server must be running.
- Car Rental Database — the `car_rental_db` database must exist, and the required tables must already be created.
- Project Files — make sure you have downloaded or cloned the project repository.

HOW TO RUN THE APPLICATION
Follow these steps to run the Car Rental Management System on your local computer.
Step 1: Open the Project Folder
1. Open the project folder on your computer.
2. Open the `Application` folder, where the `mvnw.cmd` file is located.
3. Open a terminal in this folder.

   Your terminal should be inside the `Application` folder before proceeding.

Step 2: Make Sure PostgreSQL Is Running
1. Open PostgreSQL using your installed PostgreSQL service or pgAdmin.
2. Make sure the PostgreSQL server is running on port `5432`.
3. Check that the `car_rental_db` database exists and contains the required tables.
If the database or tables are missing, follow the database setup instructions in the project's database documentation before continuing.

Step 3: Set the Database Password [Continuation of Step 1]
The application reads the PostgreSQL password from an environment variable named `DB_PASSWORD`. This helps prevent the actual password from being written directly into the project's configuration file.

Choose the instructions that match your terminal.
Option A: Windows Command Prompt (CMD)

1. In the terminal, enter the following command:
   set "DB_PASSWORD=your_actual_postgres_password"
2. Replace `your_actual_postgres_password` with the password you use to connect to PostgreSQL.
3. Press Enter.

Option B: Windows PowerShell
1. In the terminal, enter the following command:
   $env:DB_PASSWORD = "your_actual_postgres_password"
2. Replace `your_actual_postgres_password` with your actual PostgreSQL password.
3. Press Enter.

Step 4: Start the Spring Boot Application
After setting the password, run the command that matches your terminal.

If you are using Command Prompt (CMD):
mvnw.cmd spring-boot:run

If you are using PowerShell:
.\mvnw.cmd spring-boot:run

- Press Enter and wait for Spring Boot to finish starting. The first run may take longer because Maven needs to download the required dependencies.
- Keep the terminal open while using the application. Closing it or stopping the running process will stop the application.

Step 5: Check the Application
- Check the terminal for startup messages indicating that Spring Boot and the embedded Tomcat server have started successfully.
- The application is configured to use port `8080` by default. If a web page or API endpoint has already been implemented, open the corresponding URL in your browser or API testing tool.
- For example:
http://localhost:8080

Step 6: Stop the Application
When you finish testing, return to the terminal where the application is running and press:
Ctrl + C

This stops the Spring Boot application. Your PostgreSQL database service remains separate and will not automatically stop when you stop Spring Boot.

ADDITIONAL REMINDER:
- The current database configuration uses `localhost:5432` and the database name `car_rental_db`.
- The application uses `spring.jpa.hibernate.ddl-auto=none`, so Hibernate will not automatically create the database tables.
- Each developer must configure their own local PostgreSQL password.
- If the application fails to start, refer to `docs/troubleshooting.md` for common errors and possible solutions.