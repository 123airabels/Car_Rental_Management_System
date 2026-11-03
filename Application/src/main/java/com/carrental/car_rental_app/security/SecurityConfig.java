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
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error", "/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/vehicles", "/api/vehicles/*", "/api/vehicles/*/availability", "/api/categories", "/api/terms").permitAll()
                // Admin and System Admin only: staff accounts, audit log, categories, rates, adding/editing vehicles.
                .requestMatchers("/api/admin/staff/**", "/api/admin/audit/**", "/api/admin/categories/**", "/api/admin/rates/**")
                    .hasAnyRole("ADMIN", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/admin/vehicles/**").hasAnyRole("ADMIN", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/admin/vehicles/**").hasAnyRole("ADMIN", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/vehicles/**").hasAnyRole("ADMIN", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/vehicles/*/rentals").hasAnyRole("STAFF", "ADMIN", "SYSTEM_ADMIN")
                // Finance (and Admin): confirming, rejecting and refunding payments, and the summary report.
                .requestMatchers("/api/admin/payments/**", "/api/admin/reports/**").hasAnyRole("FINANCE", "ADMIN", "SYSTEM_ADMIN")
                // Customer records (contact details, rental history): Staff, Admin and System Admin. Finance does not need them.
                // Suspending or re-activating a customer is limited to Admin and System Admin.
                .requestMatchers(HttpMethod.POST, "/api/admin/customers/*/status").hasAnyRole("ADMIN", "SYSTEM_ADMIN")
                .requestMatchers("/api/admin/customers/**").hasAnyRole("STAFF", "ADMIN", "SYSTEM_ADMIN")
                // Everything else under /api/admin: staff can read; changes need Staff, Admin or System Admin.
                .requestMatchers(HttpMethod.GET, "/api/admin/**").hasAnyRole("STAFF", "FINANCE", "ADMIN", "SYSTEM_ADMIN")
                .requestMatchers("/api/admin/**").hasAnyRole("STAFF", "ADMIN", "SYSTEM_ADMIN")
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
        c.setAllowedOrigins(origins);
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", c);
        return source;
    }
}
