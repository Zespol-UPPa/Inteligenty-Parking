package com.smartparking.parking_service.dto;

public class ParkingUsageDto {
    private long idParking;
    private String type;
    private long count;

    public ParkingUsageDto() { }

    public ParkingUsageDto(long idParking, String type, long count) {
        this.idParking = idParking;
        this.type = type;
        this.count = count;
    }

    public long getIdParking() {
        return idParking;
    }

    public void setIdParking(long idParking) {
        this.idParking = idParking;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}