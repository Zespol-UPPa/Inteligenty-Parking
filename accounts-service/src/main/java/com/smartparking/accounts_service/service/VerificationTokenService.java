package com.smartparking.accounts_service.service;

import com.smartparking.accounts_service.model.VerificationToken;
import com.smartparking.accounts_service.repo.VerificationTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

@Service
public class VerificationTokenService {
    private static final int TOKEN_LENGTH = 64;
    private static final int EXPIRY_HOURS = 24;
    
    private final VerificationTokenRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    public VerificationTokenService(VerificationTokenRepository repository) {
        this.repository = repository;
    }

    public VerificationToken generateToken(Long accountId) {
        String token = generateRandomToken();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(EXPIRY_HOURS * 3600L);
        
        VerificationToken verificationToken = new VerificationToken(token, accountId, expiresAt);
        verificationToken.setCreatedAt(now);
        return repository.save(verificationToken);
    }

    public Optional<VerificationToken> validateToken(String token) {
        Optional<VerificationToken> tokenOpt = repository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }
        
        VerificationToken verificationToken = tokenOpt.get();
        if (verificationToken.isValid()) {
            return Optional.of(verificationToken);
        }
        
        return Optional.empty();
    }

    public void markAsUsed(Long tokenId) {
        repository.markAsUsed(tokenId);
    }

    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    public void cleanupExpiredTokens() {
        repository.deleteExpired();
    }

    private String generateRandomToken() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return token.toString();
    }
}

