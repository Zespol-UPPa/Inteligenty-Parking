package com.smartparking.ocr_service.web.dto;

public class OcrEventDto {
    private String plate;
    private String timestamp; // ISO-8601

    public OcrEventDto() {}

    public OcrEventDto(String plate, String timestamp) {
        this.plate = plate;
        this.timestamp = timestamp;
    }

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}

