package com.smartparking.parking_service.service;

import com.smartparking.parking_service.repository.ParkingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ParkingQueryService {
    private final ParkingRepository repo;
    public ParkingQueryService(ParkingRepository repo) {
        this.repo = repo;
    }
    public List<Map<String, Object>> getLocations() {
        return repo.listLocations();
    }
    public List<Map<String, Object>> getSpots(Long locationId) {
        return repo.listSpots(locationId);
    }
}


