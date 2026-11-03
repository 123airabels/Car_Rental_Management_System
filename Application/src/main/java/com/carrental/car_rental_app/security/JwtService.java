package com.carrental.car_rental_app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String PURPOSE_VERIFY = "verify";

    private final SecretKey key;
    private final long expirationMs;
    private final long verifyMs;

    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs,
                      @Value("${app.security.verify-token-hours:24}") long verifyHours) {
        byte[] bytes;
        if (secret == null || secret.isBlank() || secret.startsWith("change-this-secret")) {
            bytes = new byte[48];
            new SecureRandom().nextBytes(bytes);
            log.warn("JWT_SECRET is not set. Using a random secret for this run; everyone will be logged out when the app restarts. "
                    + "Set the JWT_SECRET environment variable (32+ random characters) to keep sessions across restarts.");
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET must be at least 32 characters long.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
        this.verifyMs = verifyHours * 3_600_000L;
    }

    public String create(String email, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /** A signed, expiring token for the email-confirmation link. It carries no role, so it can never be used to call the API. */
    public String createVerifyToken(String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("purpose", PURPOSE_VERIFY)
                .issuedAt(new Date(now))
                .expiration(new Date(now + verifyMs))
                .signWith(key)
                .compact();
    }

    /** @return the email inside a valid confirmation token; throws JwtException or IllegalArgumentException otherwise. */
    public String parseVerifyToken(String token) {
        Claims c = parse(token);
        if (!PURPOSE_VERIFY.equals(c.get("purpose", String.class)) || c.getSubject() == null)
            throw new JwtException("Not a confirmation token.");
        return c.getSubject();
    }
}
