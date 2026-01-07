package com.smartparking.customer_service.config;

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
                        .requestMatchers("/customer/health", "/internal/wallet/**", "/customer/internal/**", "/internal/vehicles/**").permitAll()
                        // All other endpoints require JWT (handled by JwtContextFilter)
                        .anyRequest().permitAll() // JwtContextFilter will handle authentication
                );
        return http.build();
    }
}
