package com.smartparking.payment_service.dto;

public class PaymentResult {
    private final long paymentId;
    private final String status;

    public PaymentResult(long paymentId, String status) {
        this.paymentId = paymentId;
        this.status = status;
    }

    public long getPaymentId() {
        return paymentId;
    }

    public String getStatus() {
        return status;
    }
}

