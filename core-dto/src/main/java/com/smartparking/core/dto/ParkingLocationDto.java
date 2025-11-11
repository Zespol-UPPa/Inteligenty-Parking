package com.smartparking.core.dto;

import com.smartparking.core.interfaces.Identifiable;

public final class ParkingLocationDto implements Identifiable {
    private final Long id;
    private final String nameParking;
    private final String addressLine;
    private final Long companyId;

    public ParkingLocationDto(Long id, String nameParking, String addressLine, Long companyId) {
        this.id = id;
        this.nameParking = nameParking;
        this.addressLine = addressLine;
        this.companyId = companyId;
    }

    @Override
    public Long getId() { return id; }
    public String getNameParking() { return nameParking; }
    public String getAddressLine() { return addressLine; }
    public Long getCompanyId() { return companyId; }
}
