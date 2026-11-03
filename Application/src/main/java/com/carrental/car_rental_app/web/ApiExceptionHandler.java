package com.carrental.car_rental_app.web;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

/** Turns database rule violations and oversized uploads into messages the front end can show. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException e) {
        // 23P01 = exclusion violation: the database's last line of defence against two bookings for one car at the same time.
        if (e.getMostSpecificCause() instanceof java.sql.SQLException sql && "23P01".equals(sql.getSQLState()))
            return body(HttpStatus.CONFLICT, "This car was just booked for those dates by someone else. Try other dates.");
        return body(HttpStatus.CONFLICT, "That change conflicts with existing records or breaks a data rule. Check the values and try again.");
    }

    /** Makes sure the front end always gets {"message": "..."} for errors such as wrong password, too many attempts, and validation. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException e) {
        String msg = e.getReason() == null || e.getReason().isBlank() ? "The request could not be completed." : e.getReason();
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("status", e.getStatusCode().value(), "message", msg));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> tooBig(MaxUploadSizeExceededException e) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "That file is too large. The limit is 5 MB per image.");
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(Map.of("status", s.value(), "message", msg));
    }
}
