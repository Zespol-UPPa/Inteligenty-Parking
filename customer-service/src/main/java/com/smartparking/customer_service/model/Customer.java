package com.smartparking.customer_service.model;

public class Customer {
    private Long id;
    private String firstName;
    private String lastName;
    private Long refAccountId;

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getFirstName() {return firstName;}
    public void setFirstName(String firstName) {this.firstName = firstName;}
    public String getLastName() {return lastName;}
    public void setLastName(String lastName) {this.lastName = lastName;}
    public Long getRefAccountId() {return refAccountId;}
    public void setRefAccountId(Long refAccountId) {this.refAccountId = refAccountId;}
}
