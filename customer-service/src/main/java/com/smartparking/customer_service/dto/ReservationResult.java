package com.smartparking.customer_service.dto;

public class ReservationResult {
    private boolean success;
    private String errorMessage;
    private Long reservationId;

    public ReservationResult(boolean success, String errorMessage, Long reservationId) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.reservationId = reservationId;
    }

    public static ReservationResult success(Long reservationId) {
        return new ReservationResult(true, null, reservationId);
    }

    public static ReservationResult failed(String errorMessage) {
        return new ReservationResult(false, errorMessage, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Long getReservationId() {
        return reservationId;
    }
}

