package com.smartparking.email_service.dto;

import java.math.BigDecimal;

public class TopUpConfirmationEvent {
    private String email;
    private Long accountId;
    private Long amountMinor;
    private BigDecimal newBalance;

    public TopUpConfirmationEvent() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getAmountMinor() {
        return amountMinor;
    }

    public void setAmountMinor(Long amountMinor) {
        this.amountMinor = amountMinor;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(BigDecimal newBalance) {
        this.newBalance = newBalance;
    }
}

