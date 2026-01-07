package com.smartparking.parking_service.repository;

import com.smartparking.parking_service.model.ParkingSession;

import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository {

    Optional<ParkingSession> findById(Long id);

    List<ParkingSession> findByAccountId(Long accountId);

    List<ParkingSession> findByVehicleId(Long vehicleId);

    List<ParkingSession> findByParkingId(Long parkingId);

    List<ParkingSession> findAll();

    public List<ParkingSession> findActiveSession();

    Optional<ParkingSession> findActiveSessionByVehicleAndParking(Long vehicleId, Long parkingId);

    Long countActiveSessionsByParkingId(Long parkingId);

    ParkingSession save(ParkingSession session);

}
