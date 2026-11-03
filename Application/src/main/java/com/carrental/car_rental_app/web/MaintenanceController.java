package com.carrental.car_rental_app.web;

import com.carrental.car_rental_app.support.AuditService;
import com.carrental.car_rental_app.support.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Vehicle maintenance. Status flow: Pending -> In Progress -> Completed (or Cancelled). Starting work takes the car off the road. */
@RestController
@RequestMapping("/api/admin/maintenance")
public class MaintenanceController {
    private final JdbcTemplate db;
    private final CurrentUser users;
    private final AuditService audit;

    public MaintenanceController(JdbcTemplate db, CurrentUser users, AuditService audit) {
        this.db = db; this.users = users; this.audit = audit;
    }

    record MaintenanceRequest(Integer vehicleId, String maintenanceType, String description, String maintenanceDate, BigDecimal cost, String notes) {}
    record CompleteRequest(BigDecimal cost, String notes) {}
    record MaintenanceDto(int id, int vehicleId, String vehicle, String plateNumber, String maintenanceType, String description,
                          LocalDate maintenanceDate, BigDecimal cost, String status, String notes, LocalDate completedDate) {}

    private static final String SELECT = """
        SELECT m.maintenance_id, m.vehicle_id, v.make || ' ' || v.model AS vehicle, v.plate_number, m.maintenance_type, m.description,
               m.maintenance_date, m.cost, m.status, m.notes, m.completed_date
        FROM vehicle_maintenance m JOIN vehicle v ON v.vehicle_id = m.vehicle_id
        """;

    private static MaintenanceDto map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new MaintenanceDto(rs.getInt("maintenance_id"), rs.getInt("vehicle_id"), rs.getString("vehicle"), rs.getString("plate_number"),
                rs.getString("maintenance_type"), rs.getString("description"), Rows.ld(rs, "maintenance_date"), rs.getBigDecimal("cost"),
                Rows.up(rs.getString("status")), rs.getString("notes"), Rows.ld(rs, "completed_date"));
    }

    @GetMapping
    public List<MaintenanceDto> list(@RequestParam(required = false) Integer vehicleId, @RequestParam(required = false) String status) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (vehicleId != null) { sql.append(" AND m.vehicle_id = ?"); args.add(vehicleId); }
        if (!Rows.blank(status)) { sql.append(" AND lower(m.status) = lower(?)"); args.add(status.trim()); }
        sql.append(" ORDER BY m.maintenance_date DESC, m.maintenance_id DESC");
        return db.query(sql.toString(), (rs, i) -> map(rs), args.toArray());
    }

    @PostMapping
    @Transactional
    public MaintenanceDto schedule(@RequestBody MaintenanceRequest r, Authentication a) {
        int staff = users.staffId(a);
        if (r.vehicleId() == null) throw ApiError.bad("Choose a vehicle.");
        if (Rows.blank(r.maintenanceType())) throw ApiError.bad("Enter the type of maintenance.");
        LocalDate date = Rows.date(r.maintenanceDate(), "The maintenance date must look like 2026-12-31.");
        if (r.cost() != null && r.cost().signum() < 0) throw ApiError.bad("The cost can't be negative.");
        boolean exists = Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS (SELECT 1 FROM vehicle WHERE vehicle_id = ?)", Boolean.class, r.vehicleId()));
        if (!exists) throw ApiError.notFound("Vehicle not found.");
        Integer id = db.queryForObject(
                "INSERT INTO vehicle_maintenance (vehicle_id, maintenance_type, description, maintenance_date, cost, notes) VALUES (?,?,?,?,?,?) RETURNING maintenance_id",
                Integer.class, r.vehicleId(), r.maintenanceType().trim(), Rows.clean(r.description()), date, r.cost(), Rows.clean(r.notes()));
        audit.log(staff, "CREATE", "vehicle_maintenance", id, "Scheduled " + r.maintenanceType().trim() + " for vehicle " + r.vehicleId());
        return one(id);
    }

    @PostMapping("/{id}/start")
    @Transactional
    public MaintenanceDto start(@PathVariable int id, Authentication a) {
        int staff = users.staffId(a);
        var m = lock(id);
        if (!"Pending".equalsIgnoreCase(m.status())) throw ApiError.conflict("Only pending maintenance can be started.");
        String vStatus = db.queryForObject("SELECT status FROM vehicle WHERE vehicle_id = ? FOR UPDATE", String.class, m.vehicleId());
        if ("Rented".equalsIgnoreCase(vStatus)) throw ApiError.conflict("This vehicle is rented out right now. Wait for its return first.");
        db.update("UPDATE vehicle_maintenance SET status = 'In Progress' WHERE maintenance_id = ?", id);
        db.update("UPDATE vehicle SET status = 'Maintenance' WHERE vehicle_id = ?", m.vehicleId());
        audit.log(staff, "UPDATE", "vehicle_maintenance", id, "Started maintenance " + id + "; vehicle " + m.vehicleId() + " is now in maintenance");
        return one(id);
    }

    @PostMapping("/{id}/complete")
    @Transactional
    public MaintenanceDto complete(@PathVariable int id, @RequestBody(required = false) CompleteRequest r, Authentication a) {
        int staff = users.staffId(a);
        var m = lock(id);
        if (!"In Progress".equalsIgnoreCase(m.status())) throw ApiError.conflict("Only maintenance that is in progress can be completed.");
        if (r != null && r.cost() != null && r.cost().signum() < 0) throw ApiError.bad("The cost can't be negative.");
        db.update("UPDATE vehicle_maintenance SET status = 'Completed', completed_date = GREATEST(CURRENT_DATE, maintenance_date),"
                + " cost = COALESCE(?::numeric, cost), notes = COALESCE(?::text, notes) WHERE maintenance_id = ?",
                r == null ? null : r.cost(), r == null ? null : Rows.clean(r.notes()), id);
        // Put the car back in service unless another job on it is still in progress.
        db.update("UPDATE vehicle SET status = 'Available' WHERE vehicle_id = ? AND status = 'Maintenance'"
                + " AND NOT EXISTS (SELECT 1 FROM vehicle_maintenance WHERE vehicle_id = ? AND status = 'In Progress')", m.vehicleId(), m.vehicleId());
        audit.log(staff, "UPDATE", "vehicle_maintenance", id, "Completed maintenance " + id);
        return one(id);
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public MaintenanceDto cancel(@PathVariable int id, Authentication a) {
        int staff = users.staffId(a);
        var m = lock(id);
        if (!"Pending".equalsIgnoreCase(m.status())) throw ApiError.conflict("Only pending maintenance can be cancelled.");
        db.update("UPDATE vehicle_maintenance SET status = 'Cancelled' WHERE maintenance_id = ?", id);
        audit.log(staff, "UPDATE", "vehicle_maintenance", id, "Cancelled maintenance " + id);
        return one(id);
    }

    private record Locked(int vehicleId, String status) {}

    private Locked lock(int id) {
        List<Locked> rows = db.query("SELECT vehicle_id, status FROM vehicle_maintenance WHERE maintenance_id = ? FOR UPDATE",
                (rs, i) -> new Locked(rs.getInt("vehicle_id"), rs.getString("status")), id);
        if (rows.isEmpty()) throw ApiError.notFound("Maintenance record not found.");
        return rows.get(0);
    }

    private MaintenanceDto one(int id) {
        return db.queryForObject(SELECT + " WHERE m.maintenance_id = ?", (rs, i) -> map(rs), id);
    }
}
