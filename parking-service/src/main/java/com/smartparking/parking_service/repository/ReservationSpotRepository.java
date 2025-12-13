package com.smartparking.parking_service.repository;

import com.smartparking.parking_service.model.ReservationSpot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationSpotRepository {

    Optional<ReservationSpot> findById(Long id);

    List<ReservationSpot> findByAccountId(Long accountId);

    List<ReservationSpot> findByParkingId(Long parkingId);

    List<ReservationSpot> findBySpotId(Long spotId);

    List<ReservationSpot> findActiveByAccountId(Long accountId, LocalDateTime now);

    List<ReservationSpot> findAll();

    ReservationSpot save(ReservationSpot reservation);
}
