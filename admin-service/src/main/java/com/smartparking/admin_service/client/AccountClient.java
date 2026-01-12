package com.smartparking.admin_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Component
public class AccountClient {
    private static final Logger log = LoggerFactory.getLogger(AccountClient.class);
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AccountClient(RestTemplate restTemplate,
                         @Value("${ACCOUNTS_SERVICE_URL:http://accounts-service:8087}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Get email by account ID from accounts-service
     */
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

    /**
     * Change password via accounts-service
     * @return true if successful, false if current password is incorrect
     * @throws RuntimeException if other error occurs
     */
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
            throw new RuntimeException("Failed to change password: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to change password: " + e.getMessage(), e);
        }
    }

    /**
     * Check if account is active
     */
    public boolean isAccountActive(Long accountId) {
        try {
            String url = baseUrl + "/accounts/{accountId}/status";
            ResponseEntity<Map<String, Boolean>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Boolean>>() {},
                    Map.of("accountId", accountId)
            );

            if (response.getBody() != null && response.getBody().containsKey("isActive")) {
                return response.getBody().get("isActive");
            }
            return true; // Default to active if status unknown

        } catch (Exception e) {
            log.error("Failed to get account status for accountId: {}", accountId, e);
            return true; // Default to active on error
        }
    }

    /**
     * Deactivate account
     */
    public boolean deactivateAccount(Long accountId) {
        try {
            String url = baseUrl + "/accounts/{accountId}/deactivate";
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    null,
                    Void.class,
                    Map.of("accountId", accountId)
            );
            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("Failed to deactivate account: {}", accountId, e);
            return false;
        }
    }

    /**
     * Activate account
     */
    public boolean activateAccount(Long accountId) {
        try {
            String url = baseUrl + "/accounts/{accountId}/activate";
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    null,
                    Void.class,
                    Map.of("accountId", accountId)
            );
            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("Failed to activate account: {}", accountId, e);
            return false;
        }
    }
}