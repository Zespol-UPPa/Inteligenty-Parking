package com.smartparking.accounts_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**", "/accounts/health").permitAll()
                // Internal service-to-service endpoints (for customer-service)
                .requestMatchers("/accounts/*/email", "/accounts/*/password", "/accounts/*").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            );
        return http.build();
    }
}

