package com.smartparking.parking_service.service;

import com.smartparking.parking_service.model.ParkingLocation;
import com.smartparking.parking_service.model.ParkingPricing;
import com.smartparking.parking_service.model.ParkingSession;
import com.smartparking.parking_service.model.ParkingSpot;
import com.smartparking.parking_service.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.smartparking.parking_service.dto.ParkingUsageDto;

@Service
public class ParkingQueryService {
    private final ParkingRepository repo;
    private final ParkingPricingRepository pricingRepo;
    private final ParkingLocationRepository parkingLocationRepository;
    private final ParkingSpotRepository spotRepo;
    private final ParkingSessionRepository sessionRepo;

    public ParkingQueryService(ParkingRepository repo,
                               ParkingPricingRepository pricingRepo,
                               ParkingLocationRepository parkingLocationRepository,
                               ParkingSpotRepository spotRepo,
                               ParkingSessionRepository sessionRepo) {
        this.repo = repo;
        this.pricingRepo = pricingRepo;
        this.parkingLocationRepository = parkingLocationRepository;
        this.spotRepo = spotRepo;
        this.sessionRepo = sessionRepo;
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

    public String getParkingName(Long parkingId) {
        return parkingLocationRepository.getNameById(parkingId);
    }


    /**
     * Get list of parking IDs for a company
     */
    public List<Long> getParkingIdsByCompany(Long companyId) {
        return parkingLocationRepository.getIdsByCompanyId(companyId);
    }

    /**
     * Get parking statistics (name, address, total spots, occupied spots, available spots)
     * Uses existing repositories - NO direct SQL queries
     */
    public Map<String, Object> getParkingStats(Long parkingId) {
        // Get parking location info
        Optional<ParkingLocation> locationOpt = parkingLocationRepository.findById(parkingId);

        if (locationOpt.isEmpty()) {
            return null;
        }

        ParkingLocation location = locationOpt.get();

        // Count total spots using SpotRepository
        List<ParkingSpot> allSpots = spotRepo.findByParkingId(parkingId);
        int totalSpots = allSpots.size();

        // Count occupied spots (active sessions where exit_time IS NULL)
        List<ParkingSession> activeSessions = sessionRepo.findByParkingId(parkingId)
                .stream()
                .filter(session -> session.getExitTime() == null)
                .toList();
        int occupiedSpots = activeSessions.size();

        int availableSpots = totalSpots - occupiedSpots;

        Map<String, Object> stats = new HashMap<>();
        stats.put("parkingId", parkingId);
        stats.put("name", location.getNameParking());
        stats.put("address", location.getAddressLine());
        stats.put("totalSpots", totalSpots);
        stats.put("occupiedSpots", occupiedSpots);
        stats.put("availableSpots", availableSpots);

        return stats;
    }

    /**
     * Get complete pricing information for a parking
     * Returns pricingId so frontend can update pricing
     */
    public Map<String, Object> getParkingPricing(Long parkingId) {
        Optional<ParkingPricing> pricingOpt = pricingRepo.findByParkingId(parkingId);

        if (pricingOpt.isEmpty()) {
            return null;
        }

        ParkingPricing pricing = pricingOpt.get();

        Map<String, Object> result = new HashMap<>();
        result.put("pricingId", pricing.getId());
        result.put("parkingId", pricing.getParkingId());
        result.put("currencyCode", pricing.getCurrencyCode());
        result.put("freeMinutes", pricing.getFreeMinutes());
        result.put("ratePerMin", pricing.getRatePerMin()); // grosze/minutę
        result.put("reservationFeeMinor", pricing.getReservationFeeMinor()); // grosze
        result.put("roundingStepMin", pricing.getRoundingStepMin());

        return result;
    }

    /**
     * Get pricingId by parkingId
     * Useful when frontend needs to update pricing
     */
    public Long getPricingIdByParkingId(Long parkingId) {
        Optional<ParkingPricing> pricingOpt = pricingRepo.findByParkingId(parkingId);
        return pricingOpt.map(ParkingPricing::getId).orElse(null);
    }

    /**
     * Update pricing information
     * @param pricingId ID of pricing record
     * @param freeMinutes Free minutes
     * @param ratePerMin Rate per minute in grosze (minor units)
     * @param reservationFeeMinor Reservation fee in grosze (minor units)
     * @return true if successful
     */
    public boolean updatePricing(Long pricingId, Integer freeMinutes,
                                 Integer ratePerMin, Integer reservationFeeMinor) {
        try {
            Optional<ParkingPricing> pricingOpt = pricingRepo.findById(pricingId);

            if (pricingOpt.isEmpty()) {
                return false;
            }

            ParkingPricing pricing = pricingOpt.get();
            pricing.setFreeMinutes(freeMinutes);
            pricing.setRatePerMin(ratePerMin);
            pricing.setReservationFeeMinor(reservationFeeMinor);

            pricingRepo.save(pricing);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Get session IDs by parking ID and date range
     * Used by admin-service to get sessions for financial reports
     */
    public List<Long> getSessionIdsByParkingId(Long parkingId, LocalDateTime startDate, LocalDateTime endDate) {
        return sessionRepo.findByParkingId(parkingId)
                .stream()
                .filter(session -> {
                    LocalDateTime entryTime = session.getEntryTime();
                    return entryTime != null &&
                            !entryTime.isBefore(startDate) &&
                            !entryTime.isAfter(endDate);
                })
                .map(ParkingSession::getId)
                .collect(Collectors.toList());
    }

    /**
     * Get session IDs by company ID and date range
     * Used by admin-service to get ALL sessions for a company's parkings
     */
    public List<Long> getSessionIdsByCompanyId(Long companyId, LocalDateTime startDate, LocalDateTime endDate) {
        // Get all parking IDs for this company
        List<Long> parkingIds = parkingLocationRepository.getIdsByCompanyId(companyId);

        // Get sessions for all these parkings
        return parkingIds.stream()
                .flatMap(parkingId -> sessionRepo.findByParkingId(parkingId).stream())
                .filter(session -> {
                    LocalDateTime entryTime = session.getEntryTime();
                    return entryTime != null &&
                            !entryTime.isBefore(startDate) &&
                            !entryTime.isAfter(endDate);
                })
                .map(ParkingSession::getId)
                .collect(Collectors.toList());
    }

    /**
     * Get parking ID by session ID
     * Used by admin-service to map session back to parking location
     */
    public Long getParkingIdBySessionId(Long sessionId) {
        Optional<ParkingSession> sessionOpt = sessionRepo.findById(sessionId);
        return sessionOpt.map(ParkingSession::getParkingId).orElse(null);
    }


}


