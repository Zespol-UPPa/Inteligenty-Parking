package com.smartparking.ocr_service.dto;

public class OcrEventDto {
    private String plate;
    private String imageUrl;
    private String timestamp; // ISO-8601 string or epoch
    private String direction; // "entry" or "exit" - optional, can be determined from history
    private Long parkingId; // ID parkingu - optional, can be determined from camera
    private Integer cameraId; // ID kamery

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public Long getParkingId() { return parkingId; }
    public void setParkingId(Long parkingId) { this.parkingId = parkingId; }
    public Integer getCameraId() { return cameraId; }
    public void setCameraId(Integer cameraId) { this.cameraId = cameraId; }
}

