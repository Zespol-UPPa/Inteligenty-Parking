package com.smartparking.worker_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class ParkingClient {
    private static final Logger log = LoggerFactory.getLogger(ParkingClient.class);
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ParkingClient(RestTemplate restTemplate,
                         @Value("${PARKING_SERVICE_URL:http://parking-service:8083}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Get parking location name by ID
     */
    public String getParkingName(Long parkingId) {
        try {
            String url = baseUrl + "/parking/locations/" + parkingId + "/name";

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object name = response.getBody().get("name");
                log.debug("Retrieved parking name: {} for parkingId: {}", name, parkingId);
                return name != null ? name.toString() : null;
            }

            log.warn("Failed to get parking name for parkingId: {}", parkingId);
            return null;

        } catch (Exception e) {
            log.error("Error fetching parking name for parkingId: {}", parkingId, e);
            return null;
        }
    }

    /**
     * Get session IDs by parking ID and date range
     * Used for financial reports
     */
    public List<Long> getSessionIdsByParkingId(
            Long parkingId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        try {
            // Format dates for URL (ISO-8601 format)
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            String startDateStr = startDate.format(formatter);
            String endDateStr = endDate.format(formatter);

            String url = baseUrl + "/parking/sessions/ids-by-parking" +
                    "?parkingId=" + parkingId +
                    "&startDate=" + startDateStr +
                    "&endDate=" + endDateStr;

            log.debug("Fetching session IDs from: {}", url);

            ResponseEntity<List<Long>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Long>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Long> sessionIds = response.getBody();
                log.info("Retrieved {} session IDs for parking {} between {} and {}",
                        sessionIds.size(), parkingId, startDateStr, endDateStr);
                return sessionIds;
            }

            log.warn("Failed to get session IDs for parking {}", parkingId);
            return List.of();

        } catch (Exception e) {
            log.error("Error fetching session IDs for parking {}", parkingId, e);
            return List.of();
        }
    }
}