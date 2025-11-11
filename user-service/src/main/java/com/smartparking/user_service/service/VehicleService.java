package com.smartparking.user_service.service;

import com.smartparking.user_service.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class VehicleService {
    private final VehicleRepository vehicles;
    public VehicleService(VehicleRepository vehicles) {
        this.vehicles = vehicles;
    }
    public List<Map<String, Object>> list(Long accountId) {
        return vehicles.findByAccountId(accountId);
    }
    public long add(Long accountId, String licencePlate) {
        return vehicles.addVehicle(accountId, licencePlate);
    }
}


