package com.carrental.car_rental_app.web;

import com.carrental.car_rental_app.security.JwtService;
import com.carrental.car_rental_app.support.EmailService;
import com.carrental.car_rental_app.support.PasswordPolicy;
import com.carrental.car_rental_app.support.RateLimitService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Customers live in the `customer` table, staff in `staff`. One login endpoint checks both.
 * New customers start as 'Pending' and become 'Active' when they open the link emailed to them.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final long HOUR_MS = 3_600_000L;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final EmailService mailer;
    private final RateLimitService limits;
    private final int maxFailures;
    private final long windowMs;
    /** Checked when an email is unknown, so a wrong email and a wrong password take about the same time. */
    private final String dummyHash;

    public AuthController(JdbcTemplate db, PasswordEncoder encoder, JwtService jwt, EmailService mailer, RateLimitService limits,
                          @Value("${app.security.login-max-failures:5}") int maxFailures,
                          @Value("${app.security.login-window-minutes:15}") long windowMinutes) {
        this.db = db; this.encoder = encoder; this.jwt = jwt; this.mailer = mailer; this.limits = limits;
        this.maxFailures = maxFailures; this.windowMs = windowMinutes * 60_000L;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    record AuthRequest(String name, String phone, String email, String password) {}
    record VerifyRequest(String token) {}
    record UserDto(String name, String email, String role) {}
    record AuthResponse(String token, UserDto user) {}
    record MessageResponse(String message) {}

    @PostMapping("/register")
    public MessageResponse register(@RequestBody AuthRequest req, HttpServletRequest http) {
        String ipKey = "register:ip:" + http.getRemoteAddr();
        if (limits.isBlocked(ipKey, 10, HOUR_MS)) throw ApiError.tooMany("Too many sign-ups from this device. Try again later.");

        if (blank(req.name()) || blank(req.email()) || blank(req.phone()) || req.password() == null)
            throw ApiError.bad("Enter your name, email, phone number, and a password.");
        if (req.name().length() > 200 || req.phone().length() > 30 || req.email().length() > 255)
            throw ApiError.bad("One of the fields is too long.");
        String email = req.email().trim().toLowerCase();
        if (!EMAIL.matcher(email).matches()) throw ApiError.bad("Enter a valid email address.");
        PasswordPolicy.check(req.password());

        boolean taken = Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM customer WHERE lower(email) = ?) OR EXISTS (SELECT 1 FROM staff WHERE lower(email) = ?)",
                Boolean.class, email, email));
        if (taken) throw ApiError.bad("That email is already registered. Try logging in.");

        String[] parts = req.name().trim().split("\\s+", 2);
        String first = parts[0], last = parts.length > 1 ? parts[1] : "-";
        db.update("INSERT INTO customer (first_name, last_name, email, phone_number, password_hash, account_status) VALUES (?,?,?,?,?,'Pending')",
                first, last, email, req.phone().trim(), encoder.encode(req.password()));
        limits.record(ipKey, HOUR_MS);   // only real sign-ups count, so typos and rejected attempts never lock you out

        boolean sent = mailer.sendVerification(email, first, jwt.createVerifyToken(email));
        if (sent) return new MessageResponse("Account created. We sent a confirmation link to " + email + ". Open it to activate your account.");
        if (!mailer.isConfigured()) return new MessageResponse("Account created, but this server is not set up to send email yet. Ask the administrator to activate your account.");
        return new MessageResponse("Account created, but we could not send the confirmation email. Ask the administrator to activate your account.");
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest req, HttpServletRequest http) {
        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        String pw = req.password() == null ? "" : req.password();
        String emailKey = "login:email:" + email, ipKey = "login:ip:" + http.getRemoteAddr();
        if (limits.isBlocked(emailKey, maxFailures, windowMs) || limits.isBlocked(ipKey, maxFailures * 6, windowMs))
            throw ApiError.tooMany("Too many failed log-in attempts. Please wait " + (windowMs / 60_000L) + " minutes and try again.");

        boolean known = false;
        List<Map<String, Object>> c = db.queryForList(
                "SELECT first_name, last_name, password_hash, account_status FROM customer WHERE lower(email) = ?", email);
        if (!c.isEmpty()) {
            known = true;
            Map<String, Object> row = c.get(0);
            if (encoder.matches(pw, String.valueOf(row.get("password_hash")))) {
                String status = String.valueOf(row.get("account_status"));
                if ("Pending".equalsIgnoreCase(status))
                    throw ApiError.forbidden("Please confirm your email first. Open the link we sent you when you created your account.");
                if ("Active".equalsIgnoreCase(status)) {
                    limits.clear(emailKey);
                    return respond(row.get("first_name") + " " + row.get("last_name"), email, "CUSTOMER");
                }
            }
        }

        List<Map<String, Object>> s = db.queryForList(
                "SELECT first_name, last_name, password_hash, account_status, role FROM staff WHERE lower(email) = ?", email);
        if (!s.isEmpty()) {
            known = true;
            Map<String, Object> row = s.get(0);
            if (encoder.matches(pw, String.valueOf(row.get("password_hash"))) && "Active".equalsIgnoreCase(String.valueOf(row.get("account_status")))) {
                String role = String.valueOf(row.get("role")).trim().toUpperCase().replace(' ', '_');
                limits.clear(emailKey);
                return respond(row.get("first_name") + " " + row.get("last_name"), email, role);
            }
        }

        if (!known) encoder.matches(pw, dummyHash);
        limits.record(emailKey, windowMs);
        limits.record(ipKey, windowMs);
        throw ApiError.bad("Incorrect email or password.");
    }

    /** The link in the email lands on verify.html, which calls this with the token from the URL. */
    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@RequestBody VerifyRequest req) {
        String email;
        try {
            email = jwt.parseVerifyToken(req.token());
        } catch (JwtException | IllegalArgumentException e) {
            throw ApiError.bad("This confirmation link is invalid or has expired. Request a new one from the log-in page.");
        }
        // Only a Pending account is activated, so an old link can never re-open a suspended account.
        int n = db.update("UPDATE customer SET account_status = 'Active' WHERE lower(email) = ? AND account_status = 'Pending'", email);
        if (n == 1) return new MessageResponse("Your email is confirmed. You can log in now.");
        boolean active = Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM customer WHERE lower(email) = ? AND account_status = 'Active')", Boolean.class, email));
        if (active) return new MessageResponse("Your email is already confirmed. You can log in.");
        throw ApiError.bad("This account cannot be confirmed. Please contact support.");
    }

    private AuthResponse respond(String name, String email, String role) {
        return new AuthResponse(jwt.create(email, role), new UserDto(name, email, role));
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
}
