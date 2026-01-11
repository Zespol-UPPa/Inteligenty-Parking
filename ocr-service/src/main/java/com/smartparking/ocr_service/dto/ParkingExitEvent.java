package com.smartparking.ocr_service.dto;

import java.time.Instant;

public class ParkingExitEvent {
    private String licencePlate;
    private Long parkingId;
    private Integer cameraId;
    private Instant timestamp;

    public ParkingExitEvent() {
    }

    public ParkingExitEvent(String licencePlate, Long parkingId, Integer cameraId, Instant timestamp) {
        this.licencePlate = licencePlate;
        this.parkingId = parkingId;
        this.cameraId = cameraId;
        this.timestamp = timestamp;
    }

    public String getLicencePlate() {
        return licencePlate;
    }

    public void setLicencePlate(String licencePlate) {
        this.licencePlate = licencePlate;
    }

    public Long getParkingId() {
        return parkingId;
    }

    public void setParkingId(Long parkingId) {
        this.parkingId = parkingId;
    }

    public Integer getCameraId() {
        return cameraId;
    }

    public void setCameraId(Integer cameraId) {
        this.cameraId = cameraId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}







