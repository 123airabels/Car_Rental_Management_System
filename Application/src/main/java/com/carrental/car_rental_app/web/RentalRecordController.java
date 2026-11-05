package com.carrental.car_rental_app.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * The full transaction record of one rental in one response: dates, pickup and return, status, estimated and final cost,
 * the terms the customer accepted, and cancellation details. Customers see their own; staff (incl. Finance) see any.
 */
@RestController
public class RentalRecordController {
    private final JdbcTemplate db;

    public RentalRecordController(JdbcTemplate db) { this.db = db; }

    record Party(String name, String email, String phone) {}
    record Car(String name, String plateNumber, Integer year, String category) {}
    record Dates(LocalDateTime pickup, LocalDateTime returnBy, long days) {}
    record Cost(BigDecimal dailyRate, BigDecimal estimated, BigDecimal pickupFee, BigDecimal returnFee, BigDecimal lateFee,
                String lateFeeStatus, BigDecimal finalCost, BigDecimal paid) {}
    /** One side of the handover. actual is null until the pickup or return has really happened. */
    record Handover(LocalDateTime scheduled, LocalDateTime actual, BigDecimal mileage, String fuelLevel, String notes) {}
    record TermsInfo(String version, boolean accepted, LocalDateTime acceptedAt, String text) {}
    record Cancel(String type, String by, String reason, LocalDateTime at, boolean refundEligible, BigDecimal refundAmount, String refundStatus) {}
    record RentalRecord(int id, String status, LocalDateTime createdAt, LocalDateTime approvedAt, Party customer, Car vehicle,
                        Dates dates, Cost cost, Handover pickup, Handover returned, TermsInfo terms, Cancel cancellation) {}
    record CurrentTerms(String version, String text) {}

    /** Public: the terms a customer agrees to when booking. */
    @GetMapping("/api/terms")
    public CurrentTerms currentTerms() { return new CurrentTerms(TermsController.VERSION, TermsController.TEXT); }

    @GetMapping("/api/rentals/{id}/record")
    public RentalRecord mine(@PathVariable int id, Authentication a) { return build(id, a.getName(), false); }

    @GetMapping("/api/admin/rentals/{id}/record")
    public RentalRecord any(@PathVariable int id) { return build(id, null, true); }

    private RentalRecord build(int id, String ownerEmail, boolean staff) {
        List<RentalRecord> rows = db.query("""
            SELECT r.rental_id, r.rental_status, r.created_at, r.approved_at, r.pickup_datetime, r.return_datetime,
                   r.estimated_cost, r.pickup_fee, r.return_fee, r.late_fee, r.late_fee_status, r.final_cost,
                   c.first_name || ' ' || c.last_name AS cname, c.email, c.phone_number,
                   v.make || ' ' || v.model AS vname, v.plate_number, v.year, vc.category_name,
                   rr.rate_amount + v.unit_rate_adjustment AS daily,
                   (SELECT COALESCE(SUM(p.amount), 0) FROM payment p WHERE p.rental_id = r.rental_id AND p.payment_status = 'Paid') AS paid,
                   r.actual_return_datetime
            FROM rental r JOIN customer c ON c.customer_id = r.customer_id JOIN vehicle v ON v.vehicle_id = r.vehicle_id
                 JOIN vehicle_category vc ON vc.category_id = v.category_id JOIN rental_rate rr ON rr.rate_id = r.rate_id
            WHERE r.rental_id = ? AND (?::text IS NULL OR lower(c.email) = lower(?::text))
            """, (rs, i) -> {
                LocalDateTime pickup = Rows.ldt(rs, "pickup_datetime"), back = Rows.ldt(rs, "return_datetime");
                Cost cost = new Cost(rs.getBigDecimal("daily"), rs.getBigDecimal("estimated_cost"), rs.getBigDecimal("pickup_fee"),
                        rs.getBigDecimal("return_fee"), rs.getBigDecimal("late_fee"), Rows.up(rs.getString("late_fee_status")),
                        rs.getBigDecimal("final_cost"), rs.getBigDecimal("paid"));
                Handover out = handover(id, "Pickup", pickup, null);
                Handover in = handover(id, "Return", back, Rows.ldt(rs, "actual_return_datetime"));
                return new RentalRecord(rs.getInt("rental_id"), Rows.up(rs.getString("rental_status")), Rows.ldt(rs, "created_at"),
                        Rows.ldt(rs, "approved_at"),
                        new Party(rs.getString("cname"), rs.getString("email"), rs.getString("phone_number")),
                        new Car(rs.getString("vname"), staff ? rs.getString("plate_number") : null, Rows.intOrNull(rs, "year"), rs.getString("category_name")),
                        new Dates(pickup, back, ChronoUnit.DAYS.between(pickup.toLocalDate(), back.toLocalDate())),
                        cost, out, in, terms(id), cancellation(id, staff));
            }, id, ownerEmail, ownerEmail);
        if (rows.isEmpty()) throw ApiError.notFound("Rental not found.");
        return rows.get(0);
    }

