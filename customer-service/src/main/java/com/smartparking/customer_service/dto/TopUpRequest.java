package com.smartparking.customer_service.dto;

public class TopUpRequest {
    private Long amountMinor; // w groszach/centach
    private String paymentMethod; // "blik", "bank_transfer", etc.

    public TopUpRequest() {
    }

    public TopUpRequest(Long amountMinor, String paymentMethod) {
        this.amountMinor = amountMinor;
        this.paymentMethod = paymentMethod;
    }

    public Long getAmountMinor() {
        return amountMinor;
    }

    public void setAmountMinor(Long amountMinor) {
        this.amountMinor = amountMinor;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}

