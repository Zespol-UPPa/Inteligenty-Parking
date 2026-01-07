package com.smartparking.parking_service.service;

import com.smartparking.parking_service.repository.ParkingRepository;
import com.smartparking.parking_service.repository.ParkingPricingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.smartparking.parking_service.dto.ParkingUsageDto;

@Service
public class ParkingQueryService {
    private final ParkingRepository repo;
    private final ParkingPricingRepository pricingRepo;
    
    public ParkingQueryService(ParkingRepository repo, ParkingPricingRepository pricingRepo) {
        this.repo = repo;
        this.pricingRepo = pricingRepo;
    }

    public List<Map<String, Object>> getLocations() {
        return repo.listLocations();
    }

    public List<Map<String, Object>> getSpots(Long locationId) {
        return repo.listSpots(locationId);
    }

    public List<Map<String, Object>> getSpotsForReservation(Long locationId) {
        return repo.listSpotsForReservation(locationId);
    }

    public long createLocation(String name, String address, Long companyId) {
        return repo.createLocation(name, address, companyId);
    }

    public long createSpot(Long locationId, String code, Integer floorLvl, boolean toReserved, String type) {
        return repo.createSpot(locationId, code, floorLvl, toReserved, type);
    }

    public List<ParkingUsageDto> usageReport() {
        return repo.usageReport();
    }

    // Reservation operations
    public List<Map<String, Object>> getReservationsByAccountId(Long accountId) {
        return repo.findReservationsByAccountId(accountId);
    }

    public long createReservation(Long accountId, Long parkingId, Long spotId, java.time.Instant validUntil, String status) {
        return repo.createReservation(accountId, parkingId, spotId, validUntil, status);
    }

    public List<Map<String, Object>> getActiveReservations() {
        return repo.findActiveReservations();
    }

    public java.util.Optional<Map<String, Object>> getLocationDetails(Long locationId) {
        return repo.getLocationDetails(locationId);
    }

    public Map<String, Object> getOccupancyData(Long locationId, Integer dayOfWeek) {
        return repo.getOccupancyData(locationId, dayOfWeek);
    }

    public java.util.Optional<Integer> getReservationFee(Long parkingId) {
        return pricingRepo.findByParkingId(parkingId)
                .map(pricing -> pricing.getReservationFeeMinor());
    }
}


