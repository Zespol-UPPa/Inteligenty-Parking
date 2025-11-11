package com.smartparking.core.dto;

import com.smartparking.core.interfaces.Identifiable;

public final class AccountDto implements Identifiable {
    private final Long id;
    private final String email;
    private final String passwordHash;

    public AccountDto(Long id, String email, String passwordHash) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    @Override
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
}
