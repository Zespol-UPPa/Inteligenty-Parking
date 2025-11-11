package com.smartparking.core.dto;


import com.smartparking.core.interfaces.Identifiable;

import java.util.Objects;

/**
 * DTO reprezentujące użytkownika systemu.
 * Niemutowalne, zgodne z zasadami OOP.
 */
@Deprecated
public final class UserDto implements Identifiable {

    private final Long id;
    private final String email;
    private final String fullName;
    private final String phoneNumber;
    private final String role;
    private final boolean active;

    public UserDto(Long id, String email, String fullName, String phoneNumber, String role, boolean active) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.active = active;
    }

    @Override
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserDto)) return false;
        UserDto that = (UserDto) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserDto{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", active=" + active +
                '}';
    }
}
