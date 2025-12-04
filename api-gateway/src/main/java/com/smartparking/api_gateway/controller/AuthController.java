package com.smartparking.api_gateway.controller;

import com.smartparking.api_gateway.security.JwtUtil;
import com.smartparking.api_gateway.security.Role;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> credentials) {
        if (credentials == null) {
            throw new IllegalArgumentException("Credentials cannot be null");
        }
        String email = credentials.get("username");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        String role = credentials.getOrDefault("role", "USER");
        // Validate role
        try {
            Role.valueOf(role);
        } catch (IllegalArgumentException e) {
            role = "USER"; // Default to USER if invalid role
        }
        String token = jwtUtil.generateToken(email, role);
        return Map.of("token", token);
    }
}
