package com.carrental.car_rental_app.web;

import com.carrental.car_rental_app.support.AuditService;
import com.carrental.car_rental_app.support.CurrentUser;
import com.carrental.car_rental_app.support.NotificationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Damage found at return: staff record the incident and set a fee, the customer can see it and dispute it,
 * and the fee is paid through PaymentController. Fee status: Assessed -> Disputed -> Assessed/Waived -> Paid.
 */
@RestController
public class DamageController {
    private final JdbcTemplate db;
    private final CurrentUser users;
    private final AuditService audit;
    private final NotificationService notifier;

    public DamageController(JdbcTemplate db, CurrentUser users, AuditService audit, NotificationService notifier) {
        this.db = db; this.users = users; this.audit = audit; this.notifier = notifier;
    }

    record DamageRequest(String damageType, String description, String locationOnVehicle, String severity) {}
    record FeeRequest(BigDecimal amount) {}
    record DisputeRequest(String reason) {}
    record ResolveRequest(String decision, BigDecimal amount, String notes) {}
    record IncidentDto(int id, int inspectionId, int rentalId, String vehicle, String type, String description, String location,
                       String severity, String status, LocalDateTime reportedAt, Integer feeId, BigDecimal feeAmount, String feeStatus) {}
    record FeeDto(int id, int damageId, int rentalId, String vehicle, String customer, String description, BigDecimal amount,
                  BigDecimal paid, String status, String disputeReason, String resolutionNotes, LocalDateTime assessedAt) {}

    private static final String INCIDENT_SELECT = """
        SELECT d.damage_id, d.inspection_id, i.rental_id, v.make || ' ' || v.model AS vehicle, d.damage_type, d.description,
               d.location_on_vehicle, d.severity, d.status, d.reported_at, f.damage_fee_id, f.amount, f.fee_status
        FROM damage_incident d JOIN vehicle_inspection i ON i.inspection_id = d.inspection_id
        JOIN rental r ON r.rental_id = i.rental_id JOIN vehicle v ON v.vehicle_id = r.vehicle_id
        LEFT JOIN damage_fee f ON f.damage_id = d.damage_id
        """;

    private static final String FEE_SELECT = """
        SELECT f.damage_fee_id, f.damage_id, r.rental_id, r.customer_id, v.make || ' ' || v.model AS vehicle,
               c.first_name || ' ' || c.last_name AS customer, d.description, f.amount, f.fee_status, f.dispute_reason, f.resolution_notes, f.assessment_date,
               (SELECT COALESCE(SUM(p.amount), 0) FROM payment p WHERE p.damage_fee_id = f.damage_fee_id AND p.payment_status = 'Paid') AS paid
        FROM damage_fee f JOIN damage_incident d ON d.damage_id = f.damage_id
        JOIN vehicle_inspection i ON i.inspection_id = d.inspection_id
        JOIN rental r ON r.rental_id = i.rental_id JOIN vehicle v ON v.vehicle_id = r.vehicle_id
        JOIN customer c ON c.customer_id = r.customer_id
        """;

    private static IncidentDto incident(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new IncidentDto(rs.getInt("damage_id"), rs.getInt("inspection_id"), rs.getInt("rental_id"), rs.getString("vehicle"),
                rs.getString("damage_type"), rs.getString("description"), rs.getString("location_on_vehicle"), rs.getString("severity"),
                Rows.up(rs.getString("status")), Rows.ldt(rs, "reported_at"), Rows.intOrNull(rs, "damage_fee_id"),
                rs.getBigDecimal("amount"), Rows.up(rs.getString("fee_status")));
    }

    private static FeeDto fee(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new FeeDto(rs.getInt("damage_fee_id"), rs.getInt("damage_id"), rs.getInt("rental_id"), rs.getString("vehicle"),
                rs.getString("customer"), rs.getString("description"), rs.getBigDecimal("amount"), rs.getBigDecimal("paid"),
                Rows.up(rs.getString("fee_status")), rs.getString("dispute_reason"), rs.getString("resolution_notes"), Rows.ldt(rs, "assessment_date"));
    }

