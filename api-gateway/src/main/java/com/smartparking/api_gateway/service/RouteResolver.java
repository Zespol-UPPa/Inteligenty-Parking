package com.smartparking.api_gateway.service;
import com.smartparking.api_gateway.security.Role;

public interface RouteResolver {
    String resolveBaseUrl(Role role, String incomingPath);
}
