package com.smartparking.worker_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class CompanyClient {
    private static final Logger log = LoggerFactory.getLogger(CompanyClient.class);
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CompanyClient(RestTemplate restTemplate,
                         @Value("${COMPANY_SERVICE_URL:http://company-service:8091}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Get company name by ID
     */
    public String getCompanyName(Long companyId) {
        try {
            String url = baseUrl + "/company/" + companyId + "/name";

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object name = response.getBody().get("name");
                log.debug("Retrieved company name: {} for companyId: {}", name, companyId);
                return name != null ? name.toString() : null;
            }

            log.warn("Failed to get company name for companyId: {}", companyId);
            return null;

        } catch (Exception e) {
            log.error("Error fetching company name for companyId: {}", companyId, e);
            return null;
        }
    }
}