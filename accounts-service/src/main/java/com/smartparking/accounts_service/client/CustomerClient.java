package com.smartparking.accounts_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class CustomerClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CustomerClient(RestTemplate restTemplate,
                         @Value("${CUSTOMER_SERVICE_URL:http://customer-service:8081}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public boolean createCustomer(Long accountId, String firstName, String lastName) {
        try {
            String url = baseUrl + "/customer/internal/create";
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("accountId", String.valueOf(accountId));
            params.add("firstName", firstName != null ? firstName : "");
            params.add("lastName", lastName != null ? lastName : "");
            
            String uri = UriComponentsBuilder.fromUriString(url)
                    .queryParams(params)
                    .toUriString();
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            // Log error but don't fail registration - customer can be created later via lazy creation
            return false;
        }
    }
}

