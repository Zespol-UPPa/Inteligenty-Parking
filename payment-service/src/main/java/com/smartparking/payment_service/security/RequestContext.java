package com.smartparking.payment_service.security;

public class RequestContext {
    private final String username;
    private final String role;

    public RequestContext(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }
}

