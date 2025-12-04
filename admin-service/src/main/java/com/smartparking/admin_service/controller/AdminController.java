package com.smartparking.admin_service.controller;

import com.smartparking.admin_service.client.ParkingAdminClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import com.smartparking.admin_service.dto.IdResponse;
import com.smartparking.admin_service.dto.ParkingUsageDto;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final ParkingAdminClient parkingClient;

    public AdminController(ParkingAdminClient parkingClient) {
        this.parkingClient = parkingClient;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/locations")
    public ResponseEntity<IdResponse> addLocation(@RequestParam String name,
                                                  @RequestParam String address,
                                                  @RequestParam Long companyId) {
        long id = parkingClient.createLocation(name, address, companyId);
        return ResponseEntity.ok(new IdResponse(id));
    }

    @PostMapping("/spots")
    public ResponseEntity<IdResponse> addSpot(@RequestParam Long locationId,
                                              @RequestParam String code,
                                              @RequestParam Integer floorLvl,
                                              @RequestParam(defaultValue = "false") boolean toReserved,
                                              @RequestParam(defaultValue = "Available") String type) {
        long id = parkingClient.createSpot(locationId, code, floorLvl, toReserved, type);
        return ResponseEntity.ok(new IdResponse(id));
    }

    @GetMapping("/reports/usage")
    public ResponseEntity<List<ParkingUsageDto>> usageReport() {
        return ResponseEntity.ok(parkingClient.usageReport());
    }
}

