package com.smartparking.parking_service.model;

import java.time.LocalDateTime;

public class ParkingSpot {
    private Long id;
    private String code;
    private Integer floorLvl;
    private boolean toReserved;
    private String type;
    private Long parkingId;

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getCode() {return code;}
    public void setCode(String code) {this.code = code;}
    public Integer getFloorLvl() {return floorLvl;}
    public void setFloorLvl(Integer floorLvl) {this.floorLvl = floorLvl;}
    public boolean isToReserved() {return toReserved;}
    public void setToReserved(boolean toReserved) {this.toReserved = toReserved;}
    public String getType() {return type;}
    public void setType(String type) {this.type = type;}
    public Long getParkingId() {return parkingId;}
    public void setParkingId(Long parkingId) {this.parkingId = parkingId;}
}
