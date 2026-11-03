package com.carrental.car_rental_app.web;

import com.carrental.car_rental_app.support.AuditService;
import com.carrental.car_rental_app.support.BookingService;
import com.carrental.car_rental_app.support.CurrentUser;
import com.carrental.car_rental_app.support.NotificationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Staff side of a rental: review a request, hand the car over, take it back.
 * Status flow: Pending -> Approved -> Active -> Completed (or Rejected / Cancelled).
 */
@RestController
@RequestMapping("/api/admin/rentals")
public class StaffRentalController {
    private final JdbcTemplate db;
    private final CurrentUser users;
    private final AuditService audit;
    private final NotificationService notifier;
    private final BookingService bookings;
    private final long graceMinutes;
    private final BigDecimal lateMultiplier;

    public StaffRentalController(JdbcTemplate db, CurrentUser users, AuditService audit, NotificationService notifier, BookingService bookings,
                                 @Value("${app.rental.late-grace-minutes:60}") long graceMinutes,
                                 @Value("${app.rental.late-fee-multiplier:1.0}") BigDecimal lateMultiplier) {
        this.db = db; this.users = users; this.audit = audit; this.notifier = notifier; this.bookings = bookings;
        this.graceMinutes = graceMinutes; this.lateMultiplier = lateMultiplier;
    }

    record AdminRentalDto(int id, int customerId, String customer, String customerEmail, String customerPhone, int vehicleId,
                          String vehicle, String plateNumber, LocalDateTime pickup, LocalDateTime returnBy, LocalDateTime actualReturn,
                          BigDecimal estimatedCost, BigDecimal lateFee, BigDecimal finalCost, BigDecimal paid, String status,
                          String licenseStatus, String reason) {}
    record BookingRequest(Integer customerId, Integer vehicleId, String start, String end, Boolean approve) {}
    record ChangeRequest(Integer vehicleId, String start, String end) {}
    record Check(boolean available, String reason) {}
    record ReasonRequest(String reason, BigDecimal refundAmount) {}
    record HandoverRequest(BigDecimal mileage, String fuelLevel, String conditionNotes, BigDecimal pickupFee, BigDecimal returnFee) {}
    record HandoverResult(AdminRentalDto rental, int inspectionId, BigDecimal lateFee) {}
    private record Locked(int customerId, int vehicleId, String status, LocalDateTime pickup, LocalDateTime returnBy,
                          BigDecimal estimated, BigDecimal pickupFee, BigDecimal returnFee) {}

    private static final String SELECT = """
        SELECT r.rental_id, r.customer_id, c.first_name || ' ' || c.last_name AS customer, c.email, c.phone_number,
               r.vehicle_id, v.make || ' ' || v.model AS vehicle, v.plate_number,
               r.pickup_datetime, r.return_datetime, r.actual_return_datetime,
               r.estimated_cost, r.late_fee, r.final_cost, r.rental_status,
               (SELECT COALESCE(SUM(p.amount), 0) FROM payment p WHERE p.rental_id = r.rental_id AND p.payment_status = 'Paid') AS paid,
               (SELECT d.verification_status FROM driver_license d WHERE d.customer_id = r.customer_id ORDER BY d.license_id DESC LIMIT 1) AS license_status,
               (SELECT x.cancellation_reason FROM cancellation x WHERE x.rental_id = r.rental_id ORDER BY x.cancellation_id DESC LIMIT 1) AS reason
        FROM rental r JOIN customer c ON c.customer_id = r.customer_id JOIN vehicle v ON v.vehicle_id = r.vehicle_id
        """;

