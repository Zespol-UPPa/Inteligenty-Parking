package com.smartparking.customer_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class PaymentClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalServiceToken;

    public PaymentClient(RestTemplate restTemplate,
                        @Value("${PAYMENT_SERVICE_URL:http://payment-service:8082}") String baseUrl,
                        @Value("${INTERNAL_SERVICE_TOKEN:}") String internalServiceToken) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.internalServiceToken = internalServiceToken;
    }

    public Long createDepositPayment(Long accountId, Long amountMinor, String paymentMethod) {
        try {
            String url = baseUrl + "/payment/deposit";
            Map<String, Object> body = Map.of(
                "accountId", accountId,
                "amountMinor", amountMinor,
                "paymentMethod", paymentMethod != null ? paymentMethod : "unknown"
            );
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (internalServiceToken != null && !internalServiceToken.isBlank()) {
                headers.set("X-Internal-Token", internalServiceToken);
            }
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<IdResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                IdResponse.class
            );
            if (response.getBody() != null) {
                return response.getBody().getId();
            }
            throw new IllegalStateException("Payment service did not return payment id");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create deposit payment: " + e.getMessage(), e);
        }
    }

    public PaymentResult chargeReservationFee(Long accountId, Long amountMinor) {
        try {
            String url = baseUrl + "/payment/charge/reservation";
            java.math.BigDecimal amount = new java.math.BigDecimal(amountMinor).divide(new java.math.BigDecimal(100));
            Map<String, Object> body = Map.of(
                "accountId", accountId,
                "amount", amount,
                "currency", "PLN",
                "sessionId", 0L
            );
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (internalServiceToken != null && !internalServiceToken.isBlank()) {
                headers.set("X-Internal-Token", internalServiceToken);
            }
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            if (response.getBody() != null) {
                Map<String, Object> bodyMap = response.getBody();
                Long paymentId = null;
                String status = null;
                
                Object idObj = bodyMap.get("id");
                if (idObj != null) {
                    paymentId = Long.valueOf(idObj.toString());
                }
                
                // PaymentDto has status field which is PaymentStatus enum (SUCCESS, PENDING, FAILED, CANCELLED)
                Object statusObj = bodyMap.get("status");
                if (statusObj != null) {
                    String statusStr = statusObj.toString();
                    // Map PaymentStatus enum to payment service status
                    if (statusStr.equalsIgnoreCase("SUCCESS")) {
                        status = "Paid";
                    } else if (statusStr.equalsIgnoreCase("PENDING")) {
                        status = "Pending";
                    } else if (statusStr.equalsIgnoreCase("FAILED")) {
                        status = "Failed";
                    } else if (statusStr.equalsIgnoreCase("CANCELLED")) {
                        status = "Failed";
                    } else {
                        status = statusStr; // Fallback
                    }
                }
                
                return new PaymentResult(paymentId, status);
            }
            throw new IllegalStateException("Payment service did not return payment result");
        } catch (Exception e) {
            throw new RuntimeException("Failed to charge reservation fee: " + e.getMessage(), e);
        }
    }

    public PaymentResult refundReservationFee(Long paymentId, Long accountId, Long amountMinor) {
        try {
            String url = baseUrl + "/payment/refund/reservation";
            Map<String, Object> body = Map.of(
                "paymentId", paymentId,
                "accountId", accountId,
                "amountMinor", amountMinor
            );
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (internalServiceToken != null && !internalServiceToken.isBlank()) {
                headers.set("X-Internal-Token", internalServiceToken);
            }
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            if (response.getBody() != null) {
                Map<String, Object> bodyMap = response.getBody();
                Long returnedPaymentId = null;
                String status = null;
                
                Object idObj = bodyMap.get("id");
                if (idObj != null) {
                    returnedPaymentId = Long.valueOf(idObj.toString());
                }
                
                Object statusObj = bodyMap.get("status");
                if (statusObj != null) {
                    String statusStr = statusObj.toString();
                    if (statusStr.equalsIgnoreCase("CANCELLED") || statusStr.equalsIgnoreCase("REFUNDED")) {
                        status = "Refunded";
                    } else {
                        status = statusStr;
                    }
                }
                
                return new PaymentResult(returnedPaymentId != null ? returnedPaymentId : paymentId, status);
            }
            throw new IllegalStateException("Payment service did not return refund result");
        } catch (Exception e) {
            throw new RuntimeException("Failed to refund reservation fee: " + e.getMessage(), e);
        }
    }

    public static class PaymentResult {
        private Long paymentId;
        private String status;

        public PaymentResult(Long paymentId, String status) {
            this.paymentId = paymentId;
            this.status = status;
        }

        public Long getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(Long paymentId) {
            this.paymentId = paymentId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public boolean isPaid() {
            return "Paid".equals(status) || "PAID".equals(status);
        }
    }

    public static class IdResponse {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    /**
     * Pobiera historię transakcji dla danego konta użytkownika
     * Uwaga: endpoint w payment-service wymaga JWT token (nie internal token)
     */
    public List<Map<String, Object>> getTransactions(Long accountId, String jwtToken) {
        try {
            String url = baseUrl + "/payment/transactions";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get transactions: " + e.getMessage(), e);
        }
    }

    /**
     * Pobiera statystyki dla danego konta użytkownika
     * Uwaga: endpoint w payment-service wymaga JWT token (nie internal token)
     */
    public Map<String, Object> getStatistics(Long accountId, String jwtToken) {
        try {
            String url = baseUrl + "/payment/statistics";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody() != null ? response.getBody() : Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get statistics: " + e.getMessage(), e);
        }
    }
}

