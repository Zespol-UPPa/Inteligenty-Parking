package com.smartparking.accounts_service.repo;

import com.smartparking.accounts_service.model.VerificationToken;

import java.util.List;
import java.util.Optional;

public interface VerificationTokenRepository {
    VerificationToken save(VerificationToken token);
    Optional<VerificationToken> findByToken(String token);
    List<VerificationToken> findByAccountId(Long accountId);
    void deleteExpired();
    void markAsUsed(Long tokenId);
}

