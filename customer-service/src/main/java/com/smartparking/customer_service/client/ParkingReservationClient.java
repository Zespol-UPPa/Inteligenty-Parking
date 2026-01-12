package com.smartparking.customer_service.client;

import com.smartparking.customer_service.dto.IdResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    
    public long createReservation(Long accountId, Long parkingId, Long spotId, Long vehicleId, Instant validFrom, Instant validUntil, String status) {
        try {
            String url = baseUrl + "/parking/reservations?accountId={accountId}&parkingId={parkingId}&spotId={spotId}&vehicleId={vehicleId}&validFrom={validFrom}&validUntil={validUntil}&status={status}";
            ResponseEntity<IdResponse> response = restTemplate.postForEntity(
                    url,
                    null,
                    IdResponse.class,
                    Map.of(
                            "accountId", accountId,
                            "parkingId", parkingId,
                            "spotId", spotId,
                            "vehicleId", vehicleId,
                            "validFrom", validFrom.toString(),
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

    public Optional<Map<String, Object>> getLocationDetails(Long parkingId) {
        try {
            String url = baseUrl + "/parking/locations/{parkingId}/details";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {},
                    Map.of("parkingId", parkingId)
            );
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Integer> getReservationFee(Long parkingId) {
        try {
            String url = baseUrl + "/parking/pricing/{parkingId}/reservation-fee";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {},
                    Map.of("parkingId", parkingId)
            );
            if (response.getBody() != null && response.getBody().containsKey("reservationFeeMinor")) {
                Object feeObj = response.getBody().get("reservationFeeMinor");
                if (feeObj instanceof Integer) {
                    return Optional.of((Integer) feeObj);
                } else if (feeObj instanceof Number) {
                    return Optional.of(((Number) feeObj).intValue());
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Map<String, Object>> getPricing(Long parkingId) {
        try {
            String url = baseUrl + "/parking/pricing/{parkingId}";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {},
                    Map.of("parkingId", parkingId)
            );
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Map<String, Object>> getReservationById(Long reservationId) {
        try {
            String url = baseUrl + "/parking/reservations/{reservationId}";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {},
                    Map.of("reservationId", reservationId)
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public boolean cancelReservation(Long reservationId, Long accountId) {
        try {
            String url = baseUrl + "/parking/reservations/{reservationId}?accountId={accountId}";
            restTemplate.delete(url, Map.of("reservationId", reservationId, "accountId", accountId));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<Map<String, Object>> getActiveSession(Long accountId) {
        try {
            String url = baseUrl + "/parking/sessions/active?accountId={accountId}";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {},
                    Map.of("accountId", accountId)
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<Map<String, Object>> getSessions(Long accountId, boolean unpaidOnly) {
        try {
            String url = baseUrl + "/parking/sessions?accountId={accountId}&unpaidOnly={unpaidOnly}";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                    Map.of("accountId", accountId, "unpaidOnly", unpaidOnly)
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Map<String, Object>> getUnpaidSessions(Long accountId) {
        return getSessions(accountId, true);
    }

    public ResponseEntity<Map<String, Object>> payForSession(Long sessionId) {
        try {
            String url = baseUrl + "/parking/sessions/{sessionId}/pay";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {},
                    Map.of("sessionId", sessionId)
            );
            return response;
        } catch (HttpClientErrorException e) {
            // Przekaż błąd HTTP - zostanie obsłużony w kontrolerze
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to pay session in parking-service: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getSessionStatistics(Long accountId) {
        try {
            String url = baseUrl + "/parking/sessions/statistics?accountId={accountId}";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {},
                    Map.of("accountId", accountId)
            );
            return response.getBody() != null ? response.getBody() : Map.of("totalSessions", 0L, "totalHours", 0.0, "totalSpent", 0.0);
        } catch (Exception e) {
            return Map.of("totalSessions", 0L, "totalHours", 0.0, "totalSpent", 0.0);
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}

