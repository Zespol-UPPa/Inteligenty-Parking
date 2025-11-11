package com.smartparking.security;

public class RequestContext {
    private final String subject;
    private final String role;
    public RequestContext(String subject, String role) {
        this.subject = subject;
        this.role = role;
    }
    public String getSubject() {
        return subject;
    }
    public String getRole() {
        return role;
    }
}
