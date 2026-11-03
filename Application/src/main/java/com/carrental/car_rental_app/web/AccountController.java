package com.carrental.car_rental_app.web;

import com.carrental.car_rental_app.support.AuditService;
import com.carrental.car_rental_app.support.CurrentUser;
import com.carrental.car_rental_app.support.PasswordPolicy;
import com.carrental.car_rental_app.support.RateLimitService;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/** The logged-in person's own profile and password (customers and staff). */
@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final CurrentUser users;
    private final AuditService audit;
    private final RateLimitService limits;

    public AccountController(JdbcTemplate db, PasswordEncoder encoder, CurrentUser users, AuditService audit, RateLimitService limits) {
        this.db = db; this.encoder = encoder; this.users = users; this.audit = audit; this.limits = limits;
    }

    record Me(String name, String email, String role, String phone) {}
    record PasswordRequest(String currentPassword, String newPassword) {}

    @GetMapping("/me")
    public Me me(Authentication a) {
        boolean customer = users.isCustomer(a);
        List<Me> rows = db.query(
                customer ? "SELECT first_name, last_name, email, phone_number FROM customer WHERE lower(email) = lower(?)"
                         : "SELECT first_name, last_name, email, NULL AS phone_number FROM staff WHERE lower(email) = lower(?)",
                (rs, i) -> new Me(rs.getString("first_name") + " " + rs.getString("last_name"), rs.getString("email"),
                        users.role(a), rs.getString("phone_number")),
                a.getName());
        if (rows.isEmpty()) throw ApiError.notFound("Account not found.");
        return rows.get(0);
    }

    @PostMapping("/password")
    public Map<String, String> changePassword(@RequestBody PasswordRequest r, Authentication a) {
        PasswordPolicy.check(r.newPassword());
        String limitKey = "pwchange:" + a.getName().toLowerCase();
        if (limits.isBlocked(limitKey, 5, 15 * 60_000L))
            throw ApiError.tooMany("Too many wrong attempts. Try again in a few minutes.");
        boolean customer = users.isCustomer(a);
        String table = customer ? "customer" : "staff";   // fixed strings, never user input
        List<String> hashes = db.queryForList("SELECT password_hash FROM " + table + " WHERE lower(email) = lower(?)", String.class, a.getName());
        if (hashes.isEmpty() || r.currentPassword() == null || !encoder.matches(r.currentPassword(), hashes.get(0)))
        { limits.record(limitKey, 15 * 60_000L); throw ApiError.bad("Your current password is incorrect."); }
        limits.clear(limitKey);
        db.update("UPDATE " + table + " SET password_hash = ? WHERE lower(email) = lower(?)", encoder.encode(r.newPassword()), a.getName());
        if (!customer) audit.log(users.staffId(a), "UPDATE", "staff", null, "Changed own password");
        return Map.of("message", "Password changed.");
    }
}
