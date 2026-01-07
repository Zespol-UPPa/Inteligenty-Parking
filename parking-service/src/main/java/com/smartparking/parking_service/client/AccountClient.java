package com.smartparking.parking_service.client;

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
                    new ParameterizedTypeReference<Map<String, String>>() {},
                    Map.of("accountId", accountId)
            );
            if (response.getBody() != null && response.getBody().containsKey("email")) {
                return Optional.of(response.getBody().get("email"));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

