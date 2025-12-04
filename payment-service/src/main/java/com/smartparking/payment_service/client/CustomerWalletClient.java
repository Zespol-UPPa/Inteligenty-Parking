package com.smartparking.payment_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Component
public class CustomerWalletClient {
    private final RestTemplate rest;
    private final String baseUrl;
    private final String internalToken;

    public CustomerWalletClient(RestTemplate rest,
                                @Value("${CUSTOMER_SERVICE_URL:http://localhost:8081}") String baseUrl,
                                @Value("${INTERNAL_SERVICE_TOKEN:}") String internalToken) {
        this.rest = rest;
        this.baseUrl = baseUrl;
        this.internalToken = internalToken;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (internalToken != null && !internalToken.isBlank()) {
            headers.set("X-Internal-Token", internalToken);
        }
        return headers;
    }

    public Optional<Map<String, Object>> getWallet(Long accountId) {
        try {
            HttpEntity<Void> req = new HttpEntity<>(authHeaders());
            ResponseEntity<Map<String, Object>> res = rest.exchange(
                    baseUrl + "/internal/wallet/" + accountId,
                    HttpMethod.GET,
                    req,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return Optional.ofNullable(res.getBody());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public boolean updateBalance(Long accountId, BigDecimal newBalance) {
        try {
            Map<String, Object> body = Map.of("balance_minor", newBalance.toString());
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, authHeaders());
            ResponseEntity<Void> res = rest.exchange(
                    baseUrl + "/internal/wallet/" + accountId + "/debit",
                    HttpMethod.POST,
                    req,
                    Void.class
            );
            return res.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
