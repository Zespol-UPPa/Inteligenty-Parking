package com.smartparking.core.dto;

import com.smartparking.core.enums.Status;
import com.smartparking.core.interfaces.Identifiable;

public final class ParkingSpotDto implements Identifiable {
    private final Long id;
    private final String code;
    private final int floorLvl;
    private final boolean toReserved;
    private final Status type;
    private final Long parkingId;

    public ParkingSpotDto(Long id, String code, int floorLvl, boolean toReserved, Status type, Long parkingId) {
        this.id = id;
        this.code = code;
        this.floorLvl = floorLvl;
        this.toReserved = toReserved;
        this.type = type;
        this.parkingId = parkingId;
    }

    @Override
    public Long getId() { return id; }
    public String getCode() { return code; }
    public int getFloorLvl() { return floorLvl; }
    public boolean isToReserved() { return toReserved; }
    public Status getType() { return type; }
    public Long getParkingId() { return parkingId; }
}
