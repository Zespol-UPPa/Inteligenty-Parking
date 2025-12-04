package com.smartparking.ocr_service.model;

import java.time.LocalDateTime;

public class PlateRead {
    private Integer readId;
    private Integer cameraId;
    private String rawPlate;
    private LocalDateTime eventTime;

    public Integer getReadId() { return readId; }
    public void setReadId(Integer readId) { this.readId = readId; }
    public Integer getCameraId() { return cameraId; }
    public void setCameraId(Integer cameraId) { this.cameraId = cameraId; }
    public String getRawPlate() { return rawPlate; }
    public void setRawPlate(String rawPlate) { this.rawPlate = rawPlate; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
}

