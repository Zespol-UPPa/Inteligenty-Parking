package com.smartparking.core.dto;

import com.smartparking.core.enums.StatusReservation;
import com.smartparking.core.interfaces.Identifiable;
import java.time.Instant;

public final class ReservationDto implements Identifiable {
    private final Long id;
    private final Instant startTime;
    private final Instant endTime;
    private final StatusReservation type;
    private final Long spotId;
    private final Long accountId;
    private final Long parkingId;

    public ReservationDto(Long id, Instant startTime, Instant endTime, StatusReservation type, Long spotId, Long accountId, Long parkingId) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
        this.spotId = spotId;
        this.accountId = accountId;
        this.parkingId = parkingId;
    }

    @Override
    public Long getId() { return id; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public StatusReservation getType() { return type; }
    public Long getSpotId() { return spotId; }
    public Long getAccountId() { return accountId; }
    public Long getParkingId() { return parkingId; }
}
