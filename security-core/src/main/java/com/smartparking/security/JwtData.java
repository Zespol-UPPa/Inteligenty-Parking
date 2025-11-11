package com.smartparking.security;
import java.time.Instant;
public class JwtData {
    private final String subject;
    private final Role role;
    public final Instant expiration;

    public JwtData(String subject, Role role, Instant expiration) {
        this.subject = subject;
        this.role = role;
        this.expiration = expiration;
    }
    public String getSubject() {
        return subject;
    }
    public Role getRole() {
        return role;
    }
    public Instant getExpiration() {
        return expiration;
    }
    @Override
    public String toString() {
        return "JwtData{" +
                "subject='" + subject + '\'' +
                ", role=" + role +
                ", expiration=" + expiration +
                '}';
    }
};
