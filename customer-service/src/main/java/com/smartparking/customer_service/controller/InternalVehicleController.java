package com.smartparking.customer_service.controller;

import com.smartparking.customer_service.model.Vehicle;
import com.smartparking.customer_service.repository.VehicleRepository;
import com.smartparking.customer_service.repository.CustomerRepository;
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
    private final String internalToken;

    public InternalVehicleController(VehicleRepository vehicleRepository,
                                     CustomerRepository customerRepository,
                                     @Value("${INTERNAL_SERVICE_TOKEN:}") String internalToken) {
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
        this.internalToken = internalToken;
    }

    private boolean checkToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        return internalToken != null && !internalToken.isBlank() && token.equals(internalToken);
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
        if (customerId == null) {
            return ResponseEntity.ok(Map.of(
                "vehicleId", vehicle.getId(),
                "licencePlate", vehicle.getLicencePlate(),
                "accountId", null
            ));
        }
        
        // Znajdź accountId przez customerId
        Optional<com.smartparking.customer_service.model.Customer> customerOpt = 
            customerRepository.findById(customerId);
        
        if (customerOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "vehicleId", vehicle.getId(),
                "licencePlate", vehicle.getLicencePlate(),
                "accountId", null
            ));
        }
        
        Long accountId = customerOpt.get().getRefAccountId();
        return ResponseEntity.ok(Map.of(
            "vehicleId", vehicle.getId(),
            "licencePlate", vehicle.getLicencePlate(),
            "accountId", accountId
        ));
    }
}

