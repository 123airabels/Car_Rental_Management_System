package com.carrental.car_rental_app.web;

import com.carrental.car_rental_app.support.AuditService;
import com.carrental.car_rental_app.support.CurrentUser;
import com.carrental.car_rental_app.support.FileStorage;
import com.carrental.car_rental_app.support.NotificationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Payments for rentals and damage fees. A customer submits a payment (status Pending) and gets a unique payment code (PAY-XXXXXX) to show or quote to the staff;
 * Finance looks the code up, checks it against the money received and confirms it (Paid, receipt issued), rejects it (Failed), or refunds it later.
 * There is no live payment-gateway call: the reference number is checked by a person.
 */
@RestController
public class PaymentController {
    private final JdbcTemplate db;
    private final CurrentUser users;
    private final AuditService audit;
    private final NotificationService notifier;
    private final FileStorage files;

    public PaymentController(JdbcTemplate db, CurrentUser users, AuditService audit, NotificationService notifier, FileStorage files) {
        this.db = db; this.users = users; this.audit = audit; this.notifier = notifier; this.files = files;
    }

    record PaymentRequest(Integer rentalId, Integer damageFeeId, BigDecimal amount, String method, String referenceNumber) {}
    record PaymentDto(int id, Integer rentalId, Integer damageFeeId, String customer, BigDecimal amount, LocalDateTime date,
                      String method, String status, String referenceNumber, String receiptNumber, String paymentCode, boolean hasProof) {}
    record Receipt(String receiptNumber, int paymentId, LocalDateTime date, String status, String customer, String customerEmail,
                   String description, BigDecimal amount, String method, String referenceNumber, String paymentCode, String receivedBy) {}
    record Balance(int rentalId, BigDecimal total, BigDecimal paid, BigDecimal pending, BigDecimal balance) {}
    private record PaymentRow(Integer rentalId, Integer damageFeeId, String status) {}

    private static final Set<String> METHODS = Set.of("Cash", "GCash", "Maya", "Card", "Bank Transfer");

    private static final String SELECT = """
        SELECT p.payment_id, p.rental_id, p.damage_fee_id, p.amount, p.payment_date, p.payment_method, p.payment_status,
               p.reference_number, p.receipt_number, p.payment_code, p.proof_path, c.customer_id, c.first_name || ' ' || c.last_name AS customer
        FROM payment p
        LEFT JOIN damage_fee f ON f.damage_fee_id = p.damage_fee_id
        LEFT JOIN damage_incident di ON di.damage_id = f.damage_id
        LEFT JOIN vehicle_inspection vi ON vi.inspection_id = di.inspection_id
        JOIN rental r ON r.rental_id = COALESCE(p.rental_id, vi.rental_id)
        JOIN customer c ON c.customer_id = r.customer_id
        """;