    private static AdminRentalDto map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AdminRentalDto(rs.getInt("rental_id"), rs.getInt("customer_id"), rs.getString("customer"), rs.getString("email"),
                rs.getString("phone_number"), rs.getInt("vehicle_id"), rs.getString("vehicle"), rs.getString("plate_number"),
                Rows.ldt(rs, "pickup_datetime"), Rows.ldt(rs, "return_datetime"), Rows.ldt(rs, "actual_return_datetime"),
                rs.getBigDecimal("estimated_cost"), rs.getBigDecimal("late_fee"), rs.getBigDecimal("final_cost"),
                rs.getBigDecimal("paid"), Rows.up(rs.getString("rental_status")), Rows.up(rs.getString("license_status")), rs.getString("reason"));
    }

    /** All bookings, newest first. Filters: status, vehicleId, q (customer name, email or plate number). */
    @GetMapping
    public List<AdminRentalDto> list(@RequestParam(required = false) String status,
                                     @RequestParam(required = false) Integer vehicleId,
                                     @RequestParam(required = false) String q) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1=1");
        List<Object> args = new java.util.ArrayList<>();
        if (!Rows.blank(status)) { sql.append(" AND lower(r.rental_status) = lower(?)"); args.add(status.trim()); }
        if (vehicleId != null) { sql.append(" AND r.vehicle_id = ?"); args.add(vehicleId); }
        if (!Rows.blank(q)) {
            sql.append(" AND (lower(c.first_name || ' ' || c.last_name) LIKE ? OR lower(c.email) LIKE ? OR lower(v.plate_number) LIKE ?)");
            String like = "%" + q.trim().toLowerCase() + "%";
            args.add(like); args.add(like); args.add(like);
        }
        sql.append(" ORDER BY r.rental_id DESC");
        return db.query(sql.toString(), (rs, i) -> map(rs), args.toArray());
    }

    @GetMapping("/{id}")
    public AdminRentalDto one(@PathVariable int id) {
        List<AdminRentalDto> rows = db.query(SELECT + " WHERE r.rental_id = ?", (rs, i) -> map(rs), id);
        if (rows.isEmpty()) throw ApiError.notFound("Rental not found.");
        return rows.get(0);
    }

    /** Would this booking fit these dates (and this car)? Its own current dates don't count as a clash. Read-only. */
    @GetMapping("/{id}/availability")
    public Check availability(@PathVariable int id, @RequestParam(required = false) Integer vehicleId, @RequestParam String start, @RequestParam String end) {
        AdminRentalDto cur = one(id);
        int car = vehicleId != null ? vehicleId : cur.vehicleId();
        LocalDate s = Rows.date(start, "Enter the pickup date as 2026-12-31."), e = Rows.date(end, "Enter the return date as 2026-12-31.");
        bookings.checkDates(s, e, s.equals(cur.pickup().toLocalDate()));
        String clash = bookings.conflict(car, s, e, id, true);
        return new Check(clash == null, clash);
    }

    /** Staff books a car for a customer (phone or walk-in). Starts Pending; set approve=true to approve it straight away. */
    @PostMapping
    @Transactional
    public AdminRentalDto create(@RequestBody BookingRequest req, Authentication a) {
        int staff = users.staffId(a);
        if (req.customerId() == null) throw ApiError.bad("Choose a customer.");
        if (req.vehicleId() == null) throw ApiError.bad("Choose a vehicle.");
        List<String> acct = db.queryForList("SELECT account_status FROM customer WHERE customer_id = ?", String.class, req.customerId());
        if (acct.isEmpty()) throw ApiError.notFound("That customer doesn't exist.");
        if (!"Active".equalsIgnoreCase(acct.get(0))) throw ApiError.bad("Only active customer accounts can have bookings.");
        LocalDate start = Rows.date(req.start(), "Choose a pickup date and a return date."), end = Rows.date(req.end(), "Choose a pickup date and a return date.");
        bookings.checkDates(start, end, false);

        bookings.lockVehicle(req.vehicleId());
        String clash = bookings.conflict(req.vehicleId(), start, end, null, true);
        if (clash != null) throw ApiError.conflict(clash);
        var q = bookings.quote(req.vehicleId(), start, end, null);
        boolean approve = Boolean.TRUE.equals(req.approve());
        if (approve) requireLicense(req.customerId(), end);

        Integer id = db.queryForObject(
                "INSERT INTO rental (customer_id, vehicle_id, pickup_datetime, return_datetime, rate_id, estimated_cost, rental_status, approved_at)"
              + " VALUES (?,?,?,?,?,?,?,?) RETURNING rental_id",
                Integer.class, req.customerId(), req.vehicleId(), start.atStartOfDay(), end.atStartOfDay(), q.rateId(), q.estimate(),
                approve ? "Approved" : "Pending", approve ? LocalDateTime.now() : null);
        audit.log(staff, "CREATE", "rental", id, "Created " + (approve ? "approved " : "") + "booking " + id + " for customer " + req.customerId()
                + ", vehicle " + req.vehicleId() + ", " + start + " to " + end);
        notifier.notifyCustomer(req.customerId(), approve ? "Booking approved" : "Booking received",
                "Booking #" + id + " was made for you from " + start + " to " + end + (approve ? " and is approved." : "; it is waiting for approval."));
        return one(id);
    }

    /**
     * Changes the car and/or dates of a Pending or Approved booking. An approved booking stays approved
     * (staff are making the change) as long as the license still covers the new return date.
     */
    @PutMapping("/{id}")
    @Transactional
    public AdminRentalDto update(@PathVariable int id, @RequestBody ChangeRequest req, Authentication a) {
        int staff = users.staffId(a);
        Locked r = lock(id);
        if (!List.of("Pending", "Approved").contains(r.status()))
            throw ApiError.conflict("A rental that is " + r.status().toLowerCase() + " can't be changed.");
        int vehicleId = req.vehicleId() != null ? req.vehicleId() : r.vehicleId();
        LocalDate oldStart = r.pickup().toLocalDate();
        LocalDate start = Rows.blank(req.start()) ? oldStart : Rows.date(req.start(), "Enter the pickup date as 2026-12-31.");
        LocalDate end = Rows.blank(req.end()) ? r.returnBy().toLocalDate() : Rows.date(req.end(), "Enter the return date as 2026-12-31.");
        bookings.checkDates(start, end, start.equals(oldStart));   // an unchanged start date may already be today or earlier

        bookings.lockVehicle(vehicleId);
        String clash = bookings.conflict(vehicleId, start, end, id, true);
        if (clash != null) throw ApiError.conflict(clash);
        if ("Approved".equalsIgnoreCase(r.status())) requireLicense(r.customerId(), end);
        Integer rateId = db.queryForObject("SELECT rate_id FROM rental WHERE rental_id = ?", Integer.class, id);
        var q = bookings.quote(vehicleId, start, end, vehicleId == r.vehicleId() ? rateId : null);

        db.update("UPDATE rental SET vehicle_id = ?, pickup_datetime = ?, return_datetime = ?, estimated_cost = ?, rate_id = ? WHERE rental_id = ?",
                vehicleId, start.atStartOfDay(), end.atStartOfDay(), q.estimate(), q.rateId(), id);
        audit.log(staff, "UPDATE", "rental", id, "Changed booking " + id + " to vehicle " + vehicleId + ", " + start + " to " + end);
        notifier.notifyCustomer(r.customerId(), "Booking changed", "Your booking #" + id + " was changed: " + start + " to " + end + ".");
        return one(id);
    }

    /** Needs a verified license that is still valid on the last day of the rental. */
    @PostMapping("/{id}/approve")
    @Transactional
    public AdminRentalDto approve(@PathVariable int id, Authentication a) {
        int staff = users.staffId(a);
        Locked r = lock(id);
        if (!"Pending".equalsIgnoreCase(r.status())) throw ApiError.conflict("Only pending rentals can be approved.");
        requireLicense(r.customerId(), r.returnBy().toLocalDate());
        // Re-check at approval time: the car may have gone into maintenance or been retired since the request was made.
        bookings.lockVehicle(r.vehicleId());
        String clash = bookings.conflict(r.vehicleId(), r.pickup().toLocalDate(), r.returnBy().toLocalDate(), id, true);
        if (clash != null) throw ApiError.conflict(clash);
        db.update("UPDATE rental SET rental_status = 'Approved', approved_at = CURRENT_TIMESTAMP WHERE rental_id = ?", id);
        audit.log(staff, "APPROVE", "rental", id, "Approved rental " + id);
        notifier.notifyCustomer(r.customerId(), "Booking approved", "Your booking #" + id + " has been approved.");
        return one(id);
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public AdminRentalDto reject(@PathVariable int id, @RequestBody ReasonRequest req, Authentication a) {
        return close(id, req, a, "Rejected", List.of("Pending"));
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public AdminRentalDto cancel(@PathVariable int id, @RequestBody ReasonRequest req, Authentication a) {
        return close(id, req, a, "Cancelled", List.of("Pending", "Approved"));
    }

    private AdminRentalDto close(int id, ReasonRequest req, Authentication a, String newStatus, List<String> allowedFrom) {
        int staff = users.staffId(a);
        Locked r = lock(id);
        if (allowedFrom.stream().noneMatch(s -> s.equalsIgnoreCase(r.status())))
            throw ApiError.conflict("A rental that is " + r.status().toLowerCase() + " can't be " + newStatus.toLowerCase() + ".");
        if (Rows.blank(req.reason())) throw ApiError.bad("Enter a reason.");
        boolean refund = req.refundAmount() != null && req.refundAmount().signum() > 0;
        if (req.refundAmount() != null && req.refundAmount().signum() < 0) throw ApiError.bad("The refund can't be negative.");
        db.update("INSERT INTO cancellation (rental_id, cancelled_by_staff_id, cancellation_reason, refund_eligible, refund_amount, refund_status)"
                + " VALUES (?,?,?,?,?,?)", id, staff, req.reason().trim(), refund, refund ? req.refundAmount() : null, refund ? "Pending" : null);
        db.update("UPDATE rental SET rental_status = ? WHERE rental_id = ?", newStatus, id);
        audit.log(staff, newStatus.equals("Rejected") ? "REJECT" : "CANCEL", "rental", id, newStatus + " rental " + id + ": " + req.reason().trim());
        notifier.notifyCustomer(r.customerId(), "Booking " + newStatus.toLowerCase(), "Your booking #" + id + " was " + newStatus.toLowerCase() + ": " + req.reason().trim());
        return one(id);
    }

    /** Hands the car to the customer: rental becomes Active, vehicle becomes Rented, and a Pickup inspection is recorded. */
    @PostMapping("/{id}/pickup")
    @Transactional
    public HandoverResult pickup(@PathVariable int id, @RequestBody HandoverRequest req, Authentication a) {
        int staff = users.staffId(a);
        Locked r = lock(id);
        if (!"Approved".equalsIgnoreCase(r.status())) throw ApiError.conflict("Only approved rentals can be handed over.");
        if (r.pickup().toLocalDate().isAfter(LocalDate.now()))
            throw ApiError.bad("This rental starts on " + r.pickup().toLocalDate() + ". The car can't be handed over before then.");
        if (req.pickupFee() != null && req.pickupFee().signum() < 0) throw ApiError.bad("The pickup fee can't be negative.");

        var v = db.queryForList("SELECT status, mileage FROM vehicle WHERE vehicle_id = ? FOR UPDATE", r.vehicleId()).get(0);
        if (!"Available".equalsIgnoreCase(String.valueOf(v.get("status"))))
            throw ApiError.conflict("The vehicle isn't available right now (status: " + v.get("status") + ").");
        BigDecimal recorded = (BigDecimal) v.get("mileage");
        BigDecimal mileage = req.mileage() != null ? req.mileage() : recorded;
        if (mileage != null && mileage.signum() < 0) throw ApiError.bad("Mileage can't be negative.");
        if (mileage != null && recorded != null && mileage.compareTo(recorded) < 0)
            throw ApiError.bad("That mileage is lower than the vehicle's recorded mileage (" + recorded + ").");

        int inspectionId = inspection(id, "Pickup", mileage, req.fuelLevel(), req.conditionNotes(), staff);
        db.update("UPDATE rental SET rental_status = 'Active', pickup_fee = COALESCE(?::numeric, pickup_fee) WHERE rental_id = ?", req.pickupFee(), id);
        db.update("UPDATE vehicle SET status = 'Rented', mileage = COALESCE(?::numeric, mileage) WHERE vehicle_id = ?", mileage, r.vehicleId());
        audit.log(staff, "STATUS", "vehicle", r.vehicleId(), "Vehicle " + r.vehicleId() + " status Available -> Rented (rental " + id + ")");
        audit.log(staff, "PICKUP", "rental", id, "Handed over vehicle " + r.vehicleId() + " for rental " + id);
        return new HandoverResult(one(id), inspectionId, BigDecimal.ZERO);
    }

    /**
     * Takes the car back: records the Return inspection, works out the late fee, sets the final cost, and frees the vehicle.
     * Late fee = one extra day at the booked daily rate for each started 24 hours after the grace period.
     */
    @PostMapping("/{id}/return")
    @Transactional
    public HandoverResult giveBack(@PathVariable int id, @RequestBody HandoverRequest req, Authentication a) {
        int staff = users.staffId(a);
        Locked r = lock(id);
        if (!"Active".equalsIgnoreCase(r.status())) throw ApiError.conflict("Only active rentals can be returned.");
        if (req.returnFee() != null && req.returnFee().signum() < 0) throw ApiError.bad("The return fee can't be negative.");
        if (req.mileage() != null && req.mileage().signum() < 0) throw ApiError.bad("Mileage can't be negative.");

        List<BigDecimal> pickedUp = db.queryForList(
                "SELECT mileage FROM vehicle_inspection WHERE rental_id = ? AND inspection_type = 'Pickup' AND mileage IS NOT NULL"
              + " ORDER BY inspection_id DESC LIMIT 1", BigDecimal.class, id);
        if (req.mileage() != null && !pickedUp.isEmpty() && req.mileage().compareTo(pickedUp.get(0)) < 0)
            throw ApiError.bad("The return mileage can't be lower than the pickup mileage (" + pickedUp.get(0) + ").");

        LocalDateTime now = LocalDateTime.now();
        BigDecimal daily = db.queryForObject(
                "SELECT rr.rate_amount + v.unit_rate_adjustment FROM rental x JOIN rental_rate rr ON rr.rate_id = x.rate_id"
              + " JOIN vehicle v ON v.vehicle_id = x.vehicle_id WHERE x.rental_id = ?", BigDecimal.class, id);
        long minutesLate = Duration.between(r.returnBy(), now).toMinutes();
        BigDecimal lateFee = BigDecimal.ZERO;
        if (minutesLate > graceMinutes) {
            long daysLate = (minutesLate + 1439) / 1440;
            lateFee = daily.multiply(BigDecimal.valueOf(daysLate)).multiply(lateMultiplier).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal returnFee = req.returnFee() != null ? req.returnFee() : r.returnFee();
        BigDecimal estimated = r.estimated() == null ? BigDecimal.ZERO : r.estimated();
        BigDecimal finalCost = estimated.add(r.pickupFee()).add(returnFee).add(lateFee);

        int inspectionId = inspection(id, "Return", req.mileage(), req.fuelLevel(), req.conditionNotes(), staff);
        db.update("UPDATE rental SET rental_status = 'Completed', actual_return_datetime = ?, late_fee = ?, late_fee_status = ?,"
                + " return_fee = ?, final_cost = ? WHERE rental_id = ?",
                now, lateFee, lateFee.signum() > 0 ? "Unpaid" : null, returnFee, finalCost, id);
        db.update("UPDATE vehicle SET status = 'Available', mileage = COALESCE(?::numeric, mileage) WHERE vehicle_id = ?", req.mileage(), r.vehicleId());
        audit.log(staff, "STATUS", "vehicle", r.vehicleId(), "Vehicle " + r.vehicleId() + " status Rented -> Available (rental " + id + " returned)");
        audit.log(staff, "RETURN", "rental", id, "Processed return for rental " + id + ", late fee " + lateFee + ", final cost " + finalCost);
        notifier.notifyCustomer(r.customerId(), "Car returned", "Rental #" + id + " is complete. Total: " + finalCost
                + (lateFee.signum() > 0 ? " (includes a late fee of " + lateFee + ")" : "") + ".");
        return new HandoverResult(one(id), inspectionId, lateFee);
    }

    private void requireLicense(int customerId, LocalDate returnDate) {
        boolean licensed = Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM driver_license WHERE customer_id = ? AND verification_status = 'Verified' AND expiration_date >= ?)",
                Boolean.class, customerId, returnDate));
        if (!licensed) throw ApiError.bad("The customer needs a verified driver's license that is valid for the whole rental period.");
    }

    private Locked lock(int id) {
        List<Locked> rows = db.query(
                "SELECT customer_id, vehicle_id, rental_status, pickup_datetime, return_datetime, estimated_cost, pickup_fee, return_fee"
              + " FROM rental WHERE rental_id = ? FOR UPDATE",
                (rs, i) -> new Locked(rs.getInt("customer_id"), rs.getInt("vehicle_id"), rs.getString("rental_status"),
                        Rows.ldt(rs, "pickup_datetime"), Rows.ldt(rs, "return_datetime"), rs.getBigDecimal("estimated_cost"),
                        rs.getBigDecimal("pickup_fee"), rs.getBigDecimal("return_fee")), id);
        if (rows.isEmpty()) throw ApiError.notFound("Rental not found.");
        return rows.get(0);
    }

    private int inspection(int rentalId, String type, BigDecimal mileage, String fuel, String notes, int staffId) {
        Integer id = db.queryForObject(
                "INSERT INTO vehicle_inspection (rental_id, inspection_type, mileage, fuel_level, condition_notes, performed_by_staff_id)"
              + " VALUES (?,?,?,?,?,?) RETURNING inspection_id",
                Integer.class, rentalId, type, mileage, Rows.clean(fuel), Rows.clean(notes), staffId);
        return id == null ? 0 : id;
    }
}
