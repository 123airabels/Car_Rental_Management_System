package com.carrental.car_rental_app.support;

import com.carrental.car_rental_app.web.ApiError;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * One place for the booking rules, used by the customer and the staff side so they can never disagree:
 * date checks, "is this car free for these dates", and the price estimate.
 *
 * A booking holds a car while it is Pending, Approved or Active. Dates are half-open: a car returned on the 10th
 * can be picked up by someone else on the 10th. Callers must call {@link #lockVehicle} first and keep the check
 * and the INSERT/UPDATE in the same transaction, so two people can't both pass the check for the same car.
 */
@Service
public class BookingService {
    /** Statuses that keep a car reserved. */
    public static final String HOLDING = "('Pending','Approved','Active')";

    public record Quote(int rateId, BigDecimal daily, BigDecimal estimate, long days) {}
    public record Booked(LocalDate start, LocalDate end, String kind) {}

    private final JdbcTemplate db;

    public BookingService(JdbcTemplate db) { this.db = db; }

    public void checkDates(LocalDate start, LocalDate end, boolean allowPast) {
        if (start == null || end == null) throw ApiError.bad("Choose a pickup date and a return date.");
        if (!end.isAfter(start)) throw ApiError.bad("The return date must be after the pickup date.");
        if (!allowPast && start.isBefore(LocalDate.now())) throw ApiError.bad("The pickup date can't be in the past.");
    }

    /** Locks the vehicle row until the transaction ends. Everyone booking the same car queues here. */
    public void lockVehicle(int vehicleId) {
        List<Integer> row = db.queryForList("SELECT vehicle_id FROM vehicle WHERE vehicle_id = ? FOR UPDATE", Integer.class, vehicleId);
        if (row.isEmpty()) throw ApiError.notFound("That vehicle doesn't exist.");
    }

    /**
     * Why this car can't be booked for [start, end), or null when it is free.
     * Staff get the clashing booking number; customers don't (it belongs to someone else).
     */
    public String conflict(int vehicleId, LocalDate start, LocalDate end, Integer excludeRentalId, boolean forStaff) {
        String status = db.queryForObject("SELECT status FROM vehicle WHERE vehicle_id = ?", String.class, vehicleId);
        if ("Retired".equalsIgnoreCase(status)) return "This vehicle has been retired and can't be booked.";
        if ("Maintenance".equalsIgnoreCase(status)) return "This vehicle is in maintenance right now and can't be booked.";

        List<Integer> clash = db.queryForList(
                "SELECT rental_id FROM rental WHERE vehicle_id = ? AND rental_status IN " + HOLDING
              + " AND pickup_datetime < ? AND return_datetime > ? AND (?::int IS NULL OR rental_id <> ?::int)"
              + " ORDER BY pickup_datetime LIMIT 1",
                Integer.class, vehicleId, end.atStartOfDay(), start.atStartOfDay(), excludeRentalId, excludeRentalId);
        if (!clash.isEmpty())
            return forStaff ? "This car is already booked for those dates (booking #" + clash.get(0) + "). Choose other dates or another car."
                            : "This car is already booked for those dates. Try other dates.";

        boolean service = Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM vehicle_maintenance WHERE vehicle_id = ? AND status = 'Pending'"
              + " AND maintenance_date >= ? AND maintenance_date < ?)", Boolean.class, vehicleId, start, end));
        if (service) return "This car is scheduled for maintenance during those dates. Try other dates.";
        return null;
    }

    /** Estimated price. Pass keepRateId to hold the rate an existing booking was made at. */
    public Quote quote(int vehicleId, LocalDate start, LocalDate end, Integer keepRateId) {
        Map<String, Object> v = db.queryForMap("SELECT category_id, unit_rate_adjustment FROM vehicle WHERE vehicle_id = ?", vehicleId);
        List<Map<String, Object>> rate = keepRateId != null
                ? db.queryForList("SELECT rate_id, rate_amount FROM rental_rate WHERE rate_id = ?", keepRateId)
                : db.queryForList("SELECT rate_id, rate_amount FROM rental_rate WHERE category_id = ? AND rental_period_type = 'Daily'"
                        + " AND rate_status = 'Active' ORDER BY rate_id LIMIT 1", v.get("category_id"));
        if (rate.isEmpty()) throw ApiError.bad("No daily rate is set for this vehicle type yet.");
        BigDecimal daily = ((BigDecimal) rate.get(0).get("rate_amount")).add((BigDecimal) v.get("unit_rate_adjustment"));
        long days = ChronoUnit.DAYS.between(start, end);
        return new Quote(((Number) rate.get(0).get("rate_id")).intValue(), daily, daily.multiply(BigDecimal.valueOf(days)), days);
    }

    /** Upcoming dates when the car is taken. No names or booking numbers: this is shown to the public. */
    public List<Booked> upcoming(int vehicleId) {
        List<Booked> rentals = db.query(
                "SELECT pickup_datetime::date AS s, return_datetime::date AS e FROM rental WHERE vehicle_id = ?"
              + " AND rental_status IN " + HOLDING + " AND return_datetime > CURRENT_TIMESTAMP ORDER BY pickup_datetime",
                (rs, i) -> new Booked(rs.getObject("s", LocalDate.class), rs.getObject("e", LocalDate.class), "BOOKED"), vehicleId);
        List<Booked> service = db.query(
                "SELECT maintenance_date AS s FROM vehicle_maintenance WHERE vehicle_id = ? AND status = 'Pending'"
              + " AND maintenance_date >= CURRENT_DATE ORDER BY maintenance_date",
                (rs, i) -> {
                    LocalDate d = rs.getObject("s", LocalDate.class);
                    return new Booked(d, d.plusDays(1), "MAINTENANCE");
                }, vehicleId);
        List<Booked> out = new ArrayList<>(rentals);
        out.addAll(service);
        out.sort(Comparator.comparing(Booked::start));
        return out;
    }
}
