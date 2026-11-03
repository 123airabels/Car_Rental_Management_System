package com.carrental.car_rental_app.web;

import com.carrental.car_rental_app.support.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Rental terms and conditions. The customer accepts them once per rental; the accepted text is stored with the rental. */
@RestController
@RequestMapping("/api/rentals/{id}/terms")
public class TermsController {
    private final JdbcTemplate db;
    private final CurrentUser users;

    public TermsController(JdbcTemplate db, CurrentUser users) { this.db = db; this.users = users; }

    record TermsDto(String version, String text, boolean accepted, LocalDateTime acceptedAt) {}

    static final String VERSION = "v1.0";
    // Sample wording. Replace it with the business's real terms before going live.
    static final String TEXT = """
        1. The renter must hold a valid driver's license for the whole rental period.
        2. The vehicle must be returned on the agreed date and time. Late returns are charged extra days at the booked daily rate.
        3. The renter is responsible for damage that happens during the rental and is not caused by normal wear.
        4. The vehicle must be returned with the same fuel level it had at pickup.
        5. Smoking and transporting prohibited items in the vehicle are not allowed.
        6. Cancellations and refunds follow the shop's cancellation policy.
        """;

    @GetMapping
    public TermsDto get(@PathVariable int id, Authentication a) {
        assertAccess(id, a);
        List<TermsDto> rows = db.query("SELECT terms_version, terms_text, accepted, accepted_at FROM rental_terms WHERE rental_id = ?",
                (rs, i) -> new TermsDto(rs.getString("terms_version"), rs.getString("terms_text"), rs.getBoolean("accepted"),
                        Rows.ldt(rs, "accepted_at")), id);
        return rows.isEmpty() ? new TermsDto(VERSION, TEXT, false, null) : rows.get(0);
    }

    @PostMapping("/accept")
    @Transactional
    public TermsDto accept(@PathVariable int id, Authentication a) {
        int customerId = users.customerId(a);
        List<String> st = db.queryForList("SELECT rental_status FROM rental WHERE rental_id = ? AND customer_id = ? FOR UPDATE", String.class, id, customerId);
        if (st.isEmpty()) throw ApiError.notFound("Rental not found.");
        if (List.of("Cancelled", "Rejected").contains(st.get(0))) throw ApiError.bad("This rental was " + st.get(0).toLowerCase() + ".");
        db.update("INSERT INTO rental_terms (rental_id, terms_version, terms_text, accepted, accepted_at) VALUES (?,?,?,true,CURRENT_TIMESTAMP)"
                + " ON CONFLICT (rental_id) DO UPDATE SET accepted = true, accepted_at = CURRENT_TIMESTAMP,"
                + " terms_version = EXCLUDED.terms_version, terms_text = EXCLUDED.terms_text", id, VERSION, TEXT);
        return get(id, a);
    }

    private void assertAccess(int rentalId, Authentication a) {
        if (users.isStaff(a)) return;
        int customerId = users.customerId(a);
        boolean mine = Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM rental WHERE rental_id = ? AND customer_id = ?)", Boolean.class, rentalId, customerId));
        if (!mine) throw ApiError.notFound("Rental not found.");
    }
}
