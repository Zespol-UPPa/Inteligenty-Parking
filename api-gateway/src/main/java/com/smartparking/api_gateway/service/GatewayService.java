package com.smartparking.api_gateway.service;

import com.smartparking.security.JwtData;
import com.smartparking.security.Role;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class GatewayService {
    private final RouteResolver routeResolver;
    private final DownstreamClient downstreamClient;
    public GatewayService(RouteResolver routeResolver, DownstreamClient downstreamClient) {
        this.routeResolver = routeResolver;
        this.downstreamClient = downstreamClient;
    }
    public ResponseEntity<byte[]> forward(
            JwtData jwt,
            HttpMethod method,
            String incomingPath,
            String query,
            byte[] body,
            HttpHeaders headers
    ) {
        Role role = jwt.getRole();
        if(incomingPath.startsWith("/admin/") && role != Role.ADMIN){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if(incomingPath.startsWith("/worker/") && !(role == Role.WORKER || role == Role.ADMIN)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String base = routeResolver.resolveBaseUrl(role, incomingPath);
        if(base == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        String target = base + incomingPath + (query == null || query.isBlank() ? "" : "?" + query);
        return downstreamClient.exchange(method, target, body, headers);
    }
}
