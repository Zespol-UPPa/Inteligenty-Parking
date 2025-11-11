package com.smartparking.core.dto;

import com.smartparking.core.interfaces.Identifiable;
import com.smartparking.core.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO reprezentujące płatność.
 * Używane przez serwisy Payment i Admin.
 */
public class PaymentDto implements Identifiable {

    private Long id;
    private Long userId;
    private BigDecimal amount;
    private PaymentStatus status;
    private Instant createdAt;
    private String externalTransactionId;

    public PaymentDto() {}

    public PaymentDto(Long id, Long userId, BigDecimal amount, PaymentStatus status,
                      Instant createdAt, String externalTransactionId) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.externalTransactionId = externalTransactionId;
    }

    @Override
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getExternalTransactionId() { return externalTransactionId; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setExternalTransactionId(String externalTransactionId) { this.externalTransactionId = externalTransactionId; }
}
