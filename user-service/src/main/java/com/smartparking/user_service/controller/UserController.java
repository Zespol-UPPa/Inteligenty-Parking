package com.smartparking.user_service.controller;

import com.smartparking.security.RequestContext;
import com.smartparking.user_service.service.ReservationService;
import com.smartparking.user_service.service.UserProfileService;
import com.smartparking.user_service.service.VehicleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserProfileService profiles;
    private final VehicleService vehicles;
    private final ReservationService reservations;

    public UserController(UserProfileService profiles, VehicleService vehicles, ReservationService reservations) {
        this.profiles = profiles;
        this.vehicles = vehicles;
        this.reservations = reservations;
    }

    private Long requireAccountId(RequestContext ctx) {
        return Long.parseLong(ctx.getSubject());
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
}


