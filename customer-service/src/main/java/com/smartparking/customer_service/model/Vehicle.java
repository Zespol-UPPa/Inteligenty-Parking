package com.smartparking.customer_service.model;

public class Vehicle {
    private Long id;
    private String licencePlate;
    private Long customerId;

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getLicencePlate() {return licencePlate;}
    public void setLicencePlate(String licencePlate) {this.licencePlate = licencePlate;}
    public Long getCustomerId() {return customerId;}
    public void setCustomerId(Long customerId) {this.customerId = customerId;}
}
