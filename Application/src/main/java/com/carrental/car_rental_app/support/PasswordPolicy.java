package com.carrental.car_rental_app.support;

import com.carrental.car_rental_app.web.ApiError;

/** One place for the password rules, used by sign-up and by change-password. */
public final class PasswordPolicy {
    private PasswordPolicy() {}

    public static void check(String pw) {
        if (pw == null || pw.length() < 8)
            throw ApiError.bad("The password must be at least 8 characters.");
        if (pw.length() > 72)   // BCrypt only uses the first 72 bytes
            throw ApiError.bad("The password must be 72 characters or fewer.");
        boolean letter = false, digit = false;
        for (char c : pw.toCharArray()) { if (Character.isLetter(c)) letter = true; if (Character.isDigit(c)) digit = true; }
        if (!letter || !digit)
            throw ApiError.bad("The password must contain at least one letter and one number.");
    }
}
