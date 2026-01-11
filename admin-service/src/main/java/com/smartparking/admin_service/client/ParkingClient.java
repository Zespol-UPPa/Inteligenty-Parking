package com.smartparking.admin_service.client;

import com.smartparking.admin_service.dto.IdResponse;
import com.smartparking.admin_service.dto.ParkingUsageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ParkingClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    private static final Logger log = LoggerFactory.getLogger(ParkingClient.class);

    public ParkingClient(RestTemplate restTemplate,
                         @Value("${PARKING_SERVICE_URL:http://parking-service:8083}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public long createLocation(String name, String address, Long companyId) {
        try {
            String url = baseUrl + "/parking/admin/locations?name={name}&address={address}&companyId={companyId}";
            ResponseEntity<IdResponse> response = restTemplate.postForEntity(
                    url,
                    null,
                    IdResponse.class,
                    Map.of("name", name, "address", address, "companyId", companyId)
            );
            IdResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Parking-service did not return location id");
            }
            return body.getId();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create location in parking-service: " + e.getMessage(), e);
        }
    }

    public long createSpot(Long locationId, String code, Integer floorLvl, boolean toReserved, String type) {
        try {
            String url = baseUrl + "/parking/admin/spots?locationId={locationId}&code={code}&floorLvl={floorLvl}&toReserved={toReserved}&type={type}";
            ResponseEntity<IdResponse> response = restTemplate.postForEntity(
                    url,
                    null,
                    IdResponse.class,
                    Map.of(
                            "locationId", locationId,
                            "code", code,
                            "floorLvl", floorLvl,
                            "toReserved", toReserved,
                            "type", type
                    )
            );
            IdResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Parking-service did not return spot id");
            }
            return body.getId();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create spot in parking-service: " + e.getMessage(), e);
        }
    }

    public List<ParkingUsageDto> usageReport() {
        try {
            String url = baseUrl + "/parking/admin/reports/usage";
            ResponseEntity<ParkingUsageDto[]> response =
                    restTemplate.getForEntity(url, ParkingUsageDto[].class);
            ParkingUsageDto[] body = response.getBody();
            if (body == null) {
                return List.of();
            }
            return List.of(body);
        } catch (Exception e) {
            return List.of();
        }
    }

    // ========================================
    // NEW METHODS FOR PARKING MANAGEMENT
    // ========================================

    /**
     * Get list of parking IDs for a company
     */
    public List<Long> getParkingIdsByCompany(Long companyId) {
        try {
            String url = baseUrl + "/parking/companies/{companyId}/parkings";
            ResponseEntity<List> response = restTemplate.getForEntity(
                    url,
                    List.class,
                    companyId
            );
            List<?> body = response.getBody();
            if (body != null) {
                List<Long> parkingIds = new ArrayList<>();
                for (Object item : body) {
                    if (item instanceof Number) {
                        parkingIds.add(((Number) item).longValue());
                    }
                }
                return parkingIds;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Failed to get parking IDs for company " + companyId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get statistics for a single parking
     */
    public Map<String, Object> getParkingStats(Long parkingId) {
        try {
            String url = baseUrl + "/parking/locations/{parkingId}/stats";
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    url,
                    Map.class,
                    parkingId
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Failed to get stats for parking " + parkingId + ": " + e.getMessage());
            return Map.of();
        }
    }

    /**
     * Get pricing information for a parking
     */
    public Map<String, Object> getParkingPricing(Long parkingId) {
        try {
            String url = baseUrl + "/parking/pricing/{parkingId}";
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    url,
                    Map.class,
                    parkingId
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Failed to get pricing for parking " + parkingId + ": " + e.getMessage());
            return Map.of();
        }
    }

    /**
     * Update pricing for a parking
     */
    public boolean updatePricing(Long pricingId, Integer freeMinutes, Integer ratePerMin, Integer reservationFeeMinor) {
        try {
            String url = baseUrl + "/parking/admin/pricing/{pricingId}?freeMinutes={freeMinutes}&ratePerMin={ratePerMin}&reservationFeeMinor={reservationFeeMinor}";

            restTemplate.put(
                    url,
                    null,
                    Map.of(
                            "pricingId", pricingId,
                            "freeMinutes", freeMinutes,
                            "ratePerMin", ratePerMin,
                            "reservationFeeMinor", reservationFeeMinor
                    )
            );

            return true;
        } catch (Exception e) {
            System.err.println("Failed to update pricing " + pricingId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Get company name by companyId
     */
    public String getCompanyName(Long companyId) {
        try {
            String url = baseUrl + "/parking/companies/{companyId}/name";
            ResponseEntity<Map<String, String>> response = restTemplate.getForEntity(
                    url,
                    (Class<Map<String, String>>) (Class<?>) Map.class,
                    companyId
            );
            Map<String, String> body = response.getBody();
            if (body != null && body.containsKey("name")) {
                return body.get("name");
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get parking name by parkingId
     */
    public String getParkingName(Long parkingId) {
        try {
            String url = baseUrl + "/parking/locations/{parkingId}/name";
            ResponseEntity<Map<String, String>> response = restTemplate.getForEntity(
                    url,
                    (Class<Map<String, String>>) (Class<?>) Map.class,
                    parkingId
            );
            Map<String, String> body = response.getBody();
            if (body != null && body.containsKey("name")) {
                return body.get("name");
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Create parking with sections
     * POST /parking/admin/locations/with-sections
     */
    public Long createParkingWithSections(Map<String, Object> request) {
        try {
            String url = baseUrl + "/parking/admin/locations/with-sections";

            ResponseEntity<IdResponse> response = restTemplate.postForEntity(
                    url,
                    request,
                    IdResponse.class
            );

            IdResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Parking-service did not return parking id");
            }

            return body.getId();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to create parking in parking-service: " + e.getMessage(), e);
        }
    }

    /**
     * Get session IDs for a parking in date range
     * Used by FinancialReportService to get payments for specific parking
     */
    public List<Long> getSessionIdsByParkingId(
            Long parkingId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        try {
            String url = baseUrl + "/parking/sessions/ids-by-parking?" +
                    "parkingId={parkingId}&startDate={startDate}&endDate={endDate}";

            log.info("Getting session IDs for parking {}", parkingId);

            ResponseEntity<List> response = restTemplate.getForEntity(
                    url,
                    List.class,
                    Map.of(
                            "parkingId", parkingId,
                            "startDate", startDate.toString(),
                            "endDate", endDate.toString()
                    )
            );

            List<?> body = response.getBody();
            if (body != null) {
                List<Long> sessionIds = body.stream()
                        .map(item -> ((Number) item).longValue())
                        .collect(Collectors.toList());

                log.info("Found {} sessions for parking {}", sessionIds.size(), parkingId);
                return sessionIds;
            }

            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Failed to get session IDs for parking " + parkingId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Get session IDs for all parkings in a company
     * Used by FinancialReportService for company-wide reports
     */
    public List<Long> getSessionIdsByCompanyId(
            Long companyId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        try {
            String url = baseUrl + "/parking/sessions/ids-by-company?" +
                    "companyId={companyId}&startDate={startDate}&endDate={endDate}";

            log.info("Getting session IDs for company {}", companyId);

            ResponseEntity<List> response = restTemplate.getForEntity(
                    url,
                    List.class,
                    Map.of(
                            "companyId", companyId,
                            "startDate", startDate.toString(),
                            "endDate", endDate.toString()
                    )
            );

            List<?> body = response.getBody();
            if (body != null) {
                List<Long> sessionIds = body.stream()
                        .map(item -> ((Number) item).longValue())
                        .collect(Collectors.toList());

                log.info("Found {} sessions for company {}", sessionIds.size(), companyId);
                return sessionIds;
            }

            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Failed to get session IDs for company " + companyId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Get parking ID for a specific session
     * Used to enrich transaction data with parking names
     */
    public Long getParkingIdBySessionId(Long sessionId) {
        try {
            String url = baseUrl + "/parking/sessions/{sessionId}/parking-id";

            ResponseEntity<Long> response = restTemplate.getForEntity(
                    url,
                    Long.class,
                    sessionId
            );

            return response.getBody();

        } catch (Exception e) {
            log.warn("Failed to get parking ID for session {}: {}", sessionId, e.getMessage());
            return null;
        }
    }}