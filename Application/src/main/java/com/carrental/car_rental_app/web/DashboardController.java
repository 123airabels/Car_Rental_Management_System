package com.carrental.car_rental_app.web;

import com.carrental.car_rental_app.support.CurrentUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Numbers for the staff dashboard. Open to every staff role (see SecurityConfig: GET /api/admin/**),
 * but money figures are only included for Finance, Admin and System Admin, and recent activity only for Admin and System Admin.
 */
@RestController
public class DashboardController {
    private static final Set<String> MONEY = Set.of("FINANCE", "ADMIN", "SYSTEM_ADMIN");
    private static final Set<String> ACTIVITY = Set.of("ADMIN", "SYSTEM_ADMIN");
    private final JdbcTemplate db;
    private final CurrentUser users;

    public DashboardController(JdbcTemplate db, CurrentUser users) { this.db = db; this.users = users; }

    private int count(String sql) {
        Integer n = db.queryForObject(sql, Integer.class);
        return n == null ? 0 : n;
    }

    private Map<String, Long> counts(String sql) {
        Map<String, Long> m = new LinkedHashMap<>();
        db.query(sql, rs -> { m.put(rs.getString("k"), rs.getLong("n")); });
        return m;
    }

    @GetMapping("/api/admin/dashboard")
    public Map<String, Object> dashboard(Authentication a) {
        String role = users.role(a);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("role", role);

        // Work that needs someone's attention today
        Map<String, Object> todo = new LinkedHashMap<>();
        todo.put("pendingApprovals", count("SELECT count(*) FROM rental WHERE rental_status = 'Pending'"));
        todo.put("pickupsToday", count("SELECT count(*) FROM rental WHERE rental_status = 'Approved' AND pickup_datetime::date <= CURRENT_DATE"));
        todo.put("returnsDueToday", count("SELECT count(*) FROM rental WHERE rental_status = 'Active' AND return_datetime::date = CURRENT_DATE"));
        todo.put("overdueReturns", count("SELECT count(*) FROM rental WHERE rental_status = 'Active' AND return_datetime < CURRENT_TIMESTAMP"));
        todo.put("licensesToReview", count("SELECT count(*) FROM driver_license WHERE verification_status = 'Pending'"));
        todo.put("paymentsToConfirm", count("SELECT count(*) FROM payment WHERE payment_status = 'Pending'"));
        todo.put("disputedFees", count("SELECT count(*) FROM damage_fee WHERE fee_status = 'Disputed'"));
        todo.put("damageWithoutFee", count("SELECT count(*) FROM damage_incident d WHERE d.status <> 'Resolved' AND NOT EXISTS (SELECT 1 FROM damage_fee f WHERE f.damage_id = d.damage_id)"));
        todo.put("maintenanceOpen", count("SELECT count(*) FROM vehicle_maintenance WHERE status IN ('Pending','In Progress')"));
        out.put("attention", todo);

        out.put("rentalsByStatus", counts("SELECT upper(rental_status) AS k, count(*) AS n FROM rental GROUP BY 1 ORDER BY 1"));
        out.put("fleetByStatus", counts("SELECT upper(status) AS k, count(*) AS n FROM vehicle GROUP BY 1 ORDER BY 1"));
        out.put("totalCustomers", count("SELECT count(*) FROM customer"));
        out.put("newCustomersThisMonth", count("SELECT count(*) FROM customer WHERE date_trunc('month', created_at) = date_trunc('month', CURRENT_TIMESTAMP)"));

        // Rentals booked per month, last 6 months
        out.put("bookingsByMonth", db.query("""
            SELECT to_char(m, 'YYYY-MM') AS month, count(r.rental_id) AS n
            FROM generate_series(date_trunc('month', CURRENT_DATE) - interval '5 months', date_trunc('month', CURRENT_DATE), interval '1 month') m
            LEFT JOIN rental r ON date_trunc('month', r.created_at) = m GROUP BY m ORDER BY m
            """, (rs, i) -> Map.<String, Object>of("month", rs.getString("month"), "value", rs.getLong("n"))));

        // Active rentals, soonest return first
        out.put("upcomingReturns", db.query("""
            SELECT r.rental_id, c.first_name || ' ' || c.last_name AS customer, v.make || ' ' || v.model AS vehicle, v.plate_number,
                   r.return_datetime, r.return_datetime < CURRENT_TIMESTAMP AS overdue
            FROM rental r JOIN customer c ON c.customer_id = r.customer_id JOIN vehicle v ON v.vehicle_id = r.vehicle_id
            WHERE r.rental_status = 'Active' ORDER BY r.return_datetime LIMIT 8
            """, (rs, i) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getInt("rental_id")); m.put("customer", rs.getString("customer")); m.put("vehicle", rs.getString("vehicle"));
                m.put("plateNumber", rs.getString("plate_number")); m.put("returnBy", Rows.ldt(rs, "return_datetime")); m.put("overdue", rs.getBoolean("overdue"));
                return m;
            }));

        if (MONEY.contains(role)) {
            Map<String, Object> money = new LinkedHashMap<>();
            money.put("paidThisMonth", db.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM payment WHERE payment_status = 'Paid' AND date_trunc('month', payment_date) = date_trunc('month', CURRENT_TIMESTAMP)", java.math.BigDecimal.class));
            money.put("totalPaid", db.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM payment WHERE payment_status = 'Paid'", java.math.BigDecimal.class));
            money.put("totalRefunded", db.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM payment WHERE payment_status = 'Refunded'", java.math.BigDecimal.class));
            money.put("unpaidDamageFees", db.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM damage_fee WHERE fee_status IN ('Assessed','Disputed')", java.math.BigDecimal.class));
            money.put("unpaidLateFees", db.queryForObject("SELECT COALESCE(SUM(late_fee), 0) FROM rental WHERE late_fee_status = 'Unpaid'", java.math.BigDecimal.class));
            money.put("revenueByMonth", db.query("""
                SELECT to_char(m, 'YYYY-MM') AS month, COALESCE(SUM(p.amount), 0) AS n
                FROM generate_series(date_trunc('month', CURRENT_DATE) - interval '5 months', date_trunc('month', CURRENT_DATE), interval '1 month') m
                LEFT JOIN payment p ON date_trunc('month', p.payment_date) = m AND p.payment_status = 'Paid' GROUP BY m ORDER BY m
                """, (rs, i) -> Map.<String, Object>of("month", rs.getString("month"), "value", rs.getBigDecimal("n"))));
            out.put("money", money);
        }
        if (ACTIVITY.contains(role)) {
            out.put("recentActivity", db.query("""
                SELECT l.audit_id, s.first_name || ' ' || s.last_name AS staff, l.action_type, l.table_name, l.record_id, l.action_description, l.action_datetime
                FROM audit_log l JOIN staff s ON s.staff_id = l.staff_id ORDER BY l.audit_id DESC LIMIT 8
                """, (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt("audit_id")); m.put("staff", rs.getString("staff")); m.put("action", rs.getString("action_type"));
                    m.put("table", rs.getString("table_name")); m.put("recordId", Rows.intOrNull(rs, "record_id"));
                    m.put("description", rs.getString("action_description")); m.put("at", Rows.ldt(rs, "action_datetime"));
                    return m;
                }));
        }
        return out;
    }
}
