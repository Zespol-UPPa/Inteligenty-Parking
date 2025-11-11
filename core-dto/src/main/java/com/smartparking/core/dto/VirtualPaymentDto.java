package com.smartparking.core.dto;

import com.smartparking.core.enums.StatusPaid;
import com.smartparking.core.interfaces.Identifiable;
import java.time.Instant;

public final class VirtualPaymentDto implements Identifiable {
    private final Long id;
    private final int amountMinor;
    private final String currencyCode;
    private final StatusPaid type;
    private final Instant date;
    private final Long accountId;
    private final Long sessionId;

    public VirtualPaymentDto(Long id, int amountMinor, String currencyCode, StatusPaid type, Instant date, Long accountId, Long sessionId) {
        this.id = id;
        this.amountMinor = amountMinor;
        this.currencyCode = currencyCode;
        this.type = type;
        this.date = date;
        this.accountId = accountId;
        this.sessionId = sessionId;
    }

    @Override
    public Long getId() { return id; }
    public int getAmountMinor() { return amountMinor; }
    public String getCurrencyCode() { return currencyCode; }
    public StatusPaid getType() { return type; }
    public Instant getDate() { return date; }
    public Long getAccountId() { return accountId; }
    public Long getSessionId() { return sessionId; }
}
