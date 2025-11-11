package com.smartparking.security;

import jakarta.servlet.http.HttpServletRequest;

public class RequestContextMapper {
    public static RequestContext fromJwt(JwtData jwtData) {
        return new RequestContext(jwtData.getSubject(), jwtData.getRole().name());
    }
}
