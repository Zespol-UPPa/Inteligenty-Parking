package com.smartparking.company_service.model;

public class Company {
    private Integer companyId;
    private String nameCompany;
    private String address;
    private String taxId;

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }
    public String getNameCompany() { return nameCompany; }
    public void setNameCompany(String nameCompany) { this.nameCompany = nameCompany; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
}

