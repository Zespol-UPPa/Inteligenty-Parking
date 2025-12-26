package com.smartparking.customer_service.controller;

import com.smartparking.customer_service.service.WalletService;
import com.smartparking.customer_service.security.RequestContext;
import com.smartparking.customer_service.service.ReservationService;
import com.smartparking.customer_service.service.CustomerProfileService;
import com.smartparking.customer_service.service.VehicleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/customer")
public class CustomerController {
    private final CustomerProfileService profiles;
    private final VehicleService vehicles;
    private final ReservationService reservations;
    private final WalletService walletService;

    public CustomerController(CustomerProfileService profiles, VehicleService vehicles, ReservationService reservations, WalletService walletService) {
        this.profiles = profiles;
        this.vehicles = vehicles;
        this.reservations = reservations;
        this.walletService = walletService;
    }

    private Long requireAccountId(RequestContext ctx) {
        if (ctx == null || ctx.getUsername() == null || ctx.getUsername().isBlank()) {
            throw new IllegalArgumentException("Invalid request context: username is required");
        }
        try {
            Long accountId = Long.parseLong(ctx.getUsername());
            if (accountId <= 0) {
                throw new IllegalArgumentException("Invalid account ID: must be positive, got: " + accountId);
            }
            return accountId;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid account ID format: " + ctx.getUsername(), e);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        Optional<Map<String, Object>> profile = profiles.getById(requireAccountId(ctx));
        return profile.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(HttpServletRequest request,
                                           @RequestParam String firstName,
                                           @RequestParam String lastName) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        boolean ok = profiles.updateName(requireAccountId(ctx), firstName, lastName);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<Map<String, Object>>> listVehicles(HttpServletRequest request) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(vehicles.list(requireAccountId(ctx)));
    }

    @PostMapping("/vehicles")
    public ResponseEntity<Map<String, Object>> addVehicle(HttpServletRequest request,
                                                          @RequestParam String licencePlate) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        long id = vehicles.add(requireAccountId(ctx), licencePlate);
        return ResponseEntity.ok(Map.of("id", id));
    }

    @DeleteMapping("/vehicles/{vehicleId}")
    public ResponseEntity<?> deleteVehicle(HttpServletRequest request,
                                          @PathVariable Long vehicleId) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        boolean deleted = vehicles.delete(vehicleId, requireAccountId(ctx));
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/wallet")
    public ResponseEntity<?> getWallet(HttpServletRequest request) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        return walletService.getByAccountId(requireAccountId(ctx))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/reservations")
    public ResponseEntity<List<Map<String, Object>>> listReservations(HttpServletRequest request) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(reservations.list(requireAccountId(ctx)));
    }

    @PostMapping("/reservations")
    public ResponseEntity<Map<String, Object>> createReservation(HttpServletRequest request,
                                                                 @RequestParam Long parkingId,
                                                                 @RequestParam Long spotId,
                                                                 @RequestParam(required = false) Long durationSeconds) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        Instant start = Instant.now();
        Instant end = start.plusSeconds(durationSeconds != null ? durationSeconds : 3600);
        long id = reservations.create(requireAccountId(ctx), parkingId, spotId, start, end, "Reserved");
        return ResponseEntity.ok(Map.of("id", id));
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(HttpServletRequest request,
                                           @RequestBody Map<String, String> body) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        // Note: Password change requires communication with accounts-service
        // For now, return not implemented
        return ResponseEntity.status(501).body(Map.of("error", "Password change not yet implemented"));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(HttpServletRequest request) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        // Note: History requires implementation - may need to query parking-service
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/history/statistics")
    public ResponseEntity<?> getHistoryStatistics(HttpServletRequest request) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        // Note: Statistics requires implementation
        return ResponseEntity.ok(Map.of(
                "totalSessions", 0,
                "totalTime", 0,
                "totalSpent", 0
        ));
    }

    @PostMapping("/history/{sessionId}/pay")
    public ResponseEntity<?> payForSession(HttpServletRequest request,
                                          @PathVariable Long sessionId) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        // Note: Payment requires implementation - may need to communicate with payment-service
        return ResponseEntity.status(501).body(Map.of("error", "Payment not yet implemented"));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("customer-service: OK");
    }
}
