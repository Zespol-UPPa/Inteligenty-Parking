package com.smartparking.customer_service.service;

import com.smartparking.customer_service.model.Customer;
import com.smartparking.customer_service.model.Vehicle;
import com.smartparking.customer_service.repository.CustomerRepository;
import com.smartparking.customer_service.repository.JdbcVehicleRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VehicleService {
    private final JdbcVehicleRepository vehicles;
    private final CustomerRepository customerRepository;
    
    public VehicleService(JdbcVehicleRepository vehicles, CustomerRepository customerRepository) {
        this.vehicles = vehicles;
        this.customerRepository = customerRepository;
    }
    
    public List<Map<String, Object>> list(Long accountId) {
        // accountId refers to ref_account_id in customer table
        // First find customer by ref_account_id, then find vehicles by customer_id
        Optional<Customer> customerOpt = customerRepository.findByAccountId(accountId);
        if (customerOpt.isEmpty()) {
            return List.of();
        }
        Customer customer = customerOpt.get();
        return vehicles.findByCustomerId(customer.getId()).stream()
                .map(v -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", v.getId());
                    map.put("licencePlate", v.getLicencePlate());
                    map.put("customerId", v.getCustomerId());
                    return map;
                })
                .collect(Collectors.toList());
    }
    
    public long add(Long accountId, String licencePlate) {
        // accountId refers to ref_account_id in customer table
        // First find customer by ref_account_id, then use customer_id for vehicle
        Optional<Customer> customerOpt = customerRepository.findByAccountId(accountId);
        if (customerOpt.isEmpty()) {
            throw new IllegalArgumentException("Customer not found for accountId: " + accountId);
        }
        Customer customer = customerOpt.get();
        Vehicle v = new Vehicle();
        v.setLicencePlate(licencePlate);
        v.setCustomerId(customer.getId());
        return vehicles.save(v).getId();
    }
    
    public boolean delete(Long vehicleId, Long accountId) {
        // Verify vehicle belongs to account before deleting
        // accountId refers to ref_account_id in customer table
        Optional<Customer> customerOpt = customerRepository.findByAccountId(accountId);
        if (customerOpt.isEmpty()) {
            return false;
        }
        Customer customer = customerOpt.get();
        Optional<Vehicle> vehicle = vehicles.findById(vehicleId);
        if (vehicle.isEmpty() || !vehicle.get().getCustomerId().equals(customer.getId())) {
            return false;
        }
        return vehicles.deleteById(vehicleId);
    }
}
