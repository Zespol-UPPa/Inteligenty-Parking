package com.smartparking.accounts_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

@Component
public class JwtUtil {
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

    public String generateToken(String subject, String role) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
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
