package com.smartparking.api_gateway.service;

import com.smartparking.api_gateway.security.JwtData;
import com.smartparking.api_gateway.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class GatewayService {
    private static final Logger log = LoggerFactory.getLogger(GatewayService.class);

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
        try {
            // If request is health/actuator, allow even without jwt
            if (incomingPath != null && (incomingPath.endsWith("/health") || incomingPath.startsWith("/actuator"))) {
                String base = routeResolver.resolveBaseUrl(Role.USER, incomingPath);
                // If base resolved, forward as anonymous
                if (base == null) {
                    log.warn("No route resolved for health path={}", incomingPath);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
                String target = base + incomingPath + (query == null || query.isBlank() ? "" : "?" + query);
                log.info("Forwarding anonymous health request incomingPath={} -> target={}", incomingPath, target);
                return downstreamClient.exchange(method, target, body, headers);
            }

            // Allow anonymous OCR event publish (endpoint: /api/ocr/event or /ocr/event)
            if (incomingPath != null && (incomingPath.equalsIgnoreCase("/api/ocr/event") || incomingPath.equalsIgnoreCase("/ocr/event"))) {
                String base = routeResolver.resolveBaseUrl(Role.USER, incomingPath);
                if (base == null) {
                    log.warn("No route resolved for ocr event path={}", incomingPath);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
                String target = base + incomingPath + (query == null || query.isBlank() ? "" : "?" + query);
                log.info("Forwarding anonymous OCR event incomingPath={} -> target={}", incomingPath, target);
                return downstreamClient.exchange(method, target, body, headers);
            }

            // Allow anonymous auth endpoints (registration, login, verification)
            if (incomingPath != null && (incomingPath.startsWith("/api/auth/") || incomingPath.startsWith("/auth/"))) {
                String base = routeResolver.resolveBaseUrl(Role.USER, incomingPath);
                if (base == null) {
                    log.warn("No route resolved for auth path={}", incomingPath);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
                String target = base + incomingPath + (query == null || query.isBlank() ? "" : "?" + query);
                log.info("Forwarding anonymous auth request incomingPath={} -> target={}", incomingPath, target);
                return downstreamClient.exchange(method, target, body, headers);
            }

            // Allow anonymous parking endpoints (locations, spots, details, occupancy, pricing)
            if (incomingPath != null && (
                    incomingPath.equals("/parking/locations") || 
                    incomingPath.equals("/parking/spots") ||
                    incomingPath.startsWith("/parking/spots/for-reservation") ||
                    (incomingPath.startsWith("/parking/locations/") && (incomingPath.endsWith("/details") || incomingPath.endsWith("/occupancy"))) ||
                    (incomingPath.startsWith("/parking/pricing/") && incomingPath.endsWith("/reservation-fee"))
            )) {
                String base = routeResolver.resolveBaseUrl(Role.USER, incomingPath);
                if (base == null) {
                    log.warn("No route resolved for parking path={}", incomingPath);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
                String target = base + incomingPath + (query == null || query.isBlank() ? "" : "?" + query);
                log.info("Forwarding anonymous parking request incomingPath={} -> target={}", incomingPath, target);
                return downstreamClient.exchange(method, target, body, headers);
            }

            // Payment charge requires authentication - removed anonymous access for security

            if (jwt == null) {
                // Not a health request and no jwt provided -> 401
                log.warn("Unauthorized request for path={} (no JWT)", incomingPath);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Role role;
            try {
                role = Role.valueOf(jwt.getRole());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role in JWT: {}", jwt.getRole());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if(incomingPath != null && incomingPath.startsWith("/admin/") && role != Role.ADMIN){
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if(incomingPath != null && incomingPath.startsWith("/worker/") && !(role == Role.WORKER || role == Role.ADMIN)){
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            String base = routeResolver.resolveBaseUrl(role, incomingPath);
            if(base == null) {
                log.warn("No route resolved for path={}", incomingPath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            String target = base + incomingPath + (query == null || query.isBlank() ? "" : "?" + query);
            log.info("Forwarding request: role={} incomingPath={} -> target={}", jwt.getRole(), incomingPath, target);
            ResponseEntity<byte[]> resp = downstreamClient.exchange(method, target, body, headers);
            log.info("Downstream responded: target={} status={}", target, resp != null ? resp.getStatusCode() : "null");
            return resp;
        } catch (Exception e) {
            log.error("Error forwarding request for path={}: {}", incomingPath, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(("Bad Gateway: " + e.getMessage()).getBytes());
        }
    }
}
