package com.smartparking.parking_service.model;

public class ParkingPricing {
    private Long id;
    private String currencyCode;
    private Integer ratePerMin;
    private Integer freeMinutes;
    private Integer roundingStepMin;
    private Integer reservationFeeMinor;
    private Long parkingId;

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getCurrencyCode() {return currencyCode;}
    public void setCurrencyCode(String currencyCode) {this.currencyCode = currencyCode;}
    public Integer getRatePerMin() {return ratePerMin;}
    public void setRatePerMin(Integer ratePerMin) {this.ratePerMin = ratePerMin;}
    public Integer getFreeMinutes() {return freeMinutes;}
    public void setFreeMinutes(Integer freeMinutes) {this.freeMinutes = freeMinutes;}
    public Integer getRoundingStepMin() {return roundingStepMin;}
    public void setRoundingStepMin(Integer roundingStepMin) {this.roundingStepMin = roundingStepMin;}
    public Integer getReservationFeeMinor() {return reservationFeeMinor;}
    public void setReservationFeeMinor(Integer reservationFeeMinor) {this.reservationFeeMinor = reservationFeeMinor;}
    public Long getParkingId() {return parkingId;}
    public void setParkingId(Long parkingId) {this.parkingId = parkingId;}
}
