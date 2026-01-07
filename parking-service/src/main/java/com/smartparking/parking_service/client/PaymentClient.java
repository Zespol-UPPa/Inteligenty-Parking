package com.smartparking.parking_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class PaymentClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PaymentClient(RestTemplate restTemplate,
                        @Value("${PAYMENT_SERVICE_URL:http://payment-service:8084}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public com.smartparking.parking_service.service.ParkingSessionService.PaymentResult chargeForParkingSession(
            Long accountId, Long sessionId, Long amountMinor) {
        try {
            String url = baseUrl + "/payment/charge";
            Map<String, Object> body = Map.of(
                "accountId", accountId,
                "sessionId", sessionId,
                "amount", new java.math.BigDecimal(amountMinor).divide(new java.math.BigDecimal(100)),
                "currency", "PLN"
            );
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Internal-Token", getInternalToken());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                String status = result.get("status") != null ? result.get("status").toString() : "Failed";
                boolean success = "SUCCESS".equals(status) || "Paid".equals(status);
                return success ? 
                    com.smartparking.parking_service.service.ParkingSessionService.PaymentResult.success(sessionId, amountMinor) :
                    com.smartparking.parking_service.service.ParkingSessionService.PaymentResult.failed("Payment failed: " + status);
            }
            return com.smartparking.parking_service.service.ParkingSessionService.PaymentResult.failed("Payment service returned error");
        } catch (Exception e) {
            return com.smartparking.parking_service.service.ParkingSessionService.PaymentResult.failed("Payment service error: " + e.getMessage());
        }
    }

    @org.springframework.beans.factory.annotation.Value("${INTERNAL_SERVICE_TOKEN:}")
    private String internalToken;

    private String getInternalToken() {
        return internalToken != null ? internalToken : "";
    }
}

