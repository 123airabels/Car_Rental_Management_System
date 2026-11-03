package com.carrental.car_rental_app.web;

import com.carrental.car_rental_app.support.BookingService;
import com.carrental.car_rental_app.support.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** The logged-in customer's own bookings: create, view, change dates, cancel. */
@RestController
@RequestMapping("/api/rentals")
public class RentalController {
    private final JdbcTemplate db;
    private final CurrentUser users;
    private final BookingService bookings;

    public RentalController(JdbcTemplate db, CurrentUser users, BookingService bookings) {
        this.db = db; this.users = users; this.bookings = bookings;
    }

    record CreateRequest(Integer vehicleId, String start, String end, Boolean acceptTerms) {}
    record ChangeRequest(String start, String end) {}
    record Check(boolean available, String reason) {}
    record RentalDto(int id, int vehicleId, String vehicle, LocalDate start, LocalDate end, BigDecimal total, String status, String reason) {}

    private static final String SELECT = """
        SELECT r.rental_id, r.vehicle_id, v.make || ' ' || v.model AS vehicle, r.pickup_datetime::date AS start_date,
               r.return_datetime::date AS end_date, COALESCE(r.final_cost, r.estimated_cost) AS total, r.rental_status,
               (SELECT x.cancellation_reason FROM cancellation x WHERE x.rental_id = r.rental_id ORDER BY x.cancellation_id DESC LIMIT 1) AS reason
        FROM rental r JOIN vehicle v ON v.vehicle_id = r.vehicle_id JOIN customer c ON c.customer_id = r.customer_id
        """;

    @GetMapping("/me")
    public List<RentalDto> mine(Authentication auth) {
        return db.query(SELECT + " WHERE lower(c.email) = lower(?) ORDER BY r.rental_id DESC",
                (rs, i) -> dto(rs), auth.getName());
    }

    @GetMapping("/{id}")
    public RentalDto one(@PathVariable int id, Authentication auth) {
        List<RentalDto> rows = db.query(SELECT + " WHERE r.rental_id = ? AND lower(c.email) = lower(?)",
                (rs, i) -> dto(rs), id, auth.getName());
        if (rows.isEmpty()) throw ApiError.notFound("Rental not found.");
        return rows.get(0);
    }

    /** Can this booking move to these dates? Same rules as saving, but the booking's own current dates don't count as a clash. */
    @GetMapping("/{id}/availability")
    public Check availability(@PathVariable int id, @RequestParam String start, @RequestParam String end, Authentication auth) {
        int customerId = users.customerId(auth);
        List<Integer> v = db.queryForList("SELECT vehicle_id FROM rental WHERE rental_id = ? AND customer_id = ?", Integer.class, id, customerId);
        if (v.isEmpty()) throw ApiError.notFound("Rental not found.");
        LocalDate s = date(start), e = date(end);
        bookings.checkDates(s, e, false);
        String clash = bookings.conflict(v.get(0), s, e, id, false);
        return new Check(clash == null, clash);
    }

    @PostMapping
    @Transactional
    public RentalDto create(@RequestBody CreateRequest req, Authentication auth) {
        int customerId = users.customerId(auth);
        if (req.vehicleId() == null) throw ApiError.bad("Choose a vehicle.");
        if (!Boolean.TRUE.equals(req.acceptTerms())) throw ApiError.bad("Read and accept the rental terms to book.");
        LocalDate start = date(req.start()), end = date(req.end());
        bookings.checkDates(start, end, false);

        bookings.lockVehicle(req.vehicleId());   // two customers can't book the same car at the same moment
        String clash = bookings.conflict(req.vehicleId(), start, end, null, false);
        if (clash != null) throw ApiError.conflict(clash);
        var q = bookings.quote(req.vehicleId(), start, end, null);

        Integer id = db.queryForObject(
                "INSERT INTO rental (customer_id, vehicle_id, pickup_datetime, return_datetime, rate_id, estimated_cost)"
              + " VALUES (?,?,?,?,?,?) RETURNING rental_id",
                Integer.class, customerId, req.vehicleId(), start.atStartOfDay(), end.atStartOfDay(), q.rateId(), q.estimate());
        // The exact wording and version they agreed to is stored with the booking.
        db.update("INSERT INTO rental_terms (rental_id, terms_version, terms_text, accepted, accepted_at) VALUES (?,?,?,true,CURRENT_TIMESTAMP)",
                id, TermsController.VERSION, TermsController.TEXT);
        return one(id, auth);
    }

    /**
     * Changes the dates of a Pending or Approved booking. An approved booking goes back to Pending, because staff
     * approved the old dates (and checked the license against them) and must look at the new ones.
     */
    @PutMapping("/{id}")
    @Transactional
    public RentalDto update(@PathVariable int id, @RequestBody ChangeRequest req, Authentication auth) {
        int customerId = users.customerId(auth);
        var rows = db.queryForList("SELECT vehicle_id, rate_id, rental_status FROM rental WHERE rental_id = ? AND customer_id = ? FOR UPDATE", id, customerId);
        if (rows.isEmpty()) throw ApiError.notFound("Rental not found.");
        String status = String.valueOf(rows.get(0).get("rental_status"));
        if (!List.of("Pending", "Approved").contains(status))
            throw ApiError.bad("Only pending or approved rentals can be changed.");
        int vehicleId = ((Number) rows.get(0).get("vehicle_id")).intValue();
        int rateId = ((Number) rows.get(0).get("rate_id")).intValue();

        LocalDate start = date(req.start()), end = date(req.end());
        bookings.checkDates(start, end, false);
        bookings.lockVehicle(vehicleId);
        String clash = bookings.conflict(vehicleId, start, end, id, false);   // ignore this booking's own old dates
        if (clash != null) throw ApiError.conflict(clash);
        var q = bookings.quote(vehicleId, start, end, rateId);

        db.update("UPDATE rental SET pickup_datetime = ?, return_datetime = ?, estimated_cost = ?,"
                + " rental_status = 'Pending', approved_at = NULL WHERE rental_id = ?",
                start.atStartOfDay(), end.atStartOfDay(), q.estimate(), id);
        return one(id, auth);
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public RentalDto cancel(@PathVariable int id, Authentication auth) {
        int customerId = users.customerId(auth);
        List<String> status = db.queryForList(
                "SELECT rental_status FROM rental WHERE rental_id = ? AND customer_id = ? FOR UPDATE", String.class, id, customerId);
        if (status.isEmpty()) throw ApiError.notFound("Rental not found.");
        if (!List.of("Pending", "Approved").contains(status.get(0)))
            throw ApiError.bad("Only pending or approved rentals can be cancelled.");
        db.update("UPDATE rental SET rental_status = 'Cancelled' WHERE rental_id = ?", id);
        db.update("INSERT INTO cancellation (rental_id, cancelled_by_customer_id, cancellation_reason) VALUES (?,?,?)",
                id, customerId, "Cancelled by customer on the website");
        return one(id, auth);
    }

    private static LocalDate date(String s) { return Rows.date(s, "Choose a pickup date and a return date."); }

    private static RentalDto dto(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RentalDto(rs.getInt("rental_id"), rs.getInt("vehicle_id"), rs.getString("vehicle"), rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(), rs.getBigDecimal("total"), rs.getString("rental_status").toUpperCase(), rs.getString("reason"));
    }
}
