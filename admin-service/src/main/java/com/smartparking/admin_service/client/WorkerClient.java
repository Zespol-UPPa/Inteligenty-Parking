package com.smartparking.admin_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client for communicating with worker-service
 */
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
     * Get all workers from specified company
     * Calls: GET /workers/internal/by-company/{companyId}
     */
    public List<Map<String, Object>> getWorkersByCompany(Long companyId) {
        try {
            String url = baseUrl + "/workers/internal/by-company/" + companyId;

            log.info("Fetching workers from worker-service for company: {}", companyId);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> workers = response.getBody();

            if (workers != null) {
                log.info("Successfully fetched {} workers from worker-service", workers.size());
                return workers;
            } else {
                log.warn("No workers returned from worker-service");
                return new ArrayList<>();
            }

        } catch (Exception e) {
            log.error("Failed to fetch workers from worker-service for company {}: {}",
                    companyId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Verify if worker belongs to specified company
     * Calls: GET /workers/internal/verify-company?accountId={accountId}&companyId={companyId}
     */
    public boolean verifyWorkerCompany(Long accountId, Long companyId) {
        try {
            String url = baseUrl + "/workers/internal/verify-company?accountId=" + accountId + "&companyId=" + companyId;

            log.info("Verifying worker {} belongs to company {}", accountId, companyId);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    Boolean.class
            );

            Boolean belongs = response.getBody();
            log.info("Worker {} belongs to company {}: {}", accountId, companyId, belongs);

            return belongs != null && belongs;

        } catch (Exception e) {
            log.error("Failed to verify worker company: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get worker details by account ID
     * Calls: GET /workers/internal/by-account/{accountId}
     */
    public Map<String, Object> getWorkerByAccountId(Long accountId) {
        try {
            String url = baseUrl + "/workers/internal/by-account/" + accountId;

            log.info("Fetching worker details for accountId: {}", accountId);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("Failed to fetch worker details for accountId {}: {}",
                    accountId, e.getMessage());
            return null;
        }
    }
}