package com.carrental.car_rental_app.web;

import com.carrental.car_rental_app.support.AuditService;
import com.carrental.car_rental_app.support.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Customer management.
 *  - A customer views and edits their own profile and contact details (/api/account/profile).
 *  - Staff open a customer's full record, edit contact details, and see rental history (/api/admin/customers/{id}...).
 * Registration is in AuthController; the customer list and account-status change are in AdministrationController.
 * Access rules are in SecurityConfig.
 */
@RestController
@RequestMapping("/api")
public class CustomerController {
    private static final Pattern PHONE = Pattern.compile("^[0-9+()\\-\\s]{7,30}$");

    private final JdbcTemplate db;
    private final CurrentUser users;
    private final AuditService audit;

    public CustomerController(JdbcTemplate db, CurrentUser users, AuditService audit) {
        this.db = db; this.users = users; this.audit = audit;
    }

    record ProfileDto(int id, String firstName, String lastName, String email, String phone, String address,
                      String status, LocalDateTime createdAt) {}
    record ProfileRequest(String firstName, String lastName, String phone, String address) {}
    record CustomerDetail(ProfileDto profile, String licenseStatus, LocalDate licenseExpires,
                          int totalRentals, int completedRentals, BigDecimal totalPaid) {}
    record HistoryDto(int id, String vehicle, String plateNumber, LocalDateTime pickup, LocalDateTime returnBy,
                      LocalDateTime actualReturn, BigDecimal total, BigDecimal lateFee, BigDecimal paid, String status) {}

    // ---------- the customer's own profile ----------

    @GetMapping("/account/profile")
    public ProfileDto myProfile(Authentication a) {
        return profile(users.customerId(a));
    }

    @PutMapping("/account/profile")
    @Transactional
    public ProfileDto updateMyProfile(@RequestBody ProfileRequest r, Authentication a) {
        int id = users.customerId(a);
        save(id, r);
        return profile(id);
    }

    // ---------- staff view of a customer ----------

    @GetMapping("/admin/customers/{id}")
    public CustomerDetail detail(@PathVariable int id) {
        ProfileDto p = profile(id);
        var lic = db.queryForList(
                "SELECT verification_status, expiration_date FROM driver_license WHERE customer_id = ? ORDER BY license_id DESC LIMIT 1", id);
        String licStatus = lic.isEmpty() ? null : Rows.up(String.valueOf(lic.get(0).get("verification_status")));
        Object exp = lic.isEmpty() ? null : lic.get(0).get("expiration_date");
        LocalDate licExpires = exp instanceof java.sql.Date d ? d.toLocalDate() : exp instanceof LocalDate l ? l : null;
        Integer total = db.queryForObject("SELECT count(*) FROM rental WHERE customer_id = ?", Integer.class, id);
        Integer done = db.queryForObject("SELECT count(*) FROM rental WHERE customer_id = ? AND rental_status = 'Completed'", Integer.class, id);
        BigDecimal paid = db.queryForObject(
                "SELECT COALESCE(SUM(p.amount), 0) FROM payment p JOIN rental r ON r.rental_id = p.rental_id"
              + " WHERE r.customer_id = ? AND p.payment_status = 'Paid'", BigDecimal.class, id);
        return new CustomerDetail(p, licStatus, licExpires, total == null ? 0 : total, done == null ? 0 : done, paid);
    }

    @PutMapping("/admin/customers/{id}")
    @Transactional
    public ProfileDto updateCustomer(@PathVariable int id, @RequestBody ProfileRequest r, Authentication a) {
        int me = users.staffId(a);
        profile(id);   // 404 if the customer doesn't exist
        save(id, r);
        audit.log(me, "UPDATE", "customer", id, "Updated contact details of customer " + id);
        return profile(id);
    }

    @GetMapping("/admin/customers/{id}/rentals")
    public List<HistoryDto> history(@PathVariable int id) {
        profile(id);
        return db.query("""
            SELECT r.rental_id, v.make || ' ' || v.model AS vehicle, v.plate_number, r.pickup_datetime, r.return_datetime,
                   r.actual_return_datetime, COALESCE(r.final_cost, r.estimated_cost) AS total, r.late_fee, r.rental_status,
                   (SELECT COALESCE(SUM(p.amount), 0) FROM payment p WHERE p.rental_id = r.rental_id AND p.payment_status = 'Paid') AS paid
            FROM rental r JOIN vehicle v ON v.vehicle_id = r.vehicle_id
            WHERE r.customer_id = ? ORDER BY r.rental_id DESC
            """, (rs, i) -> new HistoryDto(rs.getInt("rental_id"), rs.getString("vehicle"), rs.getString("plate_number"),
                Rows.ldt(rs, "pickup_datetime"), Rows.ldt(rs, "return_datetime"), Rows.ldt(rs, "actual_return_datetime"),
                rs.getBigDecimal("total"), rs.getBigDecimal("late_fee"), rs.getBigDecimal("paid"), Rows.up(rs.getString("rental_status"))), id);
    }

    // ---------- helpers ----------

    private ProfileDto profile(int id) {
        List<ProfileDto> rows = db.query(
                "SELECT customer_id, first_name, last_name, email, phone_number, address, account_status, created_at FROM customer WHERE customer_id = ?",
                (rs, i) -> new ProfileDto(rs.getInt("customer_id"), rs.getString("first_name"), rs.getString("last_name"), rs.getString("email"),
                        rs.getString("phone_number"), rs.getString("address"), Rows.up(rs.getString("account_status")), Rows.ldt(rs, "created_at")), id);
        if (rows.isEmpty()) throw ApiError.notFound("Customer not found.");
        return rows.get(0);
    }

    /** Validates and saves name, phone and address. The email is the login and is not changed here. */
    private void save(int id, ProfileRequest r) {
        String first = Rows.clean(r.firstName()), last = Rows.clean(r.lastName()), phone = Rows.clean(r.phone()), address = Rows.clean(r.address());
        if (first == null || last == null) throw ApiError.bad("Enter a first and last name.");
        if (first.length() > 100 || last.length() > 100) throw ApiError.bad("A name is too long (100 characters at most).");
        if (phone == null || !PHONE.matcher(phone).matches()) throw ApiError.bad("Enter a valid phone number (digits, +, -, spaces, 7 to 30 characters).");
        if (address != null && address.length() > 500) throw ApiError.bad("The address is too long (500 characters at most).");
        db.update("UPDATE customer SET first_name = ?, last_name = ?, phone_number = ?, address = ? WHERE customer_id = ?",
                first, last, phone, address, id);
    }
}
