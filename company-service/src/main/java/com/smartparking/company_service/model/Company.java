package com.smartparking.company_service.model;

public class Company {
    private Long Id;
    private String nameCompany;
    private String address;
    private String taxId;

    public Long getId() { return Id; }
    public void setId(Long Id) { this.Id = Id; }
    public String getNameCompany() { return nameCompany; }
    public void setNameCompany(String nameCompany) { this.nameCompany = nameCompany; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
}

