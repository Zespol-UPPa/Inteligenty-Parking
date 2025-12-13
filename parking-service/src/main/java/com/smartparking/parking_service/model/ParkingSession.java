package com.smartparking.parking_service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ParkingSession {

    private Long id;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private BigDecimal priceTotalMinor;
    private String paymentStatus;
    private Long parkingId;
    private Long spotId;
    private Long refVehicleId;
    private Long refAccountId;

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public LocalDateTime getEntryTime() {return entryTime;}
    public void setEntryTime(LocalDateTime entryTime) {this.entryTime = entryTime;}
    public LocalDateTime getExitTime() {return exitTime;}
    public void setExitTime(LocalDateTime exitTime) {this.exitTime = exitTime;}
    public BigDecimal getPriceTotalMinor() {return priceTotalMinor;}
    public void setPriceTotalMinor(BigDecimal priceTotalMinor) {this.priceTotalMinor = priceTotalMinor;}
    public String getPaymentStatus() {return paymentStatus;}
    public void setPaymentStatus(String paymentStatus) {this.paymentStatus = paymentStatus;}
    public Long getParkingId() {return parkingId;}
    public void setParkingId(Long parkingId) {this.parkingId = parkingId;}
    public Long getSpotId() {return spotId;}
    public void setSpotId(Long spotId) {this.spotId = spotId;}
    public Long getRefVehicleId() {return refVehicleId;}
    public void setRefVehicleId(Long refVehicleId) {this.refVehicleId = refVehicleId;}
    public Long getRefAccountId() {return refAccountId;}
    public void setRefAccountId(Long refAccountId) {this.refAccountId = refAccountId;}
}
