package com.carrental.car_rental_app.web;

import com.carrental.car_rental_app.support.AuditService;
import com.carrental.car_rental_app.support.CurrentUser;
import com.carrental.car_rental_app.support.FileStorage;
import com.carrental.car_rental_app.support.NotificationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Driver's license submission (customer) and verification (staff). */
@RestController
public class LicenseController {
    private final JdbcTemplate db;
    private final CurrentUser users;
    private final AuditService audit;
    private final FileStorage files;
    private final NotificationService notifier;

    public LicenseController(JdbcTemplate db, CurrentUser users, AuditService audit, FileStorage files, NotificationService notifier) {
        this.db = db; this.users = users; this.audit = audit; this.files = files; this.notifier = notifier;
    }

    record LicenseDto(int id, int customerId, String customer, String licenseNumber, String licenseType, LocalDate issueDate,
                      LocalDate expirationDate, String status, String rejectionReason, LocalDateTime verifiedAt) {}
    record RejectRequest(String reason) {}

    private static final String SELECT = """
        SELECT d.license_id, d.customer_id, c.first_name || ' ' || c.last_name AS customer, d.license_number, d.license_type,
               d.issue_date, d.expiration_date, d.verification_status, d.rejection_reason, d.verified_at
        FROM driver_license d JOIN customer c ON c.customer_id = d.customer_id
        """;

