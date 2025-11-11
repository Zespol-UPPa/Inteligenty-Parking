package com.smartparking.security;

import jakarta.servlet.http.HttpServletRequest;

public class RequestContextResolver {
    public RequestContext resolve(HttpServletRequest request) {
        String subject = request.getHeader("X-User-Subject");
        String role = request.getHeader("X-User-Role");
        return new RequestContext(subject, role);
    }
}
