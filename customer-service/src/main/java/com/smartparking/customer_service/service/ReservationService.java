package com.smartparking.customer_service.service;

import com.smartparking.customer_service.client.ParkingReservationClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ReservationService {
    private final ParkingReservationClient parkingClient;
    
    public ReservationService(ParkingReservationClient parkingClient) {
        this.parkingClient = parkingClient;
    }
    
    public List<Map<String, Object>> list(Long accountId) {
        return parkingClient.getReservationsByAccountId(accountId);
    }
    
    public long create(Long accountId, Long parkingId, Long spotId, Instant start, Instant end, String status) {
        // parking_db.reservation_spot uses valid_until (end time), not start_time
        return parkingClient.createReservation(accountId, parkingId, spotId, end, status);
    }
}

