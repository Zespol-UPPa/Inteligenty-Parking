package com.smartparking.parking_service.repository;
import com.smartparking.parking_service.model.ParkingSpot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ParkingSpotRepository {
    Optional<ParkingSpot> findById(Long id);

    List<ParkingSpot> findByParkingId(Long parkingId);

    List<ParkingSpot> findAll();

    Long countAllSpotsByParkingId(Long parkingId);

    ParkingSpot save(ParkingSpot spot);
}
