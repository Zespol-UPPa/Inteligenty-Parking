package com.smartparking.parking_service.controller;

import com.smartparking.parking_service.service.ParkingQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.smartparking.parking_service.dto.IdResponse;
import com.smartparking.parking_service.dto.ParkingUsageDto;

@RestController
@RequestMapping("/parking")
public class ParkingController {

    private final ParkingQueryService queries;
    public ParkingController(ParkingQueryService queries) {
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

    @GetMapping("/pricing/{parkingId}/reservation-fee")
    public ResponseEntity<?> getReservationFee(@PathVariable Long parkingId) {
        java.util.Optional<Integer> feeOpt = queries.getReservationFee(parkingId);
        return feeOpt.map(fee -> ResponseEntity.ok(Map.of("reservationFeeMinor", fee)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

