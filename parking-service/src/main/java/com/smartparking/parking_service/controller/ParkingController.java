package com.smartparking.parking_service.controller;

import com.smartparking.parking_service.service.ParkingQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    // For now, reservations are created in user-service; parking-service remains read-only for spots/locations here.
}


