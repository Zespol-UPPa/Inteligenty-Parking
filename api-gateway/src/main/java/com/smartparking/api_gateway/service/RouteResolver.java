package com.smartparking.api_gateway.service;
import com.smartparking.security.Role;

public interface RouteResolver {
    String resolveBaseUrl(Role role, String incomingPath);
}
