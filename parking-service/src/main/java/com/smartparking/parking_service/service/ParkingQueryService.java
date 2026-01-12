package com.smartparking.parking_service.service;

import com.smartparking.parking_service.repository.ParkingRepository;
import com.smartparking.parking_service.repository.ParkingPricingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public List<Map<String, Object>> getSpotsForReservation(Long locationId, java.time.Instant start, java.time.Instant end) {
        return repo.listSpotsForReservation(locationId, start, end);
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
    
    public long createReservation(Long accountId, Long parkingId, Long spotId, Long vehicleId, java.time.Instant validFrom, java.time.Instant validUntil, String status) {
        return repo.createReservation(accountId, parkingId, spotId, vehicleId, validFrom, validUntil, status);
    }

    public boolean isSpotAvailableForTimeRange(Long spotId, java.time.Instant start, java.time.Instant end) {
        return repo.isSpotAvailableForTimeRange(spotId, start, end);
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

    public java.util.Optional<com.smartparking.parking_service.model.ParkingPricing> getPricing(Long parkingId) {
        return pricingRepo.findByParkingId(parkingId);
    }

    public boolean cancelReservation(Long reservationId, Long accountId) {
        return repo.cancelReservation(reservationId, accountId);
    }
    
    /**
     * Pobiera rezerwację po ID
     */
    public Optional<Map<String, Object>> getReservationById(Long reservationId) {
        return repo.findReservationById(reservationId);
    }

    // Parking session operations
    public java.util.Optional<Map<String, Object>> getActiveSessionByAccountId(Long accountId) {
        return repo.findActiveSessionByAccountId(accountId);
    }

    /**
     * Pobiera wszystkie sesje parkingowe dla danego konta użytkownika
     */
    public List<Map<String, Object>> getSessionsByAccountId(Long accountId) {
        return repo.findSessionsByAccountId(accountId, false); // Wszystkie sesje
    }

    /**
     * Pobiera tylko niezapłacone sesje parkingowe dla danego konta użytkownika
     */
    public List<Map<String, Object>> getUnpaidSessionsByAccountId(Long accountId) {
        return repo.findSessionsByAccountId(accountId, true); // Tylko niezapłacone
    }

    /**
     * Oblicza statystyki sesji parkingowych dla danego konta użytkownika
     * @param accountId ID konta użytkownika
     * @return Map ze statystykami: totalSessions, totalHours, totalSpent
     */
    public Map<String, Object> getSessionStatistics(Long accountId) {
        return repo.getSessionStatistics(accountId);
    }
}


