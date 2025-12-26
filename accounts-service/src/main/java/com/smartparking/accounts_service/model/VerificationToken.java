package com.smartparking.accounts_service.model;

import java.time.Instant;

public class VerificationToken {
    private Long id;
    private String token;
    private Long accountId;
    private Instant createdAt;
    private Instant expiresAt;
    private Boolean isUsed;

    public VerificationToken() {}

    public VerificationToken(String token, Long accountId, Instant expiresAt) {
        this.token = token;
        this.accountId = accountId;
        this.expiresAt = expiresAt;
        this.isUsed = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getIsUsed() {
        return isUsed;
    }

    public void setIsUsed(Boolean isUsed) {
        this.isUsed = isUsed;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public boolean isValid() {
        return !isUsed && !isExpired();
    }
}

