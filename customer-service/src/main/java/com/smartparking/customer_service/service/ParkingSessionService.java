package com.smartparking.customer_service.service;

import com.smartparking.customer_service.client.ParkingReservationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
public class ParkingSessionService {
    private static final Logger log = LoggerFactory.getLogger(ParkingSessionService.class);
    private final ParkingReservationClient parkingClient;
    private final VehicleService vehicleService;
    
    public ParkingSessionService(ParkingReservationClient parkingClient, VehicleService vehicleService) {
        this.parkingClient = parkingClient;
        this.vehicleService = vehicleService;
    }
    
    public Optional<Map<String, Object>> getActiveSession(Long accountId) {
        // Pobierz aktywną sesję z parking-service
        Optional<Map<String, Object>> sessionOpt = parkingClient.getActiveSession(accountId);
        if (sessionOpt.isEmpty()) {
            log.debug("No active session found for accountId={}", accountId);
            return Optional.empty();
        }
        
        Map<String, Object> session = sessionOpt.get();
        
        // Pobierz vehicle_id z sesji
        Object vehicleIdObj = session.get("vehicle_id");
        if (vehicleIdObj != null) {
            try {
                Long vehicleId = ((Number) vehicleIdObj).longValue();
                
                // Pobierz pojazdy użytkownika i znajdź pasujący vehicle
                vehicleService.list(accountId).stream()
                    .filter(v -> {
                        Object idObj = v.get("id");
                        if (idObj == null) return false;
                        try {
                            return Long.parseLong(idObj.toString()) == vehicleId;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    })
                    .findFirst()
                    .ifPresent(vehicle -> {
                        Object plateObj = vehicle.get("licencePlate");
                        if (plateObj != null) {
                            session.put("vehicle_plate", plateObj.toString());
                            log.info("Found vehicle_plate={} for active session {} with vehicle_id={}", 
                                    plateObj.toString(), session.get("session_id"), vehicleId);
                        }
                    });
            } catch (Exception e) {
                log.warn("Failed to get vehicle plate for active session: {}", e.getMessage());
            }
        }
        
        // Oblicz czas trwania sesji (w minutach)
        Object entryTimeObj = session.get("entry_time");
        if (entryTimeObj != null) {
            try {
                Instant entryTime;
                if (entryTimeObj instanceof Instant) {
                    entryTime = (Instant) entryTimeObj;
                } else if (entryTimeObj instanceof String) {
                    entryTime = Instant.parse((String) entryTimeObj);
                } else {
                    entryTime = Instant.now(); // Fallback
                }
                
                Instant now = Instant.now();
                long durationMinutes = (now.toEpochMilli() - entryTime.toEpochMilli()) / 60000;
                session.put("duration_minutes", durationMinutes);
                session.put("duration_hours", durationMinutes / 60.0);
                
                // Oblicz szacunkowy koszt (jeśli nie ma price_total_minor)
                // TODO: Pobierz pełny cennik (rate_per_min, free_minutes, rounding_step_min) z parking-service i oblicz koszt
                // Formuła powinna być: max(0, ceil((durationMinutes - free_minutes) / rounding_step_min) * rounding_step_min * rate_per_min)
                // Na razie ustawiamy 0 jako placeholder - koszt będzie obliczany przy wyjeździe przez parking-service
                if (session.get("price_total_minor") == null) {
                    session.put("estimated_cost_minor", 0);
                }
            } catch (Exception e) {
                log.warn("Failed to calculate session duration: {}", e.getMessage());
            }
        }
        
        return Optional.of(session);
    }
    
    /**
     * Pobiera wszystkie sesje parkingowe dla danego konta użytkownika
     */
    public java.util.List<Map<String, Object>> getSessions(Long accountId) {
        return parkingClient.getSessions(accountId, false); // Wszystkie sesje
    }

    /**
     * Pobiera tylko niezapłacone sesje parkingowe dla danego konta użytkownika
     */
    public java.util.List<Map<String, Object>> getUnpaidSessions(Long accountId) {
        return parkingClient.getUnpaidSessions(accountId);
    }

    /**
     * Opłaca zakończoną sesję parkingową (exit_time != NULL, status Pending/Unpaid)
     * Sprawdza czy sesja należy do użytkownika przed wykonaniem płatności.
     * 
     * @param accountId ID konta użytkownika
     * @param sessionId ID sesji do opłacenia
     * @return ResponseEntity z informacją o płatności
     */
    public org.springframework.http.ResponseEntity<Map<String, Object>> payForSession(Long accountId, Long sessionId) {
        // Sprawdź czy sesja należy do użytkownika (poprzez listę sesji)
        java.util.List<Map<String, Object>> sessions = getSessions(accountId);
        boolean belongsToUser = sessions.stream()
            .anyMatch(s -> {
                Object idObj = s.get("session_id");
                return idObj != null && idObj.toString().equals(sessionId.toString());
            });
        
        if (!belongsToUser) {
            throw new IllegalArgumentException("Session not found or does not belong to the account.");
        }
        
        // Wywołaj płatność przez parking-service
        return parkingClient.payForSession(sessionId);
    }

    /**
     * Pobiera statystyki sesji parkingowych dla danego konta użytkownika
     * @param accountId ID konta użytkownika
     * @return Map ze statystykami: totalSessions, totalHours, totalSpent
     */
    public Map<String, Object> getSessionStatistics(Long accountId) {
        return parkingClient.getSessionStatistics(accountId);
    }
}

