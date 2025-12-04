package com.smartparking.api_gateway.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {
    @Value("${jwt.secret:change-me-to-a-strong-secret-key-of-at-least-32-chars}")
    private String secret;
    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    @Bean
    public SecretKey jwtSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes((StandardCharsets.UTF_8)));
    }

}
