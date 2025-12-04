package com.smartparking.worker_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class ParkingReservationClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ParkingReservationClient(RestTemplate restTemplate,
                                    @Value("${PARKING_SERVICE_URL:http://parking-service:8083}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<Map<String, Object>> getActiveReservations() {
        try {
            String url = baseUrl + "/parking/reservations/active";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }
}

