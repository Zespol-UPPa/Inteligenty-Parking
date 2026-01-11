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
public class WorkerClient {
    private static final Logger log = LoggerFactory.getLogger(WorkerClient.class);
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public WorkerClient(RestTemplate restTemplate,
                        @Value("${WORKER_SERVICE_URL:http://worker-service:8085}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Get worker data by account ID
     */
    public Map<String, Object> getWorkerByAccountId(Long accountId) {
        try {
            String url = baseUrl + "/workers/internal/by-account/" + accountId;

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Retrieved worker data for accountId={}", accountId);
                return response.getBody();
            }

            throw new RuntimeException("Failed to retrieve worker data");

        } catch (Exception e) {
            log.error("Failed to get worker data for accountId={}", accountId, e);
            throw new RuntimeException("Failed to retrieve worker data", e);
        }
    }

    /**
     * Update worker personal data
     */
    public boolean updateWorkerPersonalData(Long accountId, String firstName, String lastName,
                                            String phoneNumber, String peselNumber) {
        try {
            String url = baseUrl + "/workers/internal/update-personal-data";

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
                log.info("Updated worker personal data for accountId={}", accountId);
                return true;
            }

            log.error("Failed to update worker personal data for accountId={}, status={}",
                    accountId, response.getStatusCode());
            throw new RuntimeException("Failed to update worker personal data: HTTP " + response.getStatusCode());

        } catch (Exception e) {
            log.error("Failed to update worker personal data for accountId={}", accountId, e);
            // Re-throw to allow transaction rollback in AuthController
            throw new RuntimeException("Failed to update worker personal data: " + e.getMessage(), e);
        }
    }
}