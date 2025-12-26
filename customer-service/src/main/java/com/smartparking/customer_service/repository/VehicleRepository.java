package com.smartparking.customer_service.repository;
import com.smartparking.customer_service.model.Vehicle;

import java.util.List;
import java.util.Optional;
public interface VehicleRepository {
    Optional<Vehicle> findById(Long id);
    Optional<Vehicle> findByLicencePlate(String licencePlate);
    List<Vehicle> findByCustomerId(Long customerId);
    List<Vehicle> findAll();
    List<Vehicle> findUnassigned();
    Vehicle save(Vehicle vehicle);
    boolean deleteById(Long id);
}
