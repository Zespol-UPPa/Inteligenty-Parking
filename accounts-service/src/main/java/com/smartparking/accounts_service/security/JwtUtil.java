package com.smartparking.accounts_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private final Key key;
    private final long ttlMs = 24 * 3600 * 1000L;

    public JwtUtil(@Value("${jwt.secret:change-me-to-a-strong-secret-key-of-at-least-32-chars}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = Arrays.copyOf(bytes, 32);
            Arrays.fill(padded, bytes.length, 32, (byte) '0');
            bytes = padded;
        } else if (bytes.length > 64) {
            bytes = Arrays.copyOf(bytes, 64);
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * Normalizes role string to uppercase format expected by API Gateway.
     * Maps database role format ("User", "Worker", "Admin") to JWT format ("USER", "WORKER", "ADMIN").
     * 
     * @param role Role from database (e.g., "User", "Worker", "Admin")
     * @return Normalized role in uppercase format (e.g., "USER", "WORKER", "ADMIN")
     */
    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            log.warn("Role is null or empty, defaulting to USER");
            return "USER";
        }
        
        // Case-insensitive matching and conversion to uppercase
        String roleUpper = role.trim().toUpperCase();
        
        // Map known roles - case-insensitive matching
        switch (roleUpper) {
            case "USER":
                return "USER";
            case "WORKER":
                return "WORKER";
            case "ADMIN":
                return "ADMIN";
            default:
                log.warn("Unknown role '{}', defaulting to USER", role);
                return "USER";
        }
    }

    public String generateToken(String subject, String role) {
        String normalizedRole = normalizeRole(role);
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", normalizedRole)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public JwtData validateToken(String bearer) throws JwtException {
        if (bearer == null || bearer.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        String token = bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
        if (token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be empty");
        }
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            Claims c = jws.getBody();
            String subject = c.getSubject();
            String role = c.get("role", String.class);
            if (subject == null || role == null) {
                throw new MalformedJwtException("Token missing required claims");
            }
            return new JwtData(subject, role);
        } catch (ExpiredJwtException e) {
            throw new ExpiredJwtException(e.getHeader(), e.getClaims(), "Token has expired");
        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException e) {
            throw new JwtException("Invalid token: " + e.getMessage(), e);
        }
    }
}
