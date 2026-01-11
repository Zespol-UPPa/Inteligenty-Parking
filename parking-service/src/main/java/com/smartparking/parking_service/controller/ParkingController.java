package com.smartparking.parking_service.controller;

import com.smartparking.parking_service.dto.AddParkingRequest;
import com.smartparking.parking_service.service.ParkingCreationService;
import com.smartparking.parking_service.service.ParkingQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.smartparking.parking_service.dto.IdResponse;
import com.smartparking.parking_service.dto.ParkingUsageDto;

@RestController
@RequestMapping("/parking")
public class ParkingController {

    private static final Logger log = LoggerFactory.getLogger(ParkingController.class);
    private final ParkingQueryService queries;
    private final ParkingCreationService parkingCreationService;
    public ParkingController(ParkingQueryService queries, ParkingCreationService parkingCreationService) {
        this.parkingCreationService = parkingCreationService;
        this.queries = queries;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/locations")
    public ResponseEntity<List<Map<String, Object>>> listLocations() {
        return ResponseEntity.ok(queries.getLocations());
    }

    @GetMapping("/spots")
    public ResponseEntity<List<Map<String, Object>>> listSpots(@RequestParam(required = false) Long locationId) {
        return ResponseEntity.ok(queries.getSpots(locationId));
    }

    @GetMapping("/spots/for-reservation")
    public ResponseEntity<List<Map<String, Object>>> listSpotsForReservation(
            @RequestParam Long locationId) {
        return ResponseEntity.ok(queries.getSpotsForReservation(locationId));
    }

    // Admin-facing endpoints, wywoływane przez admin-service (nie bezpośrednio z frontu)
    @PostMapping("/admin/locations")
    public ResponseEntity<IdResponse> createLocation(@RequestParam String name,
                                                     @RequestParam String address,
                                                     @RequestParam Long companyId) {
        long id = queries.createLocation(name, address, companyId);
        return ResponseEntity.ok(new IdResponse(id));
    }

    @PostMapping("/admin/spots")
    public ResponseEntity<IdResponse> createSpot(@RequestParam Long locationId,
                                                 @RequestParam String code,
                                                 @RequestParam Integer floorLvl,
                                                 @RequestParam(defaultValue = "false") boolean toReserved,
                                                 @RequestParam(defaultValue = "Available") String type) {
        long id = queries.createSpot(locationId, code, floorLvl, toReserved, type);
        return ResponseEntity.ok(new IdResponse(id));
    }

    @GetMapping("/admin/reports/usage")
    public ResponseEntity<List<ParkingUsageDto>> usageReport() {
        return ResponseEntity.ok(queries.usageReport());
    }

    // Customer-facing reservation endpoints (called by customer-service)
    @GetMapping("/reservations")
    public ResponseEntity<List<Map<String, Object>>> listReservations(@RequestParam Long accountId) {
        return ResponseEntity.ok(queries.getReservationsByAccountId(accountId));
    }

    @PostMapping("/reservations")
    public ResponseEntity<IdResponse> createReservation(@RequestParam Long accountId,
                                                        @RequestParam Long parkingId,
                                                        @RequestParam Long spotId,
                                                        @RequestParam String validUntil,
                                                        @RequestParam(defaultValue = "Paid") String status) {
        java.time.Instant instant = java.time.Instant.parse(validUntil);
        long id = queries.createReservation(accountId, parkingId, spotId, instant, status);
        return ResponseEntity.ok(new IdResponse(id));
    }

    // Worker-facing endpoint for active reservations
    @GetMapping("/reservations/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveReservations() {
        return ResponseEntity.ok(queries.getActiveReservations());
    }

    // Customer-facing endpoints for parking details
    @GetMapping("/locations/{id}/details")
    public ResponseEntity<?> getLocationDetails(@PathVariable Long id) {
        java.util.Optional<Map<String, Object>> details = queries.getLocationDetails(id);
        return details.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/locations/{id}/occupancy")
    public ResponseEntity<Map<String, Object>> getOccupancyData(
            @PathVariable Long id,
            @RequestParam(required = false) Integer dayOfWeek) {
        // dayOfWeek: 0=Sunday, 1=Monday, ..., 6=Saturday
        // Jeśli nie podano, użyj aktualnego dnia tygodnia
        return ResponseEntity.ok(queries.getOccupancyData(id, dayOfWeek));
    }

    /**
     * GET /parking/locations/{parkingId}/name
     * Returns parking location name
     */
    @GetMapping("/locations/{parkingId}/name")
    public ResponseEntity<?> getParkingName(@PathVariable Long parkingId) {
        log.info("Getting parking name for parkingId: {}", parkingId);
        String name = queries.getParkingName(parkingId);

        if (name != null) {
            log.info("Found parking name: {} for parkingId: {}", name, parkingId);
            return ResponseEntity.ok(Map.of("name", name));
        }

        log.warn("Parking not found: {}", parkingId);
        return ResponseEntity.notFound().build();
    }

    /**
     * GET /parking/companies/{companyId}/parkings
     * Returns list of parking IDs for a company
     */
    @GetMapping("/companies/{companyId}/parkings")
    public ResponseEntity<List<Long>> getParkingsByCompany(@PathVariable Long companyId) {
        log.info("Getting parking IDs for companyId: {}", companyId);
        List<Long> parkingIds = queries.getParkingIdsByCompany(companyId);
        log.info("Found {} parkings for company {}", parkingIds.size(), companyId);
        return ResponseEntity.ok(parkingIds);
    }

    /**
     * GET /parking/locations/{parkingId}/stats
     * Returns statistics for a single parking (total spots, occupied, available)
     */
    @GetMapping("/locations/{parkingId}/stats")
    public ResponseEntity<Map<String, Object>> getParkingStats(@PathVariable Long parkingId) {
        log.info("Getting stats for parkingId: {}", parkingId);
        Map<String, Object> stats = queries.getParkingStats(parkingId);

        if (stats == null || stats.isEmpty()) {
            log.warn("No stats found for parking {}", parkingId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /parking/pricing/{parkingId}
     * Returns complete pricing information for a parking
     */
    @GetMapping("/pricing/{parkingId}")
    public ResponseEntity<?> getParkingPricing(@PathVariable Long parkingId) {
        log.info("Getting pricing for parkingId: {}", parkingId);
        Map<String, Object> pricing = queries.getParkingPricing(parkingId);

        if (pricing == null || pricing.isEmpty()) {
            log.warn("No pricing found for parking {}", parkingId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(pricing);
    }

    /**
     * PUT /parking/admin/pricing/{pricingId}
     * Updates pricing for a parking
     */
    @PutMapping("/admin/pricing/{pricingId}")
    public ResponseEntity<?> updatePricing(
            @PathVariable Long pricingId,
            @RequestParam Integer freeMinutes,
            @RequestParam Integer ratePerMin,
            @RequestParam Integer reservationFeeMinor) {

        log.info("Updating pricing {} - freeMinutes: {}, ratePerMin: {}, reservationFee: {}",
                pricingId, freeMinutes, ratePerMin, reservationFeeMinor);

        boolean success = queries.updatePricing(pricingId, freeMinutes, ratePerMin, reservationFeeMinor);

        if (success) {
            return ResponseEntity.ok(Map.of("message", "Pricing updated successfully"));
        } else {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to update pricing"));
        }
    }

    @PostMapping("/admin/locations/with-sections")
    public ResponseEntity<IdResponse> createParkingWithSections(
            @RequestBody AddParkingRequest request) {

        log.info("Creating parking with sections: {}", request.getName());

        try {
            // Validate request
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            if (request.getCompanyId() == null) {
                return ResponseEntity.badRequest().build();
            }

            // Create parking with sections
            Long parkingId = parkingCreationService.createParkingWithSections(request);

            log.info("Successfully created parking with id: {}", parkingId);
            return ResponseEntity.ok(new IdResponse(parkingId));

        } catch (Exception e) {
            log.error("Failed to create parking with sections", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/sessions/ids-by-parking")
    public ResponseEntity<List<Long>> getSessionIdsByParkingId(
            @RequestParam Long parkingId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<Long> sessionIds = queries.getSessionIdsByParkingId(parkingId, startDate, endDate);
        return ResponseEntity.ok(sessionIds);
    }

    @GetMapping("/sessions/ids-by-company")
    public ResponseEntity<List<Long>> getSessionIdsByCompanyId(
            @RequestParam Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<Long> sessionIds = queries.getSessionIdsByCompanyId(companyId, startDate, endDate);
        return ResponseEntity.ok(sessionIds);
    }

    @GetMapping("/sessions/{sessionId}/parking-id")
    public ResponseEntity<Long> getParkingIdBySessionId(@PathVariable Long sessionId) {
        Long parkingId = queries.getParkingIdBySessionId(sessionId);

        if (parkingId == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(parkingId);
    }
}

