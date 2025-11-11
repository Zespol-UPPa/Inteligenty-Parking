package com.smartparking.core.dto;

import com.smartparking.core.interfaces.Identifiable;

public final class CustomerDto implements Identifiable {
    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String phoneNumber;
    private final Long accountId;

    public CustomerDto(Long id, String firstName, String lastName, String phoneNumber, Long accountId) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.accountId = accountId;
    }

    @Override
    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public Long getAccountId() { return accountId; }
}
