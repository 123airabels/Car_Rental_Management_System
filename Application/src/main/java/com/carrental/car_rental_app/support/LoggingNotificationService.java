package com.carrental.car_rental_app.support;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Placeholder: writes the message to the application log instead of sending it. */
@Component
public class LoggingNotificationService implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationService.class);
    private final JdbcTemplate db;

    public LoggingNotificationService(JdbcTemplate db) { this.db = db; }

    @Override
    public void notifyCustomer(int customerId, String subject, String message) {
        List<Map<String, Object>> c = db.queryForList("SELECT email, phone_number FROM customer WHERE customer_id = ?", customerId);
        if (!c.isEmpty()) log.info("[NOTIFY] to={} phone={} | {} | {}", c.get(0).get("email"), c.get(0).get("phone_number"), subject, message);
    }
}
