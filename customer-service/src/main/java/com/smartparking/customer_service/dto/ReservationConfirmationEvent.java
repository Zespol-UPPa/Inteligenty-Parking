package com.smartparking.customer_service.dto;

import java.time.Instant;

public class ReservationConfirmationEvent {
    private String email;
    private Long accountId;
    private Long reservationId;
    private Long parkingId;
    private Long spotId;
    private Instant startTime;
    private Instant endTime;
    private String parkingName;

    public ReservationConfirmationEvent() {
    }

    public ReservationConfirmationEvent(String email, Long accountId, Long reservationId, 
                                       Long parkingId, Long spotId, Instant startTime, 
                                       Instant endTime, String parkingName) {
        this.email = email;
        this.accountId = accountId;
        this.reservationId = reservationId;
        this.parkingId = parkingId;
        this.spotId = spotId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.parkingName = parkingName;
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

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public Long getParkingId() {
        return parkingId;
    }

    public void setParkingId(Long parkingId) {
        this.parkingId = parkingId;
    }

    public Long getSpotId() {
        return spotId;
    }

    public void setSpotId(Long spotId) {
        this.spotId = spotId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public String getParkingName() {
        return parkingName;
    }

    public void setParkingName(String parkingName) {
        this.parkingName = parkingName;
    }
}

