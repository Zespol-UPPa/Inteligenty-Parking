package com.smartparking.api_gateway.controller;

import com.smartparking.api_gateway.security.JwtData;
import com.smartparking.api_gateway.filter.JwtAuthFilter;
import com.smartparking.api_gateway.service.GatewayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Proxy controller for /auth/** endpoints.
 * All authentication logic is handled by accounts-service.
 * This controller forwards requests to accounts-service to maintain backward compatibility.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final GatewayService gatewayService;
    
    public AuthController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }
    
    @RequestMapping(value = "/**",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<byte[]> proxyAuth(HttpMethod method,
                                             HttpServletRequest request,
                                             @RequestHeader HttpHeaders headers) throws IOException {
        JwtData jwtData = (JwtData) request.getAttribute(JwtAuthFilter.ATTR_JWT);
        String path = request.getRequestURI();
        String query = request.getQueryString();
        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        HttpHeaders sanitized = new HttpHeaders(headers);
        sanitized.remove(HttpHeaders.HOST);
        sanitized.remove(HttpHeaders.CONTENT_LENGTH);
        // Forward to accounts-service via /api/auth/** routing
        // Convert /auth/login to /api/auth/login
        if (path.startsWith("/auth/")) {
            path = "/api" + path;
        }
        return gatewayService.forward(jwtData, method, path, query, body, sanitized);
    }
}
