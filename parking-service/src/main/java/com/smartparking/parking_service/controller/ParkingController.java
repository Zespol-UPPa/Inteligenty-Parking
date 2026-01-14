package com.smartparking.parking_service.controller;

import com.smartparking.parking_service.dto.AddParkingRequest;
import com.smartparking.parking_service.service.ParkingCreationService;
import com.smartparking.parking_service.service.ParkingQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.smartparking.parking_service.dto.IdResponse;
import com.smartparking.parking_service.dto.ParkingUsageDto;
import com.smartparking.parking_service.service.ParkingSessionService;

@RestController
@RequestMapping("/parking")
public class ParkingController {

    private static final Logger log = LoggerFactory.getLogger(ParkingController.class);
    private final ParkingQueryService queries;
    private final ParkingCreationService parkingCreationService;
    private final ParkingSessionService sessionService;
    
    public ParkingController(ParkingQueryService queries, ParkingCreationService parkingCreationService, ParkingSessionService sessionService) {
        this.queries = queries;
        this.parkingCreationService = parkingCreationService;
        this.sessionService = sessionService;
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
    public ResponseEntity<?> listSpotsForReservation(
            @RequestParam Long locationId,
            @RequestParam(required = false) String startDateTime,
            @RequestParam(required = false) String endDateTime) {
        try {
            // Decode URL-encoded parameters if needed (Spring should do this automatically, but handle it explicitly)
            // URLDecoder.decode() is safe for already-decoded strings (it will return them as-is)
            if (startDateTime != null && !startDateTime.isBlank()) {
                try {
                    startDateTime = URLDecoder.decode(startDateTime, StandardCharsets.UTF_8);
                } catch (IllegalArgumentException e) {
                    // If decoding fails (unlikely), use the original string
                }
            }
            if (endDateTime != null && !endDateTime.isBlank()) {
                try {
                    endDateTime = URLDecoder.decode(endDateTime, StandardCharsets.UTF_8);
                } catch (IllegalArgumentException e) {
                    // If decoding fails (unlikely), use the original string
                }
            }
            
            // Parse startDateTime and endDateTime, or use defaults
            java.time.Instant start = startDateTime != null && !startDateTime.isBlank() 
                ? java.time.Instant.parse(startDateTime) 
                : java.time.Instant.now();
            java.time.Instant end = endDateTime != null && !endDateTime.isBlank()
                ? java.time.Instant.parse(endDateTime)
                : start.plusSeconds(7200); // Default 2 hours
            return ResponseEntity.ok(queries.getSpotsForReservation(locationId, start, end));
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid date format: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to parse dates: " + e.getMessage()));
        }
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
                                                        @RequestParam Long vehicleId,
                                                        @RequestParam String validFrom,
                                                        @RequestParam String validUntil,
                                                        @RequestParam(defaultValue = "Paid") String status) {
        java.time.Instant start = java.time.Instant.parse(validFrom);
        java.time.Instant end = java.time.Instant.parse(validUntil);
        
        // Sprawdź dostępność miejsca przed utworzeniem rezerwacji
        if (!queries.isSpotAvailableForTimeRange(spotId, start, end)) {
            return ResponseEntity.status(409).body(null); // Conflict - spot not available
        }
        
        long id = queries.createReservation(accountId, parkingId, spotId, vehicleId, start, end, status);
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
    // Customer-facing endpoint for active parking session
    @GetMapping("/sessions/active")
    public ResponseEntity<?> getActiveSession(@RequestParam Long accountId) {
        java.util.Optional<Map<String, Object>> session = queries.getActiveSessionByAccountId(accountId);
        return session.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Pobiera wszystkie sesje parkingowe dla danego konta użytkownika
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> getSessions(@RequestParam Long accountId,
                                                                  @RequestParam(required = false, defaultValue = "false") boolean unpaidOnly) {
        if (unpaidOnly) {
            return ResponseEntity.ok(queries.getUnpaidSessionsByAccountId(accountId));
        }
        return ResponseEntity.ok(queries.getSessionsByAccountId(accountId));
    }

    /**
     * Pobiera statystyki sesji parkingowych dla danego konta użytkownika
     */
    @GetMapping("/sessions/statistics")
    public ResponseEntity<Map<String, Object>> getSessionStatistics(@RequestParam Long accountId) {
        return ResponseEntity.ok(queries.getSessionStatistics(accountId));
    }

    /**
     * Opłaca zakończoną sesję parkingową (exit_time != NULL, status Unpaid)
     * Pobiera płatność z portfela i zmienia status na "Paid"
     */
    @PostMapping("/sessions/{sessionId}/pay")
    public ResponseEntity<?> payForSession(@PathVariable Long sessionId) {
        try {
            ParkingSessionService.PaymentResult result = sessionService.payForSession(sessionId);
            
            if (!result.isSuccess()) {
                return ResponseEntity.status(400).body(Map.of(
                    "error", result.getErrorMessage() != null ? result.getErrorMessage() : "Payment failed"
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "sessionId", result.getSessionId(),
                "amountMinor", result.getAmountMinor(),
                "amount", result.getAmountMinor() != null ? result.getAmountMinor() / 100.0 : 0.0,
                "status", "Paid",
                "message", "Session paid successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to pay session: " + e.getMessage()));
        }
    }

    @GetMapping("/pricing/{parkingId}/reservation-fee")
    public ResponseEntity<?> getReservationFee(@PathVariable Long parkingId) {
        java.util.Optional<Integer> feeOpt = queries.getReservationFee(parkingId);
        return feeOpt.map(fee -> {
            java.math.BigDecimal reservationFee = new java.math.BigDecimal(fee).divide(new java.math.BigDecimal(100));
            return ResponseEntity.ok(Map.of(
                    "reservationFeeMinor", fee,  // grosze (dla logiki biznesowej)
                    "reservationFee", reservationFee  // złotówki (dla wyświetlania)
            ));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/pricing/{parkingId}")
    public ResponseEntity<?> getPricing(@PathVariable Long parkingId) {
        java.util.Optional<com.smartparking.parking_service.model.ParkingPricing> pricingOpt = queries.getPricing(parkingId);
        return pricingOpt.map(pricing -> {
            java.math.BigDecimal ratePerMin = new java.math.BigDecimal(pricing.getRatePerMin()).divide(new java.math.BigDecimal(100));
            java.math.BigDecimal reservationFee = new java.math.BigDecimal(pricing.getReservationFeeMinor()).divide(new java.math.BigDecimal(100));
            return ResponseEntity.ok(Map.of(
                    "ratePerMin", pricing.getRatePerMin(),  // grosze za minutę
                    "ratePerMinDecimal", ratePerMin,  // złotówki za minutę
                    "freeMinutes", pricing.getFreeMinutes(),
                    "roundingStepMin", pricing.getRoundingStepMin(),
                    "reservationFeeMinor", pricing.getReservationFeeMinor(),  // grosze (deprecated - używaj ratePerMin * duration)
                    "reservationFee", reservationFee,  // złotówki (deprecated)
                    "currencyCode", pricing.getCurrencyCode() != null ? pricing.getCurrencyCode() : "PLN"
            ));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<?> getReservationById(@PathVariable Long reservationId) {
        java.util.Optional<Map<String, Object>> reservationOpt = queries.getReservationById(reservationId);
        return reservationOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<?> cancelReservation(@PathVariable Long reservationId,
                                               @RequestParam Long accountId) {
        // Walidacja: sprawdź czy rezerwacja istnieje i należy do użytkownika
        java.util.Optional<Map<String, Object>> reservationOpt = queries.getReservationById(reservationId);
        if (reservationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> reservation = reservationOpt.get();
        Long reservationAccountId = Long.valueOf(reservation.get("ref_account_id").toString());
        if (!reservationAccountId.equals(accountId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Reservation does not belong to this account"));
        }
        
        boolean cancelled = queries.cancelReservation(reservationId, accountId);
        if (cancelled) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(400).body(Map.of("error", "Cannot cancel reservation - invalid status or already started"));
        }
    }
}

