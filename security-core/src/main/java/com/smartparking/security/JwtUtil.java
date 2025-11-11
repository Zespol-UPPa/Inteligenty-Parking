package com.smartparking.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class JwtUtil {
    private final SecretKey key;
    private final long expirationSeconds;

    public JwtUtil(SecretKey key, long expirationSeconds) {
        this.key = key;
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(String subject, Role role) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(expirationSeconds);
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role.name())
                .setIssuedAt(java.util.Date.from(now))
                .setExpiration(java.util.Date.from(expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public JwtData validateToken(String token){
        try{
            String clean = token.startsWith("Bearer ") ? token.substring(7) : token;
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(clean);
            String subject = claims.getBody().getSubject();
            String roleStr = claims.getBody().get("role", String.class);
            Role role = Role.valueOf(roleStr);
            Instant expiration = claims.getBody().getExpiration().toInstant();
            return new JwtData(subject, role, expiration);
        } catch (JwtException | IllegalArgumentException e) {
            throw new SecurityException("Invalid JWT token" + e.getMessage(), e);
        }
    }
}
