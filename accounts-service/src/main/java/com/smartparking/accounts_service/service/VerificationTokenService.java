package com.smartparking.accounts_service.service;

import com.smartparking.accounts_service.model.VerificationToken;
import com.smartparking.accounts_service.repo.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class VerificationTokenService {
    private static final Logger log = LoggerFactory.getLogger(VerificationTokenService.class);
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
            String tokenPreview = token.length() > 16 
                ? token.substring(0, 8) + "..." + token.substring(token.length() - 8)
                : "***";
            log.warn("Token not found in database: token={}", tokenPreview);
            return Optional.empty();
        }
        
        VerificationToken verificationToken = tokenOpt.get();
        if (!verificationToken.isValid()) {
            boolean isUsed = Boolean.TRUE.equals(verificationToken.getIsUsed());
            boolean isExpired = verificationToken.isExpired();
            log.warn("Token is invalid: tokenId={}, accountId={}, isUsed={}, isExpired={}", 
                verificationToken.getId(), verificationToken.getAccountId(), isUsed, isExpired);
            return Optional.empty();
        }
        
        return Optional.of(verificationToken);
    }

    public void markAsUsed(Long tokenId) {
        repository.markAsUsed(tokenId);
    }

    public List<VerificationToken> findByAccountId(Long accountId) {
        return repository.findByAccountId(accountId);
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

