package com.smartparking.customer_service.service;

import com.smartparking.customer_service.repository.JdbcVehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class VehicleService {
    private final JdbcVehicleRepository vehicles;
    public VehicleService(JdbcVehicleRepository vehicles) {
        this.vehicles = vehicles;
    }
    public List<Map<String, Object>> list(Long accountId) {
        return vehicles.findByAccountId(accountId);
    }
    public long add(Long accountId, String licencePlate) {
        return vehicles.add(accountId, licencePlate);
    }
}

