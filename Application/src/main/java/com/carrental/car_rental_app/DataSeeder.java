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
        this.db = db; this.encoder = encoder; this.email = email; this.password = password;
    }

    @Override
    public void run(String... args) {
        // Older databases lack the column that stores the decision on a disputed damage fee.
        try { db.execute("ALTER TABLE damage_fee ADD COLUMN IF NOT EXISTS resolution_notes text"); }
        catch (Exception e) { System.err.println("Could not add damage_fee.resolution_notes; run Database/damage_dispute_resolution.sql manually: " + e.getMessage()); }
        // Older databases lack the payment code and the "received by" staff column (same as Database/payment_codes.sql).
        try {
            db.execute("ALTER TABLE payment ADD COLUMN IF NOT EXISTS payment_code character varying(20)");
            db.execute("ALTER TABLE payment ADD COLUMN IF NOT EXISTS received_by integer REFERENCES staff(staff_id)");
            db.update("UPDATE payment SET payment_code = 'PAY-' || upper(substr(md5(random()::text || payment_id::text), 1, 6)) WHERE payment_code IS NULL");
            db.execute("CREATE UNIQUE INDEX IF NOT EXISTS payment_payment_code_key ON payment (payment_code)");
        } catch (Exception e) { System.err.println("Could not add payment.payment_code; run Database/payment_codes.sql manually: " + e.getMessage()); }
        Integer n = db.queryForObject("SELECT count(*) FROM staff", Integer.class);
        if (n != null && n == 0) {
            db.update("INSERT INTO staff (first_name, last_name, email, password_hash, role) VALUES (?,?,?,?,?)",
                    "Byahe", "Admin", email, encoder.encode(password), "Admin");
        }
    }
}
