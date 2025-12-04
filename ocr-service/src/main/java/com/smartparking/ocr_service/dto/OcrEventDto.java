package com.smartparking.ocr_service.dto;

public class OcrEventDto {
    private String plate;
    private String imageUrl;
    private String timestamp; // ISO-8601 string or epoch

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}

