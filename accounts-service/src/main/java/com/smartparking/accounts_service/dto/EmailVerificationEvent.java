package com.smartparking.accounts_service.dto;

public class EmailVerificationEvent {
    private String email;
    private String token;
    private Long accountId;
    private String verificationUrl;

    public EmailVerificationEvent() {}

    public EmailVerificationEvent(String email, String token, Long accountId, String verificationUrl) {
        this.email = email;
        this.token = token;
        this.accountId = accountId;
        this.verificationUrl = verificationUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getVerificationUrl() {
        return verificationUrl;
    }

    public void setVerificationUrl(String verificationUrl) {
        this.verificationUrl = verificationUrl;
    }
}

