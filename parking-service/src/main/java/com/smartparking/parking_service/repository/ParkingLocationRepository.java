package com.smartparking.parking_service.repository;

import com.smartparking.parking_service.model.ParkingLocation;

import java.util.List;
import java.util.Optional;

public interface ParkingLocationRepository {
    Optional<ParkingLocation> findById(Long id);
    List<ParkingLocation> findByCompanyId(Long companyId);
    List<ParkingLocation> findAll();
    ParkingLocation save(ParkingLocation location);
}
