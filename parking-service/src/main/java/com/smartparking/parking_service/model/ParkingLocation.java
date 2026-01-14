package com.smartparking.parking_service.model;

public class ParkingLocation {
    private Long id;
    private String nameParking;
    private String addressLine;
    private Long refCompanyId;

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getNameParking() {return nameParking;}
    public void setNameParking(String nameParking) {this.nameParking = nameParking;}
    public String getAddressLine() {return addressLine;}
    public void setAddressLine(String addressLine) {this.addressLine = addressLine;}
    public Long getRefCompanyId() {return refCompanyId;}
    public void setRefCompanyId(Long refCompanyId) {this.refCompanyId = refCompanyId;}
}

