package com.smartparking.admin_service.model;

public class Admin {
    private Long id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String peselNumber;
    private Long refCompanyId;
    private Long refAccountId;

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getFirstName() {return firstName;}
    public void setFirstName(String firstName) {this.firstName = firstName;}
    public String getLastName() {return lastName;}
    public void setLastName(String lastName) {this.lastName = lastName;}
    public String getPhoneNumber() {return phoneNumber;}
    public void setPhoneNumber(String phoneNumber) {this.phoneNumber = phoneNumber;}
    public String getPeselNumber() {return peselNumber;}
    public void setPeselNumber(String peselNumber) {this.peselNumber = peselNumber;}
    public Long getRefCompanyId() {return refCompanyId;}
    public void setRefCompanyId(Long refCompanyId) {this.refCompanyId = refCompanyId;}
    public Long getRefAccountId() {return refAccountId;}
    public void setRefAccountId(Long refAccountId) {this.refAccountId = refAccountId;}
}
