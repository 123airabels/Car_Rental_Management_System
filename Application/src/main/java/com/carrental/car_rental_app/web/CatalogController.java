package com.carrental.car_rental_app.web;

import com.carrental.car_rental_app.support.AuditService;
import com.carrental.car_rental_app.support.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Categories, rates and vehicle records. Reading categories is public; changes are Admin only (see SecurityConfig). */
@RestController
@RequestMapping("/api")
public class CatalogController {
    private final JdbcTemplate db;
    private final CurrentUser users;
    private final AuditService audit;

    public CatalogController(JdbcTemplate db, CurrentUser users, AuditService audit) {
        this.db = db; this.users = users; this.audit = audit;
    }

    record CategoryDto(int id, String name, String description, String status, BigDecimal dailyRate) {}
    record CategoryRequest(String name, String description) {}
    record RateRequest(Integer categoryId, String periodType, BigDecimal amount) {}
    record VehicleRequest(Integer categoryId, String plateNumber, String make, String model, Integer year, String color,
                          Integer seatingCapacity, BigDecimal mileage, String fuelType, BigDecimal unitRateAdjustment, String status) {}
    record VehicleAdminDto(int id, int categoryId, String category, String plateNumber, String make, String model, Integer year,
                           String color, Integer seatingCapacity, BigDecimal mileage, String fuelType,
                           BigDecimal unitRateAdjustment, String status) {}

    record VehicleRentalDto(int id, String customer, LocalDateTime pickup, LocalDateTime returnBy, BigDecimal total, String status) {}

    private static final Set<String> PERIODS = Set.of("Daily", "Weekly", "Monthly");
    private static final Set<String> SETTABLE = Set.of("Available", "Maintenance", "Retired");

