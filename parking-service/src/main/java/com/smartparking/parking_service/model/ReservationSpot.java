package com.smartparking.parking_service.model;

import java.time.LocalDateTime;

public class ReservationSpot {

    private Long id;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private String statusReservation;
    private Long spotId;
    private Long parkingId;
    private Long refAccountId;

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public LocalDateTime getValidFrom() {return validFrom;}
    public void setValidFrom(LocalDateTime validFrom) {this.validFrom = validFrom;}
    public LocalDateTime getValidUntil() {return validUntil;}
    public void setValidUntil(LocalDateTime validUntil) {this.validUntil = validUntil;}
    public String getStatusReservation() {return statusReservation;}
    public void setStatusReservation(String statusReservation) {this.statusReservation = statusReservation;}
    public Long getSpotId() {return spotId;}
    public void setSpotId(Long spotId) {this.spotId = spotId;}
    public Long getParkingId() {return parkingId;}
    public void setParkingId(Long parkingId) {this.parkingId = parkingId;}
    public Long getRefAccountId() {return refAccountId;}
    public void setRefAccountId(Long refAccountId) {this.refAccountId = refAccountId;}
}