    private static LicenseDto map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new LicenseDto(rs.getInt("license_id"), rs.getInt("customer_id"), rs.getString("customer"), rs.getString("license_number"),
                rs.getString("license_type"), Rows.ld(rs, "issue_date"), Rows.ld(rs, "expiration_date"),
                Rows.up(rs.getString("verification_status")), rs.getString("rejection_reason"), Rows.ldt(rs, "verified_at"));
    }

    /** Multipart form: licenseNumber, licenseType, issueDate, expirationDate (yyyy-MM-dd), front, back (JPG or PNG). */
    @PostMapping(value = "/api/license", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public LicenseDto submit(@RequestParam("licenseNumber") String number, @RequestParam("licenseType") String type,
                             @RequestParam("issueDate") String issue, @RequestParam("expirationDate") String expiry,
                             @RequestParam("front") MultipartFile front, @RequestParam("back") MultipartFile back,
                             Authentication a) {
        int customerId = users.customerId(a);
        if (Rows.blank(number) || number.trim().length() > 100) throw ApiError.bad("Enter your license number.");
        if (Rows.blank(type) || type.trim().length() > 50) throw ApiError.bad("Enter your license type.");
        LocalDate issued = Rows.date(issue, "The issue date must look like 2026-12-31.");
        LocalDate expires = Rows.date(expiry, "The expiration date must look like 2026-12-31.");
        if (issued.isAfter(LocalDate.now())) throw ApiError.bad("The issue date can't be in the future.");
        if (!expires.isAfter(issued)) throw ApiError.bad("The expiration date must be after the issue date.");
        if (expires.isBefore(LocalDate.now())) throw ApiError.bad("That license has expired.");

        List<Map<String, Object>> last = db.queryForList(
                "SELECT verification_status, expiration_date FROM driver_license WHERE customer_id = ? ORDER BY license_id DESC LIMIT 1", customerId);
        if (!last.isEmpty()) {
            String st = String.valueOf(last.get(0).get("verification_status"));
            if ("Pending".equalsIgnoreCase(st)) throw ApiError.conflict("Your license is already waiting for review.");
            LocalDate exp = ((java.sql.Date) last.get(0).get("expiration_date")).toLocalDate();
            if ("Verified".equalsIgnoreCase(st) && !exp.isBefore(LocalDate.now()))
                throw ApiError.conflict("Your license is already verified.");
        }
        String frontPath = files.save(front, "licenses/" + customerId);
        String backPath = files.save(back, "licenses/" + customerId);
        Integer id = db.queryForObject(
                "INSERT INTO driver_license (customer_id, license_number, license_type, issue_date, expiration_date, license_front_photo, license_back_photo)"
              + " VALUES (?,?,?,?,?,?,?) RETURNING license_id",
                Integer.class, customerId, number.trim(), type.trim(), issued, expires, frontPath, backPath);
        return db.queryForObject(SELECT + " WHERE d.license_id = ?", (rs, i) -> map(rs), id);
    }

    @GetMapping("/api/license/me")
    public List<LicenseDto> mine(Authentication a) {
        return db.query(SELECT + " WHERE d.customer_id = ? ORDER BY d.license_id DESC", (rs, i) -> map(rs), users.customerId(a));
    }

    /** The owner or any staff member can view a license photo. side = front or back. */
    @GetMapping("/api/license/{id}/photo/{side}")
    public ResponseEntity<byte[]> photo(@PathVariable int id, @PathVariable String side, Authentication a) {
        List<Map<String, Object>> rows = db.queryForList(
                "SELECT customer_id, license_front_photo, license_back_photo FROM driver_license WHERE license_id = ?", id);
        if (rows.isEmpty()) throw ApiError.notFound("License not found.");
        int owner = (Integer) rows.get(0).get("customer_id");
        if (!users.isStaff(a) && users.customerId(a) != owner) throw ApiError.notFound("License not found.");
        String path;
        if ("front".equals(side)) path = String.valueOf(rows.get(0).get("license_front_photo"));
        else if ("back".equals(side)) path = String.valueOf(rows.get(0).get("license_back_photo"));
        else throw ApiError.bad("Use front or back.");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(FileStorage.contentType(path)))
                .header("Cache-Control", "private, no-store").body(files.read(path));
    }

    @GetMapping("/api/admin/licenses")
    public List<LicenseDto> list(@RequestParam(required = false) String status) {
        if (Rows.blank(status)) return db.query(SELECT + " ORDER BY d.license_id DESC", (rs, i) -> map(rs));
        return db.query(SELECT + " WHERE lower(d.verification_status) = lower(?) ORDER BY d.license_id DESC", (rs, i) -> map(rs), status.trim());
    }

    @PostMapping("/api/admin/licenses/{id}/verify")
    @Transactional
    public LicenseDto verify(@PathVariable int id, Authentication a) {
        int staff = users.staffId(a);
        Map<String, Object> row = pending(id);
        LocalDate exp = ((java.sql.Date) row.get("expiration_date")).toLocalDate();
        if (exp.isBefore(LocalDate.now())) throw ApiError.bad("This license has expired. Reject it instead.");
        db.update("UPDATE driver_license SET verification_status = 'Verified', verified_at = CURRENT_TIMESTAMP,"
                + " verified_by_staff_id = ?, rejection_reason = NULL WHERE license_id = ?", staff, id);
        audit.log(staff, "VERIFY", "driver_license", id, "Verified driver's license " + id);
        notifier.notifyCustomer((Integer) row.get("customer_id"), "License verified", "Your driver's license has been verified. You can now book a car.");
        return db.queryForObject(SELECT + " WHERE d.license_id = ?", (rs, i) -> map(rs), id);
    }

    @PostMapping("/api/admin/licenses/{id}/reject")
    @Transactional
    public LicenseDto reject(@PathVariable int id, @RequestBody RejectRequest r, Authentication a) {
        int staff = users.staffId(a);
        if (Rows.blank(r.reason())) throw ApiError.bad("Enter a reason so the customer knows what to fix.");
        Map<String, Object> row = pending(id);
        db.update("UPDATE driver_license SET verification_status = 'Rejected', rejection_reason = ?, verified_at = CURRENT_TIMESTAMP,"
                + " verified_by_staff_id = ? WHERE license_id = ?", r.reason().trim(), staff, id);
        audit.log(staff, "REJECT", "driver_license", id, "Rejected driver's license " + id + ": " + r.reason().trim());
        notifier.notifyCustomer((Integer) row.get("customer_id"), "License rejected", "Your license was rejected: " + r.reason().trim());
        return db.queryForObject(SELECT + " WHERE d.license_id = ?", (rs, i) -> map(rs), id);
    }

    private Map<String, Object> pending(int id) {
        List<Map<String, Object>> rows = db.queryForList(
                "SELECT customer_id, expiration_date, verification_status FROM driver_license WHERE license_id = ? FOR UPDATE", id);
        if (rows.isEmpty()) throw ApiError.notFound("License not found.");
        if (!"Pending".equalsIgnoreCase(String.valueOf(rows.get(0).get("verification_status"))))
            throw ApiError.conflict("This license has already been reviewed.");
        return rows.get(0);
    }
}
