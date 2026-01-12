package com.smartparking.customer_service.controller;

import com.smartparking.customer_service.model.Vehicle;
import com.smartparking.customer_service.repository.VehicleRepository;
import com.smartparking.customer_service.repository.CustomerRepository;
import com.smartparking.customer_service.service.VehicleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/customer/internal/vehicles")
public class InternalVehicleController {
    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    private final VehicleService vehicleService;
    private final String internalToken;

    public InternalVehicleController(VehicleRepository vehicleRepository,
                                     CustomerRepository customerRepository,
                                     VehicleService vehicleService,
                                     @Value("${INTERNAL_SERVICE_TOKEN:}") String internalToken) {
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
        this.vehicleService = vehicleService;
        this.internalToken = internalToken;
    }

    private boolean checkToken(HttpServletRequest request) {
        if (internalToken == null || internalToken.isBlank()) {
            return false;
        }
        String provided = request.getHeader("X-Internal-Token");
        if (provided == null || provided.isBlank()) {
            return false;
        }
        return constantTimeEquals(provided, internalToken);
    }
    
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    @GetMapping("/by-plate")
    public ResponseEntity<?> getVehicleByPlate(@RequestParam String licencePlate, HttpServletRequest request) {
        if (!checkToken(request)) return ResponseEntity.status(403).build();
        
        String normalizedPlate = licencePlate.toUpperCase().trim();
        Optional<Vehicle> vehicleOpt = vehicleRepository.findByLicencePlate(normalizedPlate);
        
        if (vehicleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Vehicle vehicle = vehicleOpt.get();
        Long customerId = vehicle.getCustomerId();
        Long accountId = null;
        
        if (customerId != null) {
            // Znajdź accountId przez customerId (dla zarejestrowanych)
            Optional<com.smartparking.customer_service.model.Customer> customerOpt = 
                customerRepository.findById(customerId);
            if (customerOpt.isPresent()) {
                accountId = customerOpt.get().getRefAccountId();
            }
        }
        // Jeśli customerId == null, to niezarejestrowany pojazd - accountId pozostaje null
        
        return ResponseEntity.ok(Map.of(
            "vehicleId", vehicle.getId(),
            "licencePlate", vehicle.getLicencePlate(),
            "customerId", customerId != null ? customerId : "",
            "accountId", accountId != null ? accountId : ""
        ));
    }
    
    /**
     * Tworzy lub znajduje pojazd po tablicy rejestracyjnej
     * Jeśli pojazd nie istnieje, tworzy nowy bez właściciela (customer_id = null)
     * Używane przez parking-service dla niezarejestrowanych klientów
     */
    @PostMapping("/create-or-get")
    public ResponseEntity<?> createOrGetVehicle(@RequestParam String licencePlate, HttpServletRequest request) {
        if (!checkToken(request)) return ResponseEntity.status(403).build();
        
        try {
            Vehicle vehicle = vehicleService.createOrGetVehicle(licencePlate);
            Long customerId = vehicle.getCustomerId();
            Long accountId = null;
            
            if (customerId != null) {
                Optional<com.smartparking.customer_service.model.Customer> customerOpt = 
                    customerRepository.findById(customerId);
                if (customerOpt.isPresent()) {
                    accountId = customerOpt.get().getRefAccountId();
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "vehicleId", vehicle.getId(),
                "licencePlate", vehicle.getLicencePlate(),
                "customerId", customerId != null ? customerId : "",
                "accountId", accountId != null ? accountId : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}