    /** The inspection done at pickup or return. Its time is when the car really changed hands. */
    private Handover handover(int rentalId, String type, LocalDateTime scheduled, LocalDateTime recordedActual) {
        List<Handover> rows = db.query(
                "SELECT inspection_datetime, mileage, fuel_level, condition_notes FROM vehicle_inspection"
              + " WHERE rental_id = ? AND inspection_type = ? ORDER BY inspection_id DESC LIMIT 1",
                (rs, i) -> new Handover(scheduled, recordedActual != null ? recordedActual : Rows.ldt(rs, "inspection_datetime"),
                        rs.getBigDecimal("mileage"), rs.getString("fuel_level"), rs.getString("condition_notes")), rentalId, type);
        return rows.isEmpty() ? new Handover(scheduled, recordedActual, null, null, null) : rows.get(0);
    }

    private TermsInfo terms(int rentalId) {
        List<TermsInfo> rows = db.query("SELECT terms_version, terms_text, accepted, accepted_at FROM rental_terms WHERE rental_id = ?",
                (rs, i) -> new TermsInfo(rs.getString("terms_version"), rs.getBoolean("accepted"), Rows.ldt(rs, "accepted_at"), rs.getString("terms_text")), rentalId);
        return rows.isEmpty() ? new TermsInfo(TermsController.VERSION, false, null, TermsController.TEXT) : rows.get(0);
    }

    /** Set for Cancelled and Rejected rentals (both are stored in the cancellation table); null otherwise. */
    private Cancel cancellation(int rentalId, boolean staff) {
        List<Cancel> rows = db.query("""
            SELECT x.cancelled_at, x.cancellation_reason, x.refund_eligible, x.refund_amount, x.refund_status,
                   x.cancelled_by_customer_id, s.first_name || ' ' || s.last_name AS staff_name, s.role AS staff_role, r.rental_status
            FROM cancellation x JOIN rental r ON r.rental_id = x.rental_id LEFT JOIN staff s ON s.staff_id = x.cancelled_by_staff_id
            WHERE x.rental_id = ?
            """, (rs, i) -> new Cancel(Rows.up(rs.getString("rental_status")),
                        rs.getObject("cancelled_by_customer_id") != null ? "Customer"
                                : (staff ? rs.getString("staff_name") + " (" + rs.getString("staff_role") + ")" : rs.getString("staff_role")),   // customers see the role: Admin or Staff
                        rs.getString("cancellation_reason"), Rows.ldt(rs, "cancelled_at"), rs.getBoolean("refund_eligible"),
                        rs.getBigDecimal("refund_amount"), Rows.up(rs.getString("refund_status"))), rentalId);
        return rows.isEmpty() ? null : rows.get(0);
    }
}