    private static final String VEHICLE_SELECT = """
        SELECT v.vehicle_id, v.category_id, c.category_name, v.plate_number, v.make, v.model, v.year, v.color,
               v.seating_capacity, v.mileage, v.fuel_type, v.unit_rate_adjustment, v.status
        FROM vehicle v JOIN vehicle_category c ON c.category_id = v.category_id
        """;

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return db.query("""
            SELECT c.category_id, c.category_name, c.description, c.category_status,
                   (SELECT r.rate_amount FROM rental_rate r WHERE r.category_id = c.category_id
                      AND r.rental_period_type = 'Daily' AND r.rate_status = 'Active' ORDER BY r.rate_id LIMIT 1) AS daily_rate
            FROM vehicle_category c WHERE c.category_status = 'Active' ORDER BY c.category_name
            """, (rs, i) -> new CategoryDto(rs.getInt("category_id"), rs.getString("category_name"), rs.getString("description"),
                Rows.up(rs.getString("category_status")), rs.getBigDecimal("daily_rate")));
    }

    @PostMapping("/admin/categories")
    @Transactional
    public CategoryDto addCategory(@RequestBody CategoryRequest r, Authentication a) {
        int staff = users.staffId(a);
        if (Rows.blank(r.name())) throw ApiError.bad("Enter a category name.");
        String name = r.name().trim();
        boolean exists = Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM vehicle_category WHERE lower(category_name) = lower(?))", Boolean.class, name));
        if (exists) throw ApiError.conflict("That category already exists.");
        Integer id = db.queryForObject("INSERT INTO vehicle_category (category_name, description) VALUES (?,?) RETURNING category_id",
                Integer.class, name, Rows.clean(r.description()));
        audit.log(staff, "CREATE", "vehicle_category", id, "Added category " + name);
        return new CategoryDto(id, name, Rows.clean(r.description()), "ACTIVE", null);
    }

    /** Sets a new rate. The old active rate becomes Inactive so existing rentals keep the price they booked at. */
    @PostMapping("/admin/rates")
    @Transactional
    public Map<String, Object> setRate(@RequestBody RateRequest r, Authentication a) {
        int staff = users.staffId(a);
        if (r.categoryId() == null || r.amount() == null || r.amount().signum() < 0)
            throw ApiError.bad("Choose a category and enter a rate of zero or more.");
        String wanted = r.periodType() == null ? "Daily" : r.periodType().trim();
        String period = PERIODS.stream().filter(p -> p.equalsIgnoreCase(wanted)).findFirst()
                .orElseThrow(() -> ApiError.bad("The rate period must be Daily, Weekly, or Monthly."));
        boolean cat = Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM vehicle_category WHERE category_id = ?)", Boolean.class, r.categoryId()));
        if (!cat) throw ApiError.notFound("That category doesn't exist.");
        db.update("UPDATE rental_rate SET rate_status = 'Inactive' WHERE category_id = ? AND rental_period_type = ? AND rate_status = 'Active'",
                r.categoryId(), period);
        Integer id = db.queryForObject("INSERT INTO rental_rate (category_id, rental_period_type, rate_amount) VALUES (?,?,?) RETURNING rate_id",
                Integer.class, r.categoryId(), period, r.amount());
        audit.log(staff, "UPDATE", "rental_rate", id, "Set " + period + " rate for category " + r.categoryId() + " to " + r.amount());
        return Map.of("rateId", id, "categoryId", r.categoryId(), "periodType", period, "amount", r.amount());
    }

    @GetMapping("/admin/vehicles/{id}")
    public VehicleAdminDto vehicle(@PathVariable int id) {
        List<VehicleAdminDto> rows = db.query(VEHICLE_SELECT + " WHERE v.vehicle_id = ?", (rs, i) -> map(rs), id);
        if (rows.isEmpty()) throw ApiError.notFound("Vehicle not found.");
        return rows.get(0);
    }

    @PostMapping("/admin/vehicles")
    @Transactional
    public VehicleAdminDto addVehicle(@RequestBody VehicleRequest r, Authentication a) {
        int staff = users.staffId(a);
        validate(r);
        String plate = r.plateNumber().trim().toUpperCase();
        if (plateTaken(plate, null)) throw ApiError.conflict("A vehicle with that plate number already exists.");
        Integer id = db.queryForObject(
                "INSERT INTO vehicle (category_id, plate_number, make, model, year, color, seating_capacity, mileage, fuel_type, unit_rate_adjustment)"
              + " VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING vehicle_id",
                Integer.class, r.categoryId(), plate, r.make().trim(), r.model().trim(), r.year(), Rows.clean(r.color()),
                r.seatingCapacity(), r.mileage(), Rows.clean(r.fuelType()),
                r.unitRateAdjustment() == null ? BigDecimal.ZERO : r.unitRateAdjustment());
        audit.log(staff, "CREATE", "vehicle", id, "Added vehicle " + plate + " (" + r.make().trim() + " " + r.model().trim() + ")");
        return vehicle(id);
    }

    @PutMapping("/admin/vehicles/{id}")
    @Transactional
    public VehicleAdminDto updateVehicle(@PathVariable int id, @RequestBody VehicleRequest r, Authentication a) {
        int staff = users.staffId(a);
        List<String> found = db.queryForList("SELECT status FROM vehicle WHERE vehicle_id = ? FOR UPDATE", String.class, id);
        if (found.isEmpty()) throw ApiError.notFound("Vehicle not found.");
        String current = found.get(0);
        validate(r);
        String plate = r.plateNumber().trim().toUpperCase();
        if (plateTaken(plate, id)) throw ApiError.conflict("Another vehicle already uses that plate number.");

        String status = current;
        if (r.status() != null && !r.status().equalsIgnoreCase(current)) {
            if ("Rented".equalsIgnoreCase(current))
                throw ApiError.conflict("This vehicle is currently rented out. Process its return first.");
            status = SETTABLE.stream().filter(s -> s.equalsIgnoreCase(r.status().trim())).findFirst()
                    .orElseThrow(() -> ApiError.bad("Status must be Available, Maintenance, or Retired."));
        }
        db.update("UPDATE vehicle SET category_id=?, plate_number=?, make=?, model=?, year=?, color=?, seating_capacity=?, mileage=?,"
                + " fuel_type=?, unit_rate_adjustment=?, status=? WHERE vehicle_id=?",
                r.categoryId(), plate, r.make().trim(), r.model().trim(), r.year(), Rows.clean(r.color()), r.seatingCapacity(),
                r.mileage(), Rows.clean(r.fuelType()), r.unitRateAdjustment() == null ? BigDecimal.ZERO : r.unitRateAdjustment(), status, id);
        audit.log(staff, "UPDATE", "vehicle", id, "Updated vehicle " + plate + " (status " + status + ")");
        return vehicle(id);
    }

    /**
     * Deletes a vehicle that has never been rented or serviced. One with history can't be deleted (the records must stay);
     * set its status to Retired instead, which also hides it from customers.
     */
    @DeleteMapping("/admin/vehicles/{id}")
    @Transactional
    public Map<String, String> deleteVehicle(@PathVariable int id, Authentication a) {
        int staff = users.staffId(a);
        List<Map<String, Object>> found = db.queryForList("SELECT plate_number, status FROM vehicle WHERE vehicle_id = ? FOR UPDATE", id);
        if (found.isEmpty()) throw ApiError.notFound("Vehicle not found.");
        String plate = String.valueOf(found.get(0).get("plate_number"));
        if ("Rented".equalsIgnoreCase(String.valueOf(found.get(0).get("status"))))
            throw ApiError.conflict("This vehicle is currently rented out. Process its return first.");
        boolean history = Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM rental WHERE vehicle_id = ?) OR EXISTS (SELECT 1 FROM vehicle_maintenance WHERE vehicle_id = ?)",
                Boolean.class, id, id));
        if (history) throw ApiError.conflict("This vehicle has rental or maintenance history, so it can't be deleted. "
                + "Set its status to Retired instead: it disappears for customers and the records are kept.");
        db.update("DELETE FROM vehicle WHERE vehicle_id = ?", id);
        audit.log(staff, "DELETE", "vehicle", id, "Deleted vehicle " + plate);
        return Map.of("message", "Vehicle deleted.");
    }

    @GetMapping("/admin/vehicles/{id}/rentals")
    public List<VehicleRentalDto> vehicleRentals(@PathVariable int id) {
        vehicle(id);   // 404 if it doesn't exist
        return db.query("""
            SELECT r.rental_id, c.first_name || ' ' || c.last_name AS customer, r.pickup_datetime, r.return_datetime,
                   COALESCE(r.final_cost, r.estimated_cost) AS total, r.rental_status
            FROM rental r JOIN customer c ON c.customer_id = r.customer_id
            WHERE r.vehicle_id = ? ORDER BY r.rental_id DESC LIMIT 50
            """, (rs, i) -> new VehicleRentalDto(rs.getInt("rental_id"), rs.getString("customer"), Rows.ldt(rs, "pickup_datetime"),
                Rows.ldt(rs, "return_datetime"), rs.getBigDecimal("total"), Rows.up(rs.getString("rental_status"))), id);
    }

    private void validate(VehicleRequest r) {
        if (r.categoryId() == null) throw ApiError.bad("Choose a vehicle category.");
        boolean cat = Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM vehicle_category WHERE category_id = ?)", Boolean.class, r.categoryId()));
        if (!cat) throw ApiError.notFound("That category doesn't exist.");
        if (Rows.blank(r.plateNumber()) || Rows.blank(r.make()) || Rows.blank(r.model()))
            throw ApiError.bad("Plate number, make, and model are required.");
        if (r.year() != null && (r.year() < 1900 || r.year() > Year.now().getValue() + 1)) throw ApiError.bad("Enter a valid model year.");
        if (r.seatingCapacity() != null && r.seatingCapacity() <= 0) throw ApiError.bad("Seating capacity must be more than zero.");
        if (r.mileage() != null && r.mileage().signum() < 0) throw ApiError.bad("Mileage can't be negative.");
    }

    private boolean plateTaken(String plate, Integer exceptId) {
        return Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM vehicle WHERE upper(plate_number) = ? AND (?::int IS NULL OR vehicle_id <> ?::int))",
                Boolean.class, plate, exceptId, exceptId));
    }

    private static VehicleAdminDto map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new VehicleAdminDto(rs.getInt("vehicle_id"), rs.getInt("category_id"), rs.getString("category_name"),
                rs.getString("plate_number"), rs.getString("make"), rs.getString("model"), Rows.intOrNull(rs, "year"),
                rs.getString("color"), Rows.intOrNull(rs, "seating_capacity"), rs.getBigDecimal("mileage"),
                rs.getString("fuel_type"), rs.getBigDecimal("unit_rate_adjustment"), Rows.up(rs.getString("status")));
    }
}
