package com.carrental.car_rental_app.support;

import com.carrental.car_rental_app.web.ApiError;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/** Looks up who is calling. The JWT only carries the email and role, so ids come from the database. */
@Component
public class CurrentUser {
    private static final Set<String> STAFF_ROLES = Set.of("STAFF", "FINANCE", "ADMIN");   // System Admin is technical only, so it is not operational staff
    private final JdbcTemplate db;

    public CurrentUser(JdbcTemplate db) { this.db = db; }

    public String role(Authentication a) {
        return a.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .filter(s -> s.startsWith("ROLE_")).map(s -> s.substring(5)).findFirst().orElse("");
    }

    public boolean isStaff(Authentication a) { return STAFF_ROLES.contains(role(a)); }
    public boolean isCustomer(Authentication a) { return "CUSTOMER".equals(role(a)); }

    /** Id of the active customer behind this token. */
    public int customerId(Authentication a) {
        List<Integer> ids = db.queryForList(
                "SELECT customer_id FROM customer WHERE lower(email) = lower(?) AND account_status = 'Active'", Integer.class, a.getName());
        if (ids.isEmpty()) throw ApiError.forbidden("Only active customer accounts can do this.");
        return ids.get(0);
    }

    /** Id of the active staff member behind this token. Deactivated staff are refused even if their token is still valid. */
    public int staffId(Authentication a) {
        List<Integer> ids = db.queryForList(
                "SELECT staff_id FROM staff WHERE lower(email) = lower(?) AND account_status = 'Active'", Integer.class, a.getName());
        if (ids.isEmpty()) throw ApiError.forbidden("Only active staff accounts can do this.");
        return ids.get(0);
    }
}
