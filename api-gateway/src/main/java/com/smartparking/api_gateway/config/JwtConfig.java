package com.smartparking.api_gateway.config;

import com.smartparking.security.JwtUtil;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {
    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expiration}")
    private Long expiration;

    @Bean
    public SecretKey jwtSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes((StandardCharsets.UTF_8)));
    }

    @Bean
    public JwtUtil jwtUtil(SecretKey jwtSecretKey) {
        return new JwtUtil(jwtSecretKey, expiration);
    }
}
