package com.smartparking.accounts_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class AdminClient {
    private static final Logger log = LoggerFactory.getLogger(AdminClient.class);
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AdminClient(RestTemplate restTemplate,
                       @Value("${ADMIN_SERVICE_URL:http://admin-service:8084}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Get admin data by account ID
     */
    public Map<String, Object> getAdminByAccountId(Long accountId) {
        try {
            String url = baseUrl + "/admins/internal/by-account/" + accountId;

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Retrieved admin data for accountId={}", accountId);
                return response.getBody();
            }

            throw new RuntimeException("Failed to retrieve admin data");

        } catch (Exception e) {
            log.error("Failed to get admin data for accountId={}", accountId, e);
            throw new RuntimeException("Failed to retrieve admin data", e);
        }
    }

    /**
     * Update admin personal data
     */
    public boolean updateAdminPersonalData(Long accountId, String firstName, String lastName,
                                           String phoneNumber, String peselNumber) {
        try {
            String url = baseUrl + "/admins/internal/update-personal-data";

            Map<String, Object> updateData = Map.of(
                    "accountId", accountId,
                    "firstName", firstName != null ? firstName : "",
                    "lastName", lastName != null ? lastName : "",
                    "phoneNumber", phoneNumber != null ? phoneNumber : "",
                    "peselNumber", peselNumber != null ? peselNumber : ""
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(updateData);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Updated admin personal data for accountId={}", accountId);
                return true;
            }

            log.error("Failed to update admin personal data for accountId={}, status={}",
                    accountId, response.getStatusCode());
            throw new RuntimeException("Failed to update admin personal data: HTTP " + response.getStatusCode());

        } catch (Exception e) {
            log.error("Failed to update admin personal data for accountId={}", accountId, e);
            // Re-throw to allow transaction rollback in AuthController
            throw new RuntimeException("Failed to update admin personal data: " + e.getMessage(), e);
        }
    }
}