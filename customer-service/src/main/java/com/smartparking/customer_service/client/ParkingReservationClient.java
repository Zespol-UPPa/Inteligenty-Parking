package com.smartparking.customer_service.client;

import com.smartparking.customer_service.dto.IdResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
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

    public List<Map<String, Object>> getReservationsByAccountId(Long accountId) {
        try {
            String url = baseUrl + "/parking/reservations?accountId={accountId}";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                    Map.of("accountId", accountId)
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    public long createReservation(Long accountId, Long parkingId, Long spotId, Instant validUntil, String status) {
        try {
            String url = baseUrl + "/parking/reservations?accountId={accountId}&parkingId={parkingId}&spotId={spotId}&validUntil={validUntil}&status={status}";
            ResponseEntity<IdResponse> response = restTemplate.postForEntity(
                    url,
                    null,
                    IdResponse.class,
                    Map.of(
                            "accountId", accountId,
                            "parkingId", parkingId,
                            "spotId", spotId,
                            "validUntil", validUntil.toString(),
                            "status", status
                    )
            );
            IdResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Parking-service did not return reservation id");
            }
            return body.getId();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create reservation in parking-service: " + e.getMessage(), e);
        }
    }
}

