package com.carrental.car_rental_app.web;

import com.carrental.car_rental_app.support.BookingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class VehicleController {
    private final JdbcTemplate db;
    private final BookingService bookings;

    public VehicleController(JdbcTemplate db, BookingService bookings) { this.db = db; this.bookings = bookings; }

    record VehicleDto(int id, String make, String model, Integer year, String category, Integer seats,
                      String fuel, String color, BigDecimal dailyRate, String status) {}

    /** What a customer needs to decide on a car. Plate number, mileage and rate adjustments stay internal. */
    record VehicleDetail(int id, String make, String model, Integer year, String category, String categoryDescription,
                         Integer seats, String fuel, String color, BigDecimal dailyRate, String status, Boolean availableForDates) {}

    private static final String BASE = """
        SELECT v.vehicle_id, v.make, v.model, v.year, c.category_name, v.seating_capacity, v.fuel_type, v.color, v.status,
               (SELECT r.rate_amount FROM rental_rate r
                 WHERE r.category_id = v.category_id AND r.rental_period_type = 'Daily' AND r.rate_status = 'Active'
                 ORDER BY r.rate_id LIMIT 1) + v.unit_rate_adjustment AS daily_rate
        FROM vehicle v JOIN vehicle_category c ON c.category_id = v.category_id
        """;

    /** Public. Optional filters: category, and start/end dates (hides cars already booked, or in or due for maintenance, in that window). */
    @GetMapping("/vehicles")
    public List<VehicleDto> list(@RequestParam(required = false) String category,
                                 @RequestParam(required = false) String start,
                                 @RequestParam(required = false) String end) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE c.category_status = 'Active' AND v.status <> 'Retired'");
        List<Object> args = new ArrayList<>();
        if (category != null && !category.isBlank()) { sql.append(" AND lower(c.category_name) = lower(?)"); args.add(category); }
        LocalDate s = date(start), e = date(end);
        if (s != null && e != null) {
            sql.append(" AND NOT EXISTS (SELECT 1 FROM rental x WHERE x.vehicle_id = v.vehicle_id"
                     + " AND x.rental_status IN ('Pending','Approved','Active')"
                     + " AND x.pickup_datetime < ? AND x.return_datetime > ?)"
                     + " AND v.status <> 'Maintenance'"
                     + " AND NOT EXISTS (SELECT 1 FROM vehicle_maintenance m WHERE m.vehicle_id = v.vehicle_id AND m.status = 'Pending'"
                     + " AND m.maintenance_date >= ? AND m.maintenance_date < ?)");
            args.add(e.atStartOfDay()); args.add(s.atStartOfDay()); args.add(s); args.add(e);
        }
        sql.append(" ORDER BY c.category_name, v.make, v.model");
        return db.query(sql.toString(), (rs, i) -> map(rs), args.toArray());
    }

    /** Public. One car's details. With start and end dates it also says whether the car is free for those dates. */
    @GetMapping("/vehicles/{id}")
    public VehicleDetail one(@PathVariable int id, @RequestParam(required = false) String start, @RequestParam(required = false) String end) {
        List<VehicleDetail> rows = db.query("""
            SELECT v.vehicle_id, v.make, v.model, v.year, c.category_name, c.description, v.seating_capacity, v.fuel_type, v.color, v.status,
                   (SELECT r.rate_amount FROM rental_rate r
                     WHERE r.category_id = v.category_id AND r.rental_period_type = 'Daily' AND r.rate_status = 'Active'
                     ORDER BY r.rate_id LIMIT 1) + v.unit_rate_adjustment AS daily_rate
            FROM vehicle v JOIN vehicle_category c ON c.category_id = v.category_id
            WHERE v.vehicle_id = ? AND c.category_status = 'Active' AND v.status <> 'Retired'
            """, (rs, i) -> new VehicleDetail(rs.getInt("vehicle_id"), rs.getString("make"), rs.getString("model"),
                (Integer) rs.getObject("year"), rs.getString("category_name"), rs.getString("description"),
                (Integer) rs.getObject("seating_capacity"), rs.getString("fuel_type"), rs.getString("color"),
                rs.getBigDecimal("daily_rate"), rs.getString("status").toUpperCase(), null), id);
        if (rows.isEmpty()) throw ApiError.notFound("That car doesn't exist.");
        VehicleDetail v = rows.get(0);
        LocalDate s = date(start), e = date(end);
        Boolean free = null;
        if (s != null && e != null) {
            bookings.checkDates(s, e, false);
            free = bookings.conflict(id, s, e, null, false) == null;
        }
        return new VehicleDetail(v.id(), v.make(), v.model(), v.year(), v.category(), v.categoryDescription(), v.seats(),
                v.fuel(), v.color(), v.dailyRate(), v.status(), free);
    }

    record Availability(int vehicleId, String start, String end, Boolean available, String reason, List<BookingService.Booked> booked) {}

    /**
     * Public. Is this car free for the dates, and which upcoming dates are already taken (no names, just dates).
     * Without start and end it only returns the taken dates, so a calendar can be drawn.
     */
    @GetMapping("/vehicles/{id}/availability")
    public Availability availability(@PathVariable int id, @RequestParam(required = false) String start, @RequestParam(required = false) String end) {
        if (db.queryForList("SELECT 1 FROM vehicle WHERE vehicle_id = ? AND status <> 'Retired'", id).isEmpty())
            throw ApiError.notFound("That car doesn't exist.");
        LocalDate s = date(start), e = date(end);
        Boolean free = null; String reason = null;
        if (s != null && e != null) {
            bookings.checkDates(s, e, false);
            reason = bookings.conflict(id, s, e, null, false);
            free = reason == null;
        }
        return new Availability(id, start, end, free, reason, bookings.upcoming(id));
    }

    record AdminVehicleRow(int id, int categoryId, String category, String plateNumber, String make, String model, Integer year,
                           String color, Integer seatingCapacity, BigDecimal mileage, String fuelType, BigDecimal unitRateAdjustment,
                           BigDecimal dailyRate, String status) {}

    /** Staff and admin only (see SecurityConfig). Every vehicle, any status, with the internal fields. */
    @GetMapping("/admin/vehicles")
    public List<AdminVehicleRow> all() {
        return db.query("""
            SELECT v.vehicle_id, v.category_id, c.category_name, v.plate_number, v.make, v.model, v.year, v.color, v.seating_capacity,
                   v.mileage, v.fuel_type, v.unit_rate_adjustment, v.status,
                   (SELECT r.rate_amount FROM rental_rate r
                     WHERE r.category_id = v.category_id AND r.rental_period_type = 'Daily' AND r.rate_status = 'Active'
                     ORDER BY r.rate_id LIMIT 1) + v.unit_rate_adjustment AS daily_rate
            FROM vehicle v JOIN vehicle_category c ON c.category_id = v.category_id
            ORDER BY v.vehicle_id
            """, (rs, i) -> new AdminVehicleRow(rs.getInt("vehicle_id"), rs.getInt("category_id"), rs.getString("category_name"),
                rs.getString("plate_number"), rs.getString("make"), rs.getString("model"), (Integer) rs.getObject("year"),
                rs.getString("color"), (Integer) rs.getObject("seating_capacity"), rs.getBigDecimal("mileage"), rs.getString("fuel_type"),
                rs.getBigDecimal("unit_rate_adjustment"), rs.getBigDecimal("daily_rate"), rs.getString("status").toUpperCase()));
    }

    private static VehicleDto map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new VehicleDto(rs.getInt("vehicle_id"), rs.getString("make"), rs.getString("model"),
                (Integer) rs.getObject("year"), rs.getString("category_name"), (Integer) rs.getObject("seating_capacity"),
                rs.getString("fuel_type"), rs.getString("color"), rs.getBigDecimal("daily_rate"),
                rs.getString("status").toUpperCase());
    }

    private static LocalDate date(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); }
        catch (DateTimeParseException ex) { throw ApiError.bad("Dates must look like 2026-12-31."); }
    }
}
