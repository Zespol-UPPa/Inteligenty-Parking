package com.smartparking.api_gateway.controller;

import com.smartparking.security.JwtUtil;
import com.smartparking.security.Role;
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
        String email = credentials.get("username");
        String role = credentials.getOrDefault("role", "USER");
        String token = jwtUtil.generateToken(email, Role.valueOf(role));
        return Map.of("token", token);
    }
}
