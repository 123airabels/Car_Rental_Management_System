package com.carrental.car_rental_app.web;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/** Small helpers shared by the controllers. */
final class Rows {
    private Rows() {}

    static LocalDateTime ldt(ResultSet rs, String col) throws SQLException { return rs.getObject(col, LocalDateTime.class); }
    static LocalDate ld(ResultSet rs, String col) throws SQLException { return rs.getObject(col, LocalDate.class); }
    static Integer intOrNull(ResultSet rs, String col) throws SQLException { return rs.getObject(col, Integer.class); }

    /** Statuses go to the front end in capitals, like the existing rental and vehicle endpoints. */
    static String up(String s) { return s == null ? null : s.toUpperCase(); }

    static boolean blank(String s) { return s == null || s.isBlank(); }

    /** Trimmed text, or null when empty. */
    static String clean(String s) { return blank(s) ? null : s.trim(); }

    static LocalDate date(String s, String message) {
        try { return LocalDate.parse(s == null ? "" : s.trim()); }
        catch (DateTimeParseException e) { throw ApiError.bad(message); }
    }
}
