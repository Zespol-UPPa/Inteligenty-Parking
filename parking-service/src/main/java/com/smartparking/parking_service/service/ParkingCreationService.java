package com.smartparking.parking_service.service;

import com.smartparking.parking_service.dto.AddParkingRequest;
import com.smartparking.parking_service.model.ParkingLocation;
import com.smartparking.parking_service.model.ParkingPricing;
import com.smartparking.parking_service.model.ParkingSpot;
import com.smartparking.parking_service.repository.ParkingLocationRepository;
import com.smartparking.parking_service.repository.ParkingPricingRepository;
import com.smartparking.parking_service.repository.ParkingSpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ParkingCreationService {

    private static final Logger log = LoggerFactory.getLogger(ParkingCreationService.class);

    private final ParkingLocationRepository locationRepo;
    private final ParkingSpotRepository spotRepo;
    private final ParkingPricingRepository pricingRepo;

    public ParkingCreationService(
            ParkingLocationRepository locationRepo,
            ParkingSpotRepository spotRepo,
            ParkingPricingRepository pricingRepo) {
        this.locationRepo = locationRepo;
        this.spotRepo = spotRepo;
        this.pricingRepo = pricingRepo;
    }

    /**
     * Create a new parking with sections and spots
     *
     * Process:
     * 1. Create parking_location
     * 2. Create default parking_pricing
     * 3. For each section, create parking_spots with codes (e.g. A1, A2, ...)
     *
     * @return parkingId
     */
    @Transactional
    public Long createParkingWithSections(AddParkingRequest request) {
        log.info("Creating parking: {} with {} sections",
                request.getName(),
                request.getSections() != null ? request.getSections().size() : 0);

        // Step 1: Create parking location
        ParkingLocation location = new ParkingLocation();
        location.setNameParking(request.getName());
        location.setAddressLine(request.getAddress());
        location.setRefCompanyId(request.getCompanyId());

        location = locationRepo.save(location);
        Long parkingId = location.getId();

        log.info("Created parking_location with id: {}", parkingId);

        // Step 2: Create default pricing
        createDefaultPricing(parkingId);

        // Step 3: Create spots for each section
        if (request.getSections() != null && !request.getSections().isEmpty()) {
            int totalSpots = 0;

            for (AddParkingRequest.SectionDto section : request.getSections()) {
                int spotsCreated = createSpotsForSection(
                        parkingId,
                        section.getPrefix(),
                        section.getNumberOfSpots(),
                        section.getFloorLevel(),
                        section.getReservable()
                );
                totalSpots += spotsCreated;
            }

            log.info("Created {} total spots for parking {}", totalSpots, parkingId);
        } else {
            log.warn("No sections provided for parking {}", parkingId);
        }

        return parkingId;
    }

    /**
     * Create default pricing for parking
     * Admin will need to update this later
     */
    private void createDefaultPricing(Long parkingId) {
        ParkingPricing pricing = new ParkingPricing();
        pricing.setParkingId(parkingId);
        pricing.setCurrencyCode("PLN");
        pricing.setFreeMinutes(15);           // 15 free minutes
        pricing.setRatePerMin(10);            // 10 groszy/min = 0.10 PLN/min
        pricing.setRoundingStepMin(1);        // Round to 1 minute
        pricing.setReservationFeeMinor(500);  // 500 groszy = 5.00 PLN

        pricingRepo.save(pricing);
        log.info("Created default pricing for parking {}", parkingId);
    }

    /**
     * Create parking spots for a section
     * Generates codes like: A1, A2, A3, ..., A23
     *
     * @return number of spots created
     */
    private int createSpotsForSection(
            Long parkingId,
            String prefix,
            Integer numberOfSpots,
            Integer floorLevel,
            Boolean reservable) {

        if (numberOfSpots == null || numberOfSpots <= 0) {
            log.warn("Invalid numberOfSpots: {}", numberOfSpots);
            return 0;
        }

        log.info("Creating {} spots for section {} on floor {} (reservable: {})",
                numberOfSpots, prefix, floorLevel, reservable);

        List<ParkingSpot> spots = new ArrayList<>();

        for (int i = 1; i <= numberOfSpots; i++) {
            ParkingSpot spot = new ParkingSpot();
            spot.setParkingId(parkingId);
            spot.setCode(prefix + i);              // A1, A2, A3, ...
            spot.setFloorLvl(floorLevel != null ? floorLevel : 0);
            spot.setToReserved(reservable != null ? reservable : false);
            spot.setType("Available");             // Default type

            spots.add(spot);
        }

        // Batch save all spots
        for (ParkingSpot spot : spots) {
            spotRepo.save(spot);
        }

        log.info("Created {} spots for section {}", spots.size(), prefix);
        return spots.size();
    }
}