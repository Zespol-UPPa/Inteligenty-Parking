package com.smartparking.parking_service.scheduler;

import com.smartparking.parking_service.repository.ParkingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task do automatycznego wygaszania niewykorzystanych rezerwacji.
 * 
 * Rezerwacje ze statusem "Paid", które nie zostały użyte (nie ma sesji parkingowej)
 * i których valid_until minął, są automatycznie oznaczane jako "Expired".
 * 
 * Użytkownik i tak ponosi koszt rezerwacji (już zapłacone), ale miejsce jest zwalniane.
 */
@Component
public class ReservationExpirationScheduler {
    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationScheduler.class);
    
    private final ParkingRepository parkingRepo;
    
    public ReservationExpirationScheduler(ParkingRepository parkingRepo) {
        this.parkingRepo = parkingRepo;
    }
    
    /**
     * Uruchamia się co 5 minut i sprawdza niewykorzystane rezerwacje.
     * Rezerwacje ze statusem "Paid", które minęły (valid_until < now()) i nie zostały użyte,
     * są automatycznie oznaczane jako "Expired".
     */
    @Scheduled(fixedRate = 300000) // 5 minut = 300000 ms
    public void expireUnusedReservations() {
        try {
            int expiredCount = parkingRepo.expireUnusedReservations();
            
            if (expiredCount > 0) {
                log.info("Expired {} unused reservations (status changed from 'Paid' to 'Expired')", expiredCount);
            }
        } catch (Exception e) {
            log.error("Error in expireUnusedReservations scheduled task: {}", e.getMessage(), e);
        }
    }
}

