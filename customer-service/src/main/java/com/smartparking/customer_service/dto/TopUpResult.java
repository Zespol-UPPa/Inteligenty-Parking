package com.smartparking.customer_service.dto;

import java.math.BigDecimal;

public class TopUpResult {
    private boolean success;
    private Long paymentId;
    private BigDecimal newBalance;
    private String errorMessage;

    private TopUpResult(boolean success, Long paymentId, BigDecimal newBalance, String errorMessage) {
        this.success = success;
        this.paymentId = paymentId;
        this.newBalance = newBalance;
        this.errorMessage = errorMessage;
    }

    public static TopUpResult success(Long paymentId, BigDecimal newBalance) {
        return new TopUpResult(true, paymentId, newBalance, null);
    }

    public static TopUpResult failed(String errorMessage) {
        return new TopUpResult(false, null, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}