    // ---------- staff ----------

    @PostMapping("/api/admin/inspections/{inspectionId}/damages")
    @Transactional
    public IncidentDto report(@PathVariable int inspectionId, @RequestBody DamageRequest r, Authentication a) {
        int staff = users.staffId(a);
        if (Rows.blank(r.description())) throw ApiError.bad("Describe the damage.");
        boolean exists = Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM vehicle_inspection WHERE inspection_id = ?)", Boolean.class, inspectionId));
        if (!exists) throw ApiError.notFound("Inspection not found.");
        Integer id = db.queryForObject(
                "INSERT INTO damage_incident (inspection_id, damage_type, description, location_on_vehicle, severity) VALUES (?,?,?,?,?) RETURNING damage_id",
                Integer.class, inspectionId, Rows.clean(r.damageType()), r.description().trim(), Rows.clean(r.locationOnVehicle()), Rows.clean(r.severity()));
        audit.log(staff, "CREATE", "damage_incident", id, "Recorded damage on inspection " + inspectionId);
        return db.queryForObject(INCIDENT_SELECT + " WHERE d.damage_id = ?", (rs, i) -> incident(rs), id);
    }

    /** Sets (or changes) the fee for a damage incident. An amount of 0 waives it. Not allowed once it has been paid. */
    @PutMapping("/api/admin/damages/{damageId}/fee")
    @Transactional
    public FeeDto assess(@PathVariable int damageId, @RequestBody FeeRequest r, Authentication a) {
        int staff = users.staffId(a);
        if (r.amount() == null || r.amount().signum() < 0) throw ApiError.bad("Enter a fee of zero or more.");
        List<Map<String, Object>> customer = db.queryForList(
                "SELECT r.customer_id FROM damage_incident d JOIN vehicle_inspection i ON i.inspection_id = d.inspection_id"
              + " JOIN rental r ON r.rental_id = i.rental_id WHERE d.damage_id = ?", damageId);
        if (customer.isEmpty()) throw ApiError.notFound("Damage record not found.");
        String status = r.amount().signum() == 0 ? "Waived" : "Assessed";

        List<Map<String, Object>> existing = db.queryForList("SELECT damage_fee_id, fee_status FROM damage_fee WHERE damage_id = ? FOR UPDATE", damageId);
        Integer feeId;
        if (existing.isEmpty()) {
            feeId = db.queryForObject("INSERT INTO damage_fee (damage_id, amount, fee_status, resolved_at)"
                    + " VALUES (?,?,?, CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END) RETURNING damage_fee_id",
                    Integer.class, damageId, r.amount(), status, r.amount().signum() == 0);
        } else {
            feeId = (Integer) existing.get(0).get("damage_fee_id");
            String current = String.valueOf(existing.get(0).get("fee_status"));
            if ("Paid".equalsIgnoreCase(current))
                throw ApiError.conflict("This fee has already been paid and can't be changed.");
            if ("Disputed".equalsIgnoreCase(current))
                throw ApiError.conflict("The customer has disputed this fee. Resolve the dispute instead of changing the amount.");
            BigDecimal paid = db.queryForObject(
                    "SELECT COALESCE(SUM(amount), 0) FROM payment WHERE damage_fee_id = ? AND payment_status IN ('Pending','Paid')", BigDecimal.class, feeId);
            if (paid.compareTo(r.amount()) > 0) throw ApiError.conflict("Payments already received (" + paid + ") are more than the new amount.");
            db.update("UPDATE damage_fee SET amount = ?, fee_status = ?, assessment_date = CURRENT_TIMESTAMP,"
                    + " resolved_at = CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END WHERE damage_fee_id = ?",
                    r.amount(), status, r.amount().signum() == 0, feeId);
        }
        audit.log(staff, "UPDATE", "damage_fee", feeId, "Set damage fee for incident " + damageId + " to " + r.amount() + " (" + status + ")");
        notifier.notifyCustomer((Integer) customer.get(0).get("customer_id"), "Damage fee",
                "A damage fee of " + r.amount() + " was recorded for your rental. Status: " + status + ".");
        return db.queryForObject(FEE_SELECT + " WHERE f.damage_fee_id = ?", (rs, i) -> fee(rs), feeId);
    }

    @PostMapping("/api/admin/damages/{damageId}/resolve")
    @Transactional
    public IncidentDto resolve(@PathVariable int damageId, Authentication a) {
        int staff = users.staffId(a);
        List<String> feeStatus = db.queryForList("SELECT fee_status FROM damage_fee WHERE damage_id = ?", String.class, damageId);
        if (!feeStatus.isEmpty() && !List.of("Paid", "Waived").contains(feeStatus.get(0)))
            throw ApiError.conflict("This damage can't be closed while its fee is " + feeStatus.get(0).toLowerCase() + ". The fee must be paid or waived first.");
        int n = db.update("UPDATE damage_incident SET status = 'Resolved', resolved_at = CURRENT_TIMESTAMP WHERE damage_id = ? AND status <> 'Resolved'", damageId);
        if (n == 0) throw ApiError.conflict("That damage record doesn't exist or is already resolved.");
        audit.log(staff, "UPDATE", "damage_incident", damageId, "Marked damage " + damageId + " as resolved");
        return db.queryForObject(INCIDENT_SELECT + " WHERE d.damage_id = ?", (rs, i) -> incident(rs), damageId);
    }

    @GetMapping("/api/admin/damages")
    public List<IncidentDto> incidents(@RequestParam(required = false) String status) {
        if (Rows.blank(status)) return db.query(INCIDENT_SELECT + " ORDER BY d.damage_id DESC", (rs, i) -> incident(rs));
        return db.query(INCIDENT_SELECT + " WHERE lower(d.status) = lower(?) ORDER BY d.damage_id DESC", (rs, i) -> incident(rs), status.trim());
    }

    @GetMapping("/api/admin/damage-fees")
    public List<FeeDto> fees(@RequestParam(required = false) String status) {
        if (Rows.blank(status)) return db.query(FEE_SELECT + " ORDER BY f.damage_fee_id DESC", (rs, i) -> fee(rs));
        return db.query(FEE_SELECT + " WHERE lower(f.fee_status) = lower(?) ORDER BY f.damage_fee_id DESC", (rs, i) -> fee(rs), status.trim());
    }

    /**
     * Staff decision on a disputed fee: UPHOLD keeps the amount, ADJUST sets a new amount (0 waives it), WAIVE removes it.
     * The decision and the reasoning are stored with the fee so the customer can read them.
     */
    @PostMapping("/api/admin/damage-fees/{id}/resolve-dispute")
    @Transactional
    public FeeDto resolveDispute(@PathVariable int id, @RequestBody ResolveRequest r, Authentication a) {
        int staff = users.staffId(a);
        String decision = r.decision() == null ? "" : r.decision().trim().toUpperCase();
        if (!List.of("UPHOLD", "ADJUST", "WAIVE").contains(decision)) throw ApiError.bad("Choose uphold, adjust or waive.");
        if (Rows.blank(r.notes())) throw ApiError.bad("Write the reason for your decision. The customer will see it.");
        List<Map<String, Object>> rows = db.queryForList(
                "SELECT f.fee_status, f.amount, r.customer_id FROM damage_fee f JOIN damage_incident d ON d.damage_id = f.damage_id"
              + " JOIN vehicle_inspection i ON i.inspection_id = d.inspection_id JOIN rental r ON r.rental_id = i.rental_id"
              + " WHERE f.damage_fee_id = ? FOR UPDATE OF f", id);
        if (rows.isEmpty()) throw ApiError.notFound("Damage fee not found.");
        if (!"Disputed".equalsIgnoreCase(String.valueOf(rows.get(0).get("fee_status"))))
            throw ApiError.conflict("Only a disputed fee can be resolved this way.");
        BigDecimal amount = (BigDecimal) rows.get(0).get("amount");
        if ("WAIVE".equals(decision)) amount = BigDecimal.ZERO;
        if ("ADJUST".equals(decision)) {
            if (r.amount() == null || r.amount().signum() < 0) throw ApiError.bad("Enter the adjusted fee (zero or more).");
            if (r.amount().compareTo(amount) > 0) throw ApiError.bad("An adjusted fee can't be higher than the original (" + amount + ").");
            amount = r.amount();
        }
        BigDecimal pending = db.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM payment WHERE damage_fee_id = ? AND payment_status IN ('Pending','Paid')", BigDecimal.class, id);
        if (pending.compareTo(amount) > 0) throw ApiError.conflict("Payments already received (" + pending + ") are more than the new amount.");
        boolean waived = amount.signum() == 0;
        db.update("UPDATE damage_fee SET amount = ?, fee_status = ?, resolution_notes = ?, resolved_at = CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END"
                + " WHERE damage_fee_id = ?", amount, waived ? "Waived" : "Assessed", r.notes().trim(), waived, id);
        audit.log(staff, "UPDATE", "damage_fee", id, "Dispute on damage fee " + id + " decided: " + decision + ", amount now " + amount);
        notifier.notifyCustomer((Integer) rows.get(0).get("customer_id"), "Damage fee dispute decided",
                "Your dispute was reviewed. Decision: " + decision.toLowerCase() + ". The fee is now " + amount + ".");
        return db.queryForObject(FEE_SELECT + " WHERE f.damage_fee_id = ?", (rs, i) -> fee(rs), id);
    }

    // ---------- customer ----------

    @GetMapping("/api/damage-fees/me")
    public List<FeeDto> mine(Authentication a) {
        return db.query(FEE_SELECT + " WHERE r.customer_id = ? ORDER BY f.damage_fee_id DESC", (rs, i) -> fee(rs), users.customerId(a));
    }

    @PostMapping("/api/damage-fees/{id}/dispute")
    @Transactional
    public FeeDto dispute(@PathVariable int id, @RequestBody DisputeRequest r, Authentication a) {
        int customerId = users.customerId(a);
        if (Rows.blank(r.reason())) throw ApiError.bad("Tell us why you disagree with this fee.");
        List<Map<String, Object>> rows = db.queryForList(
                "SELECT f.fee_status, f.resolution_notes FROM damage_fee f JOIN damage_incident d ON d.damage_id = f.damage_id"
              + " JOIN vehicle_inspection i ON i.inspection_id = d.inspection_id JOIN rental r ON r.rental_id = i.rental_id"
              + " WHERE f.damage_fee_id = ? AND r.customer_id = ? FOR UPDATE OF f", id, customerId);
        if (rows.isEmpty()) throw ApiError.notFound("Damage fee not found.");
        if (!"Assessed".equalsIgnoreCase(String.valueOf(rows.get(0).get("fee_status"))))
            throw ApiError.conflict("Only an unpaid, assessed fee can be disputed.");
        if (rows.get(0).get("resolution_notes") != null) throw ApiError.conflict("This fee was already reviewed after a dispute.");
        if (r.reason().length() > 2000) throw ApiError.bad("Please keep your reason under 2000 characters.");
        db.update("UPDATE damage_fee SET fee_status = 'Disputed', dispute_reason = ? WHERE damage_fee_id = ?", r.reason().trim(), id);
        return db.queryForObject(FEE_SELECT + " WHERE f.damage_fee_id = ?", (rs, i) -> fee(rs), id);
    }
}
