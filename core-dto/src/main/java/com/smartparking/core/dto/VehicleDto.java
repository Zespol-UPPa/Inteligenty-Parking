package com.smartparking.core.dto;

import com.smartparking.core.interfaces.Identifiable;

public final class VehicleDto implements Identifiable {
    private final Long id;
    private final String licencePlate;
    private final Long accountId;

    public VehicleDto(Long id, String licencePlate, Long accountId) {
        this.id = id;
        this.licencePlate = licencePlate;
        this.accountId = accountId;
    }

    @Override
    public Long getId() { return id; }
    public String getLicencePlate() { return licencePlate; }
    public Long getAccountId() { return accountId; }
}
