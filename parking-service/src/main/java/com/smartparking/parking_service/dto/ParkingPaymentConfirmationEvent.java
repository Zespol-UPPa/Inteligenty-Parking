package com.smartparking.parking_service.dto;

import java.time.Instant;

public class ParkingPaymentConfirmationEvent {
    private String email;
    private Long accountId;
    private Long sessionId;
    private Instant entryTime;
    private Instant exitTime;
    private Long amountMinor;
    private Long durationMinutes;

    public ParkingPaymentConfirmationEvent() {
    }

    public ParkingPaymentConfirmationEvent(String email, Long accountId, Long sessionId,
                                          Instant entryTime, Instant exitTime,
                                          Long amountMinor, Long durationMinutes) {
        this.email = email;
        this.accountId = accountId;
        this.sessionId = sessionId;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.amountMinor = amountMinor;
        this.durationMinutes = durationMinutes;
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

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Instant getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(Instant entryTime) {
        this.entryTime = entryTime;
    }

    public Instant getExitTime() {
        return exitTime;
    }

    public void setExitTime(Instant exitTime) {
        this.exitTime = exitTime;
    }

    public Long getAmountMinor() {
        return amountMinor;
    }

    public void setAmountMinor(Long amountMinor) {
        this.amountMinor = amountMinor;
    }

    public Long getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}

