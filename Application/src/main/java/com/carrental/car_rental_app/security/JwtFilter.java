package com.carrental.car_rental_app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final JdbcTemplate db;

    public JwtFilter(JwtService jwt, JdbcTemplate db) { this.jwt = jwt; this.db = db; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Claims c = null;
            try {
                c = jwt.parse(header.substring(7));
            } catch (JwtException | IllegalArgumentException e) {
                // Bad or expired token: continue unauthenticated; protected routes answer 401.
            }
            // Only real login tokens count. Email-confirmation tokens have no role and must never authenticate a request.
            String role = c == null ? null : c.get("role", String.class);
            if (role != null && c.get("purpose") == null && !"CUSTOMER".equals(role)) {
                // Staff roles are re-read from the database on every request, so a role change or deactivation takes effect
                // immediately instead of when the token expires.
                var current = db.queryForList("SELECT role FROM staff WHERE lower(email) = lower(?) AND account_status = 'Active'",
                        String.class, c.getSubject());
                role = current.isEmpty() ? null : current.get(0).trim().toUpperCase().replace(' ', '_');
            }
            if (role != null && c.get("purpose") == null) {
                var auth = new UsernamePasswordAuthenticationToken(c.getSubject(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(req, res);
    }
}
