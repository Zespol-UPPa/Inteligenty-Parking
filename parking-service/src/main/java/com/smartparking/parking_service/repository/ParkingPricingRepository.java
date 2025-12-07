package com.smartparking.parking_service.repository;

import com.smartparking.parking_service.model.ParkingPricing;

import java.util.List;
import java.util.Optional;

public interface ParkingPricingRepository {

    Optional<ParkingPricing> findById(Long id);

    Optional<ParkingPricing> findByParkingId(Long parkingId);

    List<ParkingPricing> findAll();

    ParkingPricing save(ParkingPricing pricing);
}