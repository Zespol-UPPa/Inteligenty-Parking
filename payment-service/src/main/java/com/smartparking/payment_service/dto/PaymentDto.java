package com.smartparking.payment_service.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentDto {
    public long id;
    public long accountId;
    public BigDecimal amount;
    public PaymentStatus status;
    public Instant date;
    public String source;

    public PaymentDto(long id, long accountId, BigDecimal amount, PaymentStatus status, Instant date, String source) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.status = status;
        this.date = date;
        this.source = source;
    }
}
