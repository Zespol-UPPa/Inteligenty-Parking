package com.smartparking.customer_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Component
public class AccountClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AccountClient(RestTemplate restTemplate,
                        @Value("${ACCOUNTS_SERVICE_URL:http://accounts-service:8087}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public Optional<String> getEmailByAccountId(Long accountId) {
        try {
            String url = baseUrl + "/accounts/{accountId}/email";
            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, String>>() {},
                    Map.of("accountId", accountId)
            );
            if (response.getBody() != null && response.getBody().containsKey("email")) {
                return Optional.of(response.getBody().get("email"));
            }
            return Optional.empty();
        } catch (Exception e) {
            // Log error but don't fail - return empty email
            // This allows profile to load even if accounts-service is unavailable
            return Optional.empty();
        }
    }
    
    public boolean changePassword(Long accountId, String currentPassword, String newPassword) {
        try {
            String url = baseUrl + "/accounts/{accountId}/password";
            Map<String, String> body = Map.of(
                "currentPassword", currentPassword,
                "newPassword", newPassword
            );
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new org.springframework.http.HttpEntity<>(body),
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {},
                    Map.of("accountId", accountId)
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) {
                return false; // Current password incorrect
            }
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to change password: " + e.getMessage(), e);
        }
    }
    
    public boolean deleteAccount(Long accountId) {
        try {
            String url = baseUrl + "/accounts/{accountId}";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {},
                    Map.of("accountId", accountId)
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete account: " + e.getMessage(), e);
        }
    }
}

