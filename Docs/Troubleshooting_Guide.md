# TROUBLESHOOTING GUIDE

## Purpose

This guide covers common setup and development problems when preparing the Car Rental Management System on a Windows computer using PostgreSQL and Spring Boot.

## Database Password and Connection Errors

### Problem 1: DB_PASSWORD Is Not Set

**Possible cause:** The database password environment variable was not set in the terminal before starting the application.

**Solution:**

1. Stop the application if it is still running.
2. Open a terminal in the Application folder.
3. Set the DB_PASSWORD environment variable using the command for your terminal.
4. Start the application again using the Maven Wrapper command.

For Windows Command Prompt:

```cmd
set "DB_PASSWORD=your_actual_postgres_password"
mvnw.cmd spring-boot:run
```

For PowerShell:

```powershell
$env:DB_PASSWORD = "your_actual_postgres_password"
.\mvnw.cmd spring-boot:run
```

### Problem 2: Password Authentication Failed

**Possible cause:** The password is incorrect, or the application is connecting with a different PostgreSQL username than expected.

**Solution:**

1. Verify the PostgreSQL username in `application.properties`.
2. Confirm that the password belongs to that PostgreSQL account.
3. Set the correct `DB_PASSWORD` value in the terminal.
4. Restart the application and check the terminal for the result.

### Problem 3: Connection Refused

**Possible cause:** PostgreSQL is not running, or the application is using the wrong host or port.

**Solution:**

1. Check that the PostgreSQL server is running.
2. Confirm that the configured host is `localhost` and the port is `5432`.
3. Check that the database connection settings in `application.properties` are correct.
4. Run the application again.

### Problem 4: Database or Table Does Not Exist

**Possible cause:** The database has not been created, or the schema SQL file has not been executed.

**Solution:**

1. Open pgAdmin and connect to your PostgreSQL server.
2. Check whether `car_rental_db` exists.
3. Verify that the required tables are present.
4. If needed, follow the project's database setup instructions to create the database and tables.
5. Restart the application and check for errors.

### Problem 5: Whitelabel Error Page with Status 404

**Possible cause:** The requested URL does not have a matching controller or page.

**Solution:**

1. Check the terminal to confirm that Spring Boot started successfully.
2. Confirm that you are opening a URL supported by the application.
3. If no homepage or root controller has been implemented, a 404 response at [http://localhost:8080](http://localhost:8080) may be expected.

A 404 response at the
