package com.carrental.car_rental_app.web;

import com.carrental.car_rental_app.support.AuditService;
import com.carrental.car_rental_app.support.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Staff accounts, the customer list, the audit log, and a summary report. Access rules are in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdministrationController {
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final CurrentUser users;
    private final AuditService audit;

    public AdministrationController(JdbcTemplate db, PasswordEncoder encoder, CurrentUser users, AuditService audit) {
        this.db = db; this.encoder = encoder; this.users = users; this.audit = audit;
    }

    record StaffRequest(String firstName, String lastName, String email, String password, String role) {}
    record StatusRequest(String status) {}
    record RoleRequest(String role) {}
    record StaffDto(int id, String firstName, String lastName, String email, String role, String status, LocalDateTime createdAt) {}
    record CustomerDto(int id, String name, String email, String phone, String status, LocalDateTime createdAt, String licenseStatus) {}
    record AuditDto(int id, String staff, String action, String table, Integer recordId, String description, LocalDateTime at) {}

    private static final List<String> ROLES = List.of("Staff", "Finance", "Admin", "System Admin");
    /** Roles an Admin may create and manage. Only a System Admin can create or change Admin and System Admin accounts. */
    private static final List<String> ADMIN_MANAGEABLE = List.of("Staff", "Finance");

    private boolean isSystemAdmin(Authentication a) { return "SYSTEM_ADMIN".equals(users.role(a)); }

    /** Resolves a typed role to its canonical name and checks the caller is allowed to hand it out. */
    private String grantableRole(String requested, Authentication a) {
        String wanted = requested == null ? "" : requested.trim();
        String role = ROLES.stream().filter(x -> x.equalsIgnoreCase(wanted)).findFirst()
                .orElseThrow(() -> ApiError.bad("Role must be Staff, Finance, Admin, or System Admin."));
        if (!isSystemAdmin(a) && !ADMIN_MANAGEABLE.contains(role))
            throw ApiError.forbidden("Only a System Admin can create or assign the " + role + " role.");
        return role;
    }

    /** Looks up an account's current role, and refuses if the caller is an Admin and the target is an Admin or System Admin. */
    private String manageableTargetRole(int id, Authentication a) {
        List<String> found = db.queryForList("SELECT role FROM staff WHERE staff_id = ?", String.class, id);
        if (found.isEmpty()) throw ApiError.notFound("Staff member not found.");
        String current = found.get(0);
        if (!isSystemAdmin(a) && !ADMIN_MANAGEABLE.contains(current))
            throw ApiError.forbidden("Only a System Admin can change " + current + " accounts.");
        return current;
    }

    /** Keeps at least one active System Admin so the system can never lock itself out of account management. */
    private void requireAnotherSystemAdmin(int excludingId) {
        Integer others = db.queryForObject(
                "SELECT COUNT(*) FROM staff WHERE role = 'System Admin' AND account_status = 'Active' AND staff_id <> ?", Integer.class, excludingId);
        if (others == null || others == 0) throw ApiError.conflict("There must be at least one active System Admin.");
    }

    // ---------- staff accounts ----------

    @GetMapping("/staff")
    public List<StaffDto> staff() {
        return db.query("SELECT staff_id, first_name, last_name, email, role, account_status, created_at FROM staff ORDER BY staff_id",
                (rs, i) -> new StaffDto(rs.getInt("staff_id"), rs.getString("first_name"), rs.getString("last_name"), rs.getString("email"),
                        rs.getString("role"), Rows.up(rs.getString("account_status")), Rows.ldt(rs, "created_at")));
    }

    @PostMapping("/staff")
    @Transactional
    public StaffDto addStaff(@RequestBody StaffRequest r, Authentication a) {
        int me = users.staffId(a);
        if (Rows.blank(r.firstName()) || Rows.blank(r.lastName()) || Rows.blank(r.email()))
            throw ApiError.bad("Enter first name, last name, and email.");
        if (r.password() == null || r.password().length() < 8) throw ApiError.bad("The password must be at least 8 characters.");
        String role = grantableRole(r.role(), a);
        String email = r.email().trim().toLowerCase();
        boolean taken = Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM customer WHERE lower(email) = ?) OR EXISTS (SELECT 1 FROM staff WHERE lower(email) = ?)",
                Boolean.class, email, email));
        if (taken) throw ApiError.conflict("That email is already registered.");
        Integer id = db.queryForObject("INSERT INTO staff (first_name, last_name, email, password_hash, role) VALUES (?,?,?,?,?) RETURNING staff_id",
                Integer.class, r.firstName().trim(), r.lastName().trim(), email, encoder.encode(r.password()), role);
        audit.log(me, "CREATE", "staff", id, "Created " + role + " account for " + email);
        return db.queryForObject("SELECT staff_id, first_name, last_name, email, role, account_status, created_at FROM staff WHERE staff_id = ?",
                (rs, i) -> new StaffDto(rs.getInt("staff_id"), rs.getString("first_name"), rs.getString("last_name"), rs.getString("email"),
                        rs.getString("role"), Rows.up(rs.getString("account_status")), Rows.ldt(rs, "created_at")), id);
    }

    @PostMapping("/staff/{id}/status")
    @Transactional
    public Map<String, String> staffStatus(@PathVariable int id, @RequestBody StatusRequest r, Authentication a) {
        int me = users.staffId(a);
        String status = pick(r.status(), "Active", "Inactive");
        if (me == id) throw ApiError.bad("You can't change your own status.");
        String target = manageableTargetRole(id, a);
        if ("Inactive".equals(status) && "System Admin".equals(target)) requireAnotherSystemAdmin(id);
        db.update("UPDATE staff SET account_status = ? WHERE staff_id = ?", status, id);
        audit.log(me, "UPDATE", "staff", id, "Set staff " + id + " (" + target + ") to " + status);
        return Map.of("status", status.toUpperCase());
    }

    /** Changes a staff member's role. Admins can only move people between Staff and Finance. */
    @PostMapping("/staff/{id}/role")
    @Transactional
    public Map<String, String> staffRole(@PathVariable int id, @RequestBody RoleRequest r, Authentication a) {
        int me = users.staffId(a);
        if (me == id) throw ApiError.bad("You can't change your own role.");
        String role = grantableRole(r.role(), a);
        String current = manageableTargetRole(id, a);
        if (current.equals(role)) return Map.of("role", role);
        if ("System Admin".equals(current)) requireAnotherSystemAdmin(id);
        db.update("UPDATE staff SET role = ? WHERE staff_id = ?", role, id);
        audit.log(me, "UPDATE", "staff", id, "Changed role of staff " + id + " from " + current + " to " + role);
        return Map.of("role", role);
    }

    // ---------- customers ----------

    @GetMapping("/customers")
    public List<CustomerDto> customers(@RequestParam(required = false) String q) {
        String sql = """
            SELECT c.customer_id, c.first_name || ' ' || c.last_name AS name, c.email, c.phone_number, c.account_status, c.created_at,
                   (SELECT d.verification_status FROM driver_license d WHERE d.customer_id = c.customer_id ORDER BY d.license_id DESC LIMIT 1) AS license_status
            FROM customer c
            """;
        var mapper = (org.springframework.jdbc.core.RowMapper<CustomerDto>) (rs, i) -> new CustomerDto(rs.getInt("customer_id"), rs.getString("name"),
                rs.getString("email"), rs.getString("phone_number"), Rows.up(rs.getString("account_status")), Rows.ldt(rs, "created_at"),
                Rows.up(rs.getString("license_status")));
        if (Rows.blank(q)) return db.query(sql + " ORDER BY c.customer_id DESC LIMIT 200", mapper);
        String like = "%" + q.trim().toLowerCase() + "%";
        return db.query(sql + " WHERE lower(c.first_name || ' ' || c.last_name) LIKE ? OR lower(c.email) LIKE ? ORDER BY c.customer_id DESC LIMIT 200",
                mapper, like, like);
    }

    @PostMapping("/customers/{id}/status")
    @Transactional
    public Map<String, String> customerStatus(@PathVariable int id, @RequestBody StatusRequest r, Authentication a) {
        int me = users.staffId(a);
        String status = pick(r.status(), "Active", "Suspended");
        int n = db.update("UPDATE customer SET account_status = ? WHERE customer_id = ?", status, id);
        if (n == 0) throw ApiError.notFound("Customer not found.");
        audit.log(me, "UPDATE", "customer", id, "Set customer " + id + " to " + status);
        return Map.of("status", status.toUpperCase());
    }

    // ---------- audit log and report ----------

    /** Audit log, newest first. Filters are optional: action, table, staffId, text search, and a date range (yyyy-MM-dd). */
    @GetMapping("/audit")
    public List<AuditDto> auditLog(@RequestParam(defaultValue = "100") int limit, @RequestParam(defaultValue = "0") int offset,
                                   @RequestParam(required = false) String action, @RequestParam(required = false) String table,
                                   @RequestParam(required = false) Integer staffId, @RequestParam(required = false) String q,
                                   @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        int cap = Math.max(1, Math.min(limit, 500));
        StringBuilder sql = new StringBuilder("""
            SELECT l.audit_id, s.first_name || ' ' || s.last_name AS staff, l.action_type, l.table_name, l.record_id, l.action_description, l.action_datetime
            FROM audit_log l JOIN staff s ON s.staff_id = l.staff_id WHERE 1=1
            """);
        List<Object> args = new java.util.ArrayList<>();
        if (!Rows.blank(action)) { sql.append(" AND lower(l.action_type) = lower(?)"); args.add(action.trim()); }
        if (!Rows.blank(table)) { sql.append(" AND lower(l.table_name) = lower(?)"); args.add(table.trim()); }
        if (staffId != null) { sql.append(" AND l.staff_id = ?"); args.add(staffId); }
        if (!Rows.blank(q)) { sql.append(" AND lower(coalesce(l.action_description, '')) LIKE ?"); args.add("%" + q.trim().toLowerCase() + "%"); }
        if (!Rows.blank(from)) { sql.append(" AND l.action_datetime >= ?"); args.add(Rows.date(from, "Enter the start date as yyyy-MM-dd.").atStartOfDay()); }
        if (!Rows.blank(to)) { sql.append(" AND l.action_datetime < ?"); args.add(Rows.date(to, "Enter the end date as yyyy-MM-dd.").plusDays(1).atStartOfDay()); }
        sql.append(" ORDER BY l.audit_id DESC LIMIT ? OFFSET ?");
        args.add(cap); args.add(Math.max(0, offset));
        return db.query(sql.toString(), (rs, i) -> new AuditDto(rs.getInt("audit_id"), rs.getString("staff"), rs.getString("action_type"), rs.getString("table_name"),
                Rows.intOrNull(rs, "record_id"), rs.getString("action_description"), Rows.ldt(rs, "action_datetime")), args.toArray());
    }

    /** Values for the audit page's filter drop-downs. */
    @GetMapping("/audit/options")
    public Map<String, Object> auditOptions() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("actions", db.queryForList("SELECT DISTINCT action_type FROM audit_log ORDER BY 1", String.class));
        m.put("tables", db.queryForList("SELECT DISTINCT table_name FROM audit_log ORDER BY 1", String.class));
        m.put("staff", db.queryForList("SELECT staff_id AS id, first_name || ' ' || last_name AS name FROM staff WHERE staff_id IN (SELECT DISTINCT staff_id FROM audit_log) ORDER BY 2"));
        return m;
    }

    @GetMapping("/reports/summary")
    public Map<String, Object> summary() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rentalsByStatus", counts("SELECT upper(rental_status) AS k, count(*) AS n FROM rental GROUP BY 1 ORDER BY 1"));
        out.put("vehiclesByStatus", counts("SELECT upper(status) AS k, count(*) AS n FROM vehicle GROUP BY 1 ORDER BY 1"));
        out.put("paymentsByStatus", counts("SELECT upper(payment_status) AS k, count(*) AS n FROM payment GROUP BY 1 ORDER BY 1"));
        out.put("totalPaid", db.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM payment WHERE payment_status = 'Paid'", BigDecimal.class));
        out.put("paidThisMonth", db.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM payment WHERE payment_status = 'Paid' AND date_trunc('month', payment_date) = date_trunc('month', CURRENT_TIMESTAMP)",
                BigDecimal.class));
        out.put("totalRefunded", db.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM payment WHERE payment_status = 'Refunded'", BigDecimal.class));
        out.put("unpaidDamageFees", db.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM damage_fee WHERE fee_status IN ('Assessed','Disputed')", BigDecimal.class));
        out.put("licensesAwaitingReview", db.queryForObject("SELECT count(*) FROM driver_license WHERE verification_status = 'Pending'", Integer.class));
        return out;
    }

    private Map<String, Long> counts(String sql) {
        Map<String, Long> m = new LinkedHashMap<>();
        db.query(sql, rs -> { m.put(rs.getString("k"), rs.getLong("n")); });
        return m;
    }

    private static String pick(String value, String... allowed) {
        String v = value == null ? "" : value.trim();
        for (String s : allowed) if (s.equalsIgnoreCase(v)) return s;
        throw ApiError.bad("Status must be " + String.join(" or ", allowed) + ".");
    }
}
