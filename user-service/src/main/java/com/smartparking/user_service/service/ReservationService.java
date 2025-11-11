package com.smartparking.user_service.service;

import com.smartparking.user_service.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ReservationService {
    private final ReservationRepository reservations;
    public ReservationService(ReservationRepository reservations) {
        this.reservations = reservations;
    }
    public List<Map<String, Object>> list(Long accountId) {
        return reservations.findByAccountId(accountId);
    }
    public long create(Long accountId, Long parkingId, Long spotId, Instant start, Instant end, String status) {
        return reservations.create(accountId, parkingId, spotId, start, end, status);
    }
}


