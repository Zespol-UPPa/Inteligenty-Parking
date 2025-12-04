package com.smartparking.worker_service.controller;

import com.smartparking.worker_service.client.ParkingReservationClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/worker")
public class WorkerController {

    private final ParkingReservationClient parkingClient;
    
    public WorkerController(ParkingReservationClient parkingClient) {
        this.parkingClient = parkingClient;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/reservations/active")
    public ResponseEntity<List<Map<String, Object>>> activeReservations() {
        // Get active reservations from parking-service via HTTP
        List<Map<String, Object>> reservations = parkingClient.getActiveReservations();
        return ResponseEntity.ok(reservations);
    }
}


