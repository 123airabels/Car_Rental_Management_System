
package com.carrental.car_rental_app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates one staff admin on first run so you can log in to the staff pages. Change the password after. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final String email, password;

    public DataSeeder(JdbcTemplate db, PasswordEncoder encoder,
                      @Value("${app.seed.admin-email}") String email,
                      @Value("${app.seed.admin-password}") String password) {
        this.db = db;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        Integer n = db.queryForObject("SELECT count(*) FROM staff", Integer.class);

        if (n != null && n == 0) {
            db.update(
                "INSERT INTO staff (first_name, last_name, email, password_hash, role) VALUES (?,?,?,?,?)",
                "Byahe", "Admin", email, encoder.encode(password), "Admin"
            );
        }
    }
}
