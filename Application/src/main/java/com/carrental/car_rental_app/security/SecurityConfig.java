package com.carrental.car_rental_app.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain chain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        http.csrf(c -> c.disable())   // safe: no cookies or sessions, the token is sent in the Authorization header
            .cors(Customizer.withDefaults())
            .headers(h -> h
                // The strict policy is for API responses only; the web pages served by this app load Tailwind/fonts from CDNs and use inline scripts.
                .addHeaderWriter((req, res) -> {
                    if (req.getRequestURI().startsWith("/api/"))
                        res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
                })
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error", "/api/auth/**").permitAll()
                // The website itself (Frontend folder) is public; every page then calls /api with its own login token.
                .requestMatchers(HttpMethod.GET, "/", "/*.html", "/js/**", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/vehicles", "/api/vehicles/*", "/api/vehicles/*/availability", "/api/categories", "/api/terms").permitAll()
                // System Admin is technical only: staff accounts, role assignments and the audit log (plus its own account/password).
                // It is deliberately NOT in any rule below, so it cannot touch bookings, customers, payments, vehicles or damages.
                // Admin may also manage Staff and Finance accounts, but AdministrationController refuses an Admin any change
                // to Admin or System Admin accounts, so only a System Admin can hand out those roles.
                .requestMatchers("/api/admin/staff/**").hasAnyRole("ADMIN", "SYSTEM_ADMIN")
                // The audit log is for oversight, so Admin and System Admin can read it.
                .requestMatchers("/api/admin/audit/**").hasAnyRole("ADMIN", "SYSTEM_ADMIN")
                // Admin only: categories, rates, adding/editing vehicles.
                .requestMatchers("/api/admin/categories/**", "/api/admin/rates/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/admin/vehicles/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/admin/vehicles/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/vehicles/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/vehicles/*/rentals").hasAnyRole("STAFF", "ADMIN")
                // Finance (and Admin): confirming, rejecting and refunding payments, and the summary report.
                .requestMatchers("/api/admin/payments/**", "/api/admin/reports/**").hasAnyRole("FINANCE", "ADMIN")
                // Finance (and Staff, Admin) can set damage fees; Finance has no other damage rights.
                .requestMatchers(HttpMethod.PUT, "/api/admin/damages/*/fee").hasAnyRole("STAFF", "FINANCE", "ADMIN")
                // Customer records (contact details, rental history): Staff and Admin. Finance does not need them.
                // Suspending or re-activating a customer is limited to Admin. Editing a customer's details is limited to Staff (Admin is view-only).
                .requestMatchers(HttpMethod.POST, "/api/admin/customers/*/status").hasRole("ADMIN")
                // Customers' personal information: Staff can edit it, Admin can only view it.
                .requestMatchers(HttpMethod.PUT, "/api/admin/customers/*").hasRole("STAFF")
                .requestMatchers("/api/admin/customers/**").hasAnyRole("STAFF", "ADMIN")
                // Everything else under /api/admin: staff can read; changes need Staff or Admin.
                .requestMatchers(HttpMethod.GET, "/api/admin/**").hasAnyRole("STAFF", "FINANCE", "ADMIN")
                .requestMatchers("/api/admin/**").hasAnyRole("STAFF", "ADMIN")
                // Shared customer/staff endpoints (rentals, inspections, licenses, payments, damage fees): everyone except System Admin.
                .requestMatchers("/api/rentals/**", "/api/inspections/**", "/api/inspection-photos/**", "/api/license/**",
                                 "/api/payments/**", "/api/payments", "/api/damage-fees/**", "/api/account/profile")
                    .hasAnyRole("CUSTOMER", "STAFF", "FINANCE", "ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origins}") List<String> origins) {
        var c = new CorsConfiguration();
        c.setAllowedOriginPatterns(origins);   // patterns, so entries like http://192.168.*.*:5500 work
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", c);
        return source;
    }
}
