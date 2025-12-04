package com.smartparking.api_gateway.controller;

import com.smartparking.api_gateway.security.JwtData;
import com.smartparking.api_gateway.filter.JwtAuthFilter;
import com.smartparking.api_gateway.service.GatewayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class ProxyController {
    private final GatewayService gatewayService;
    public ProxyController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }
    @RequestMapping(value = "/customer/**",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<byte[]> proxyCustomer(HttpMethod method,
                                        HttpServletRequest request,
                                        @RequestHeader HttpHeaders headers) throws IOException{
        JwtData jwtData = (JwtData) request.getAttribute(JwtAuthFilter.ATTR_JWT);
        String path = request.getRequestURI();
        String query = request.getQueryString();
        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        HttpHeaders sanitized = new HttpHeaders(headers);
        sanitized.remove(HttpHeaders.HOST);
        sanitized.remove(HttpHeaders.CONTENT_LENGTH);
        return gatewayService.forward(jwtData, method, path, query, body, sanitized);

    }

    @RequestMapping(value = {"/payment/**", "/parking/**", "/admin/**", "/worker/**", "/accounts/**", "/company/**", "/ocr/**"},
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<byte[]> proxyOther(HttpMethod method,
                                             HttpServletRequest request,
                                             @RequestHeader HttpHeaders headers) throws IOException {
        JwtData jwtData = (JwtData) request.getAttribute(JwtAuthFilter.ATTR_JWT);
        String path = request.getRequestURI();
        String query = request.getQueryString();
        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        HttpHeaders sanitized = new HttpHeaders(headers);
        sanitized.remove(HttpHeaders.HOST);
        sanitized.remove(HttpHeaders.CONTENT_LENGTH);
        return gatewayService.forward(jwtData, method, path, query, body, sanitized);
    }
}
