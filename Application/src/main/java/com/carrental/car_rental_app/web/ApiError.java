package com.carrental.car_rental_app.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ApiError {
    private ApiError() {}
    public static ResponseStatusException bad(String msg) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg); }
    public static ResponseStatusException forbidden(String msg) { return new ResponseStatusException(HttpStatus.FORBIDDEN, msg); }
    public static ResponseStatusException notFound(String msg) { return new ResponseStatusException(HttpStatus.NOT_FOUND, msg); }
    public static ResponseStatusException tooMany(String msg) { return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, msg); }
    public static ResponseStatusException conflict(String msg) { return new ResponseStatusException(HttpStatus.CONFLICT, msg); }
}
