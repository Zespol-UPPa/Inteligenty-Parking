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
public class CustomerVehicleClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CustomerVehicleClient(RestTemplate restTemplate,
                                @Value("${CUSTOMER_SERVICE_URL:http://customer-service:8081}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Znajduje pojazd po tablicy rejestracyjnej i zwraca accountId oraz vehicleId
     * @param licencePlate - tablica rejestracyjna (znormalizowana: uppercase, trimmed)
     * @return Optional z Map zawierającą accountId i vehicleId, lub empty jeśli nie znaleziono
     */
    public Optional<Map<String, Object>> findVehicleByPlate(String licencePlate) {
        try {
            String url = baseUrl + "/customer/internal/vehicles/by-plate?licencePlate={plate}";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            // Użyj X-Internal-Token zgodnie z InternalWalletController
            headers.set("X-Internal-Token", getInternalToken());
            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {},
                    Map.of("plate", licencePlate)
            );
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @org.springframework.beans.factory.annotation.Value("${INTERNAL_SERVICE_TOKEN:}")
    private String internalToken;

    private String getInternalToken() {
        return internalToken != null ? internalToken : "";
    }
}