    private static PaymentDto map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PaymentDto(rs.getInt("payment_id"), Rows.intOrNull(rs, "rental_id"), Rows.intOrNull(rs, "damage_fee_id"),
                rs.getString("customer"), rs.getBigDecimal("amount"), Rows.ldt(rs, "payment_date"), rs.getString("payment_method"),
                Rows.up(rs.getString("payment_status")), rs.getString("reference_number"), rs.getString("receipt_number"), rs.getString("payment_code"), rs.getString("proof_path") != null);
    }

    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";   // no 0/O/1/I/L, easy to read out loud
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    private static String newCode() {
        StringBuilder sb = new StringBuilder("PAY-");
        for (int i = 0; i < 6; i++) sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        return sb.toString();
    }

    /** Accepts "pay-abc234", "ABC234" or " PAY-ABC234 " and returns PAY-ABC234. */
    private static String normalizeCode(String raw) {
        String c = raw == null ? "" : raw.trim().toUpperCase().replaceAll("\\s", "");
        if (!c.startsWith("PAY-")) c = "PAY-" + c.replaceFirst("^PAY", "").replaceFirst("^-", "");
        return c;
    }

    // ---------- customer ----------

    @GetMapping("/api/payments/me")
    public List<PaymentDto> mine(Authentication a) {
        return db.query(SELECT + " WHERE c.customer_id = ? ORDER BY p.payment_id DESC", (rs, i) -> map(rs), users.customerId(a));
    }

    @GetMapping("/api/rentals/{id}/balance")
    public Balance balance(@PathVariable int id, Authentication a) {
        if (!users.isStaff(a)) {
            int customerId = users.customerId(a);
            boolean mine = Boolean.TRUE.equals(db.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM rental WHERE rental_id = ? AND customer_id = ?)", Boolean.class, id, customerId));
            if (!mine) throw ApiError.notFound("Rental not found.");
        }
        List<BigDecimal> total = db.queryForList("SELECT COALESCE(final_cost, estimated_cost, 0) FROM rental WHERE rental_id = ?", BigDecimal.class, id);
        if (total.isEmpty()) throw ApiError.notFound("Rental not found.");
        BigDecimal paid = sum(id, null, "Paid"), pending = sum(id, null, "Pending");
        return new Balance(id, total.get(0), paid, pending, total.get(0).subtract(paid).subtract(pending));
    }

    /** The receipt for one payment. Customers can open their own; staff can open any. Issued only once the payment is confirmed. */
    @GetMapping("/api/payments/{id}/receipt")
    public Receipt receipt(@PathVariable int id, Authentication a) {
        if (!users.isStaff(a) && users.customerId(a) != customerOf(id, false))
            throw ApiError.notFound("Payment not found.");
        List<Receipt> rows = db.query("""
            SELECT p.payment_id, p.amount, p.payment_date, p.payment_method, p.payment_status, p.reference_number, p.receipt_number, p.payment_code,
                   s.first_name || ' ' || s.last_name AS received_by,
                   c.first_name || ' ' || c.last_name AS customer, c.email,
                   r.rental_id, v.make, v.model, v.plate_number, p.damage_fee_id, di.description AS damage_description
            FROM payment p
            LEFT JOIN damage_fee f ON f.damage_fee_id = p.damage_fee_id
            LEFT JOIN damage_incident di ON di.damage_id = f.damage_id
            LEFT JOIN vehicle_inspection vi ON vi.inspection_id = di.inspection_id
            JOIN rental r ON r.rental_id = COALESCE(p.rental_id, vi.rental_id)
            JOIN customer c ON c.customer_id = r.customer_id
            JOIN vehicle v ON v.vehicle_id = r.vehicle_id
            LEFT JOIN staff s ON s.staff_id = p.received_by
            WHERE p.payment_id = ?""", (rs, i) -> {
                String car = rs.getString("make") + " " + rs.getString("model") + " (" + rs.getString("plate_number") + ")";
                String what = rs.getObject("damage_fee_id") == null
                        ? "Rental #" + rs.getInt("rental_id") + " - " + car
                        : "Damage fee #" + rs.getInt("damage_fee_id") + " - " + rs.getString("damage_description") + " - Rental #" + rs.getInt("rental_id") + ", " + car;
                return new Receipt(rs.getString("receipt_number"), rs.getInt("payment_id"), Rows.ldt(rs, "payment_date"), Rows.up(rs.getString("payment_status")),
                        rs.getString("customer"), rs.getString("email"), what, rs.getBigDecimal("amount"), rs.getString("payment_method"), rs.getString("reference_number"),
                        rs.getString("payment_code"), rs.getString("received_by"));
            }, id);
        if (rows.isEmpty()) throw ApiError.notFound("Payment not found.");
        if (rows.get(0).receiptNumber() == null) throw ApiError.conflict("A receipt is issued once the payment is confirmed.");
        return rows.get(0);
    }

    @PostMapping("/api/payments")
    @Transactional
    public PaymentDto pay(@RequestBody PaymentRequest r, Authentication a) {
        int customerId = users.customerId(a);
        if (r.rentalId() != null) {
            boolean mine = Boolean.TRUE.equals(db.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM rental WHERE rental_id = ? AND customer_id = ?)", Boolean.class, r.rentalId(), customerId));
            if (!mine) throw ApiError.notFound("Rental not found.");
            // Customers pay after staff approve the booking, not when they reserve.
            String status = db.queryForObject("SELECT rental_status FROM rental WHERE rental_id = ?", String.class, r.rentalId());
            if ("Pending".equalsIgnoreCase(status)) throw ApiError.bad("You can pay once staff approve this booking. We will show the amount due and the payment deadline then.");
        } else if (r.damageFeeId() != null) {
            boolean mine = Boolean.TRUE.equals(db.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM damage_fee f JOIN damage_incident d ON d.damage_id = f.damage_id"
                  + " JOIN vehicle_inspection i ON i.inspection_id = d.inspection_id JOIN rental x ON x.rental_id = i.rental_id"
                  + " WHERE f.damage_fee_id = ? AND x.customer_id = ?)", Boolean.class, r.damageFeeId(), customerId));
            if (!mine) throw ApiError.notFound("Damage fee not found.");
        }
        return insert(r);
    }

    /** Attaches (or replaces) the proof-of-payment screenshot on one of the customer's own Pending payments. */
    @PostMapping(value = "/api/payments/{id}/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public PaymentDto uploadProof(@PathVariable int id, @RequestParam("photo") MultipartFile photo, Authentication a) {
        if (users.customerId(a) != customerOf(id, false)) throw ApiError.notFound("Payment not found.");
        PaymentRow p = lock(id);
        if (!"Pending".equalsIgnoreCase(p.status())) throw ApiError.conflict("Proof can only be added while the payment is waiting for confirmation.");
        String path = files.save(photo, "payment-proofs/" + id);
        db.update("UPDATE payment SET proof_path = ? WHERE payment_id = ?", path, id);
        return one(id);
    }

    /** The proof screenshot. The customer who paid can open it; so can staff (Finance and Admin check it before confirming). */
    @GetMapping("/api/payments/{id}/proof")
    public ResponseEntity<byte[]> proof(@PathVariable int id, Authentication a) {
        if (!users.isStaff(a) && users.customerId(a) != customerOf(id, false)) throw ApiError.notFound("Payment not found.");
        List<String> paths = db.queryForList("SELECT proof_path FROM payment WHERE payment_id = ? AND proof_path IS NOT NULL", String.class, id);
        if (paths.isEmpty()) throw ApiError.notFound("No proof of payment was attached.");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(FileStorage.contentType(paths.get(0)))).body(files.read(paths.get(0)));
    }

    // ---------- finance ----------

    @GetMapping("/api/admin/payments")
    public List<PaymentDto> list(@RequestParam(required = false) String status) {
        if (Rows.blank(status)) return db.query(SELECT + " ORDER BY p.payment_id DESC", (rs, i) -> map(rs));
        return db.query(SELECT + " WHERE lower(p.payment_status) = lower(?) ORDER BY p.payment_id DESC", (rs, i) -> map(rs), status.trim());
    }

    /** Finds a payment from the code the customer shows at the counter. */
    @GetMapping("/api/admin/payments/code/{code}")
    public PaymentDto byCode(@PathVariable String code) {
        List<PaymentDto> rows = db.query(SELECT + " WHERE p.payment_code = ?", (rs, i) -> map(rs), normalizeCode(code));
        if (rows.isEmpty()) throw ApiError.notFound("No payment found with that code.");
        return rows.get(0);
    }

    /** Records a payment received over the counter and marks it Paid straight away. */
    @PostMapping("/api/admin/payments")
    @Transactional
    public PaymentDto recordPayment(@RequestBody PaymentRequest r, Authentication a) {
        int staff = users.staffId(a);
        PaymentDto p = insert(r);
        PaymentDto paid = markPaid(p.id(), staff);
        audit.log(staff, "CREATE", "payment", p.id(), "Recorded " + p.method() + " payment of " + p.amount());
        return paid;
    }

    @PostMapping("/api/admin/payments/{id}/confirm")
    @Transactional
    public PaymentDto confirm(@PathVariable int id, Authentication a) {
        int staff = users.staffId(a);
        PaymentRow p = lock(id);
        if (!"Pending".equalsIgnoreCase(p.status())) throw ApiError.conflict("Only pending payments can be confirmed.");
        PaymentDto paid = markPaid(id, staff);
        audit.log(staff, "CONFIRM", "payment", id, "Confirmed payment " + paid.paymentCode() + " (receipt " + paid.receiptNumber() + ")");
        return paid;
    }

    @PostMapping("/api/admin/payments/{id}/reject")
    @Transactional
    public PaymentDto reject(@PathVariable int id, Authentication a) {
        int staff = users.staffId(a);
        PaymentRow p = lock(id);
        if (!"Pending".equalsIgnoreCase(p.status())) throw ApiError.conflict("Only pending payments can be rejected.");
        db.update("UPDATE payment SET payment_status = 'Failed' WHERE payment_id = ?", id);
        audit.log(staff, "REJECT", "payment", id, "Rejected payment " + id);
        PaymentDto dto = one(id);
        notifier.notifyCustomer(customerOf(id), "Payment not accepted", "We couldn't verify your payment (reference " + dto.referenceNumber() + "). Please contact us.");
        return dto;
    }

    @PostMapping("/api/admin/payments/{id}/refund")
    @Transactional
    public PaymentDto refund(@PathVariable int id, Authentication a) {
        int staff = users.staffId(a);
        PaymentRow p = lock(id);
        if (!"Paid".equalsIgnoreCase(p.status())) throw ApiError.conflict("Only paid payments can be refunded.");
        db.update("UPDATE payment SET payment_status = 'Refunded' WHERE payment_id = ?", id);
        if (p.rentalId() != null)
            db.update("UPDATE cancellation SET refund_status = 'Refunded', processed_at = CURRENT_TIMESTAMP WHERE rental_id = ?", p.rentalId());
        if (p.damageFeeId() != null)
            db.update("UPDATE damage_fee SET fee_status = 'Assessed', resolved_at = NULL WHERE damage_fee_id = ? AND fee_status = 'Paid'", p.damageFeeId());
        audit.log(staff, "REFUND", "payment", id, "Refunded payment " + id);
        notifier.notifyCustomer(customerOf(id), "Payment refunded", "Payment #" + id + " has been refunded.");
        return one(id);
    }

    // ---------- shared ----------

    /** Validates and stores a new Pending payment. Locks the rental or fee so two requests can't overpay. */
    private PaymentDto insert(PaymentRequest r) {
        boolean forRental = r.rentalId() != null, forFee = r.damageFeeId() != null;
        if (forRental == forFee) throw ApiError.bad("Choose either a rental or a damage fee to pay for.");
        if (r.amount() == null || r.amount().signum() <= 0) throw ApiError.bad("Enter an amount greater than zero.");
        String wanted = r.method() == null ? "" : r.method().trim();
        String method = METHODS.stream().filter(m -> m.equalsIgnoreCase(wanted)).findFirst()
                .orElseThrow(() -> ApiError.bad("Payment method must be Cash, GCash, Maya, Card, or Bank Transfer."));
        String ref = Rows.clean(r.referenceNumber());
        if (!method.equals("Cash") && ref == null) throw ApiError.bad("Enter the payment reference number.");

        if (ref != null && Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM payment WHERE lower(reference_number) = lower(?) AND payment_method = ? AND payment_status IN ('Pending', 'Paid'))",
                Boolean.class, ref, method)))
            throw ApiError.bad("That reference number has already been used for another payment.");

        BigDecimal due = forRental ? rentalDue(r.rentalId()) : feeDue(r.damageFeeId());
        if (r.amount().compareTo(due) > 0) throw ApiError.bad("That's more than the amount still due (" + due + ").");

        // The payment code is what the customer brings to the staff. The unique constraint backs up the existence check.
        String code;
        do { code = newCode(); } while (Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM payment WHERE payment_code = ?)", Boolean.class, code)));
        Integer id = db.queryForObject("INSERT INTO payment (rental_id, damage_fee_id, amount, payment_method, reference_number, payment_code)"
                + " VALUES (?,?,?,?,?,?) RETURNING payment_id", Integer.class, r.rentalId(), r.damageFeeId(), r.amount(), method, ref, code);
        return one(id);
    }

    private BigDecimal rentalDue(int rentalId) {
        List<String> st = db.queryForList("SELECT rental_status FROM rental WHERE rental_id = ? FOR UPDATE", String.class, rentalId);
        if (st.isEmpty()) throw ApiError.notFound("Rental not found.");
        if (List.of("Cancelled", "Rejected").contains(st.get(0))) throw ApiError.bad("This rental was " + st.get(0).toLowerCase() + ", so there is nothing to pay.");
        BigDecimal total = db.queryForObject("SELECT COALESCE(final_cost, estimated_cost, 0) FROM rental WHERE rental_id = ?", BigDecimal.class, rentalId);
        return total.subtract(sum(rentalId, null, "Pending")).subtract(sum(rentalId, null, "Paid"));
    }

    private BigDecimal feeDue(int feeId) {
        List<java.util.Map<String, Object>> f = db.queryForList("SELECT amount, fee_status FROM damage_fee WHERE damage_fee_id = ? FOR UPDATE", feeId);
        if (f.isEmpty()) throw ApiError.notFound("Damage fee not found.");
        if (!"Assessed".equalsIgnoreCase(String.valueOf(f.get(0).get("fee_status"))))
            throw ApiError.bad("This damage fee can't be paid right now (status: " + f.get(0).get("fee_status") + ").");
        BigDecimal total = (BigDecimal) f.get(0).get("amount");
        return total.subtract(sum(null, feeId, "Pending")).subtract(sum(null, feeId, "Paid"));
    }

    private BigDecimal sum(Integer rentalId, Integer feeId, String status) {
        String col = rentalId != null ? "rental_id" : "damage_fee_id";   // fixed strings, never user input
        return db.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM payment WHERE " + col + " = ? AND payment_status = ?",
                BigDecimal.class, rentalId != null ? rentalId : feeId, status);
    }

    private PaymentDto markPaid(int id, int staffId) {
        String receipt = "RCPT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%06d", id);
        db.update("UPDATE payment SET payment_status = 'Paid', payment_date = CURRENT_TIMESTAMP, receipt_number = ?, received_by = ? WHERE payment_id = ?", receipt, staffId, id);
        PaymentRow p = lock(id);
        if (p.rentalId() != null) {
            // Once the final cost is fully paid, a late fee on that rental counts as paid too.
            db.update("UPDATE rental SET late_fee_status = 'Paid' WHERE rental_id = ? AND final_cost IS NOT NULL AND late_fee > 0"
                    + " AND (SELECT COALESCE(SUM(amount), 0) FROM payment WHERE rental_id = ? AND payment_status = 'Paid') >= final_cost",
                    p.rentalId(), p.rentalId());
        }
        if (p.damageFeeId() != null) {
            db.update("UPDATE damage_fee SET fee_status = 'Paid', resolved_at = CURRENT_TIMESTAMP WHERE damage_fee_id = ? AND fee_status = 'Assessed'"
                    + " AND (SELECT COALESCE(SUM(amount), 0) FROM payment WHERE damage_fee_id = ? AND payment_status = 'Paid') >= amount",
                    p.damageFeeId(), p.damageFeeId());
        }
        notifier.notifyCustomer(customerOf(id), "Payment received", "We received your payment " + one(id).paymentCode() + ". Receipt " + receipt + ".");
        return one(id);
    }

    private PaymentRow lock(int id) {
        List<PaymentRow> rows = db.query("SELECT rental_id, damage_fee_id, payment_status FROM payment WHERE payment_id = ? FOR UPDATE",
                (rs, i) -> new PaymentRow(Rows.intOrNull(rs, "rental_id"), Rows.intOrNull(rs, "damage_fee_id"), rs.getString("payment_status")), id);
        if (rows.isEmpty()) throw ApiError.notFound("Payment not found.");
        return rows.get(0);
    }

    private PaymentDto one(int id) {
        return db.queryForObject(SELECT + " WHERE p.payment_id = ?", (rs, i) -> map(rs), id);
    }

    private int customerOf(int paymentId) { return customerOf(paymentId, true); }

    /** Owner of a payment. With mustExist=false a missing payment gives -1, so callers can answer "not found" without leaking which ids exist. */
    private int customerOf(int paymentId, boolean mustExist) {
        List<Integer> ids = db.queryForList("SELECT c.customer_id FROM (" + SELECT + " WHERE p.payment_id = ?) c", Integer.class, paymentId);
        if (!ids.isEmpty()) return ids.get(0);
        if (mustExist) throw ApiError.notFound("Payment not found.");
        return -1;
    }
}
