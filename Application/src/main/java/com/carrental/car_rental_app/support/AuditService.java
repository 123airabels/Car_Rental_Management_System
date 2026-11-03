package com.carrental.car_rental_app.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Writes to audit_log. The table requires a staff id, so only staff actions are recorded. */
@Component
public class AuditService {
    private final JdbcTemplate db;

    public AuditService(JdbcTemplate db) { this.db = db; }

    public void log(int staffId, String action, String table, Integer recordId, String description) {
        db.update("INSERT INTO audit_log (staff_id, action_type, table_name, record_id, action_description) VALUES (?,?,?,?,?)",
                staffId, action, table, recordId, description);
    }
}
