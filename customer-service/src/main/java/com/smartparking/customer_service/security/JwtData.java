package com.smartparking.customer_service.security;

public class JwtData {
    private final String subject;
    private final String role;

    public JwtData(String subject, String role) {
        this.subject = subject;
        this.role = role;
    }

    public String getSubject() { return subject; }
    public String getRole() { return role; }
}

