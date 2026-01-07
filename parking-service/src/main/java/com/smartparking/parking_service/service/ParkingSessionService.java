package com.smartparking.parking_service.service;

import com.smartparking.parking_service.client.CustomerVehicleClient;
import com.smartparking.parking_service.client.PaymentClient;
import com.smartparking.parking_service.messaging.ParkingPaymentEventPublisher;
import com.smartparking.parking_service.model.ParkingSession;
import com.smartparking.parking_service.model.ParkingPricing;
import com.smartparking.parking_service.repository.ParkingSessionRepository;
import com.smartparking.parking_service.repository.ParkingRepository;
import com.smartparking.parking_service.repository.ParkingPricingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

@Service
public class ParkingSessionService {
    private static final Logger log = LoggerFactory.getLogger(ParkingSessionService.class);
    
    private final ParkingSessionRepository sessionRepo;
    private final ParkingRepository parkingRepo;
    private final ParkingPricingRepository pricingRepo;
    private final CustomerVehicleClient vehicleClient;
    private final PaymentClient paymentClient;
    private final ParkingPaymentEventPublisher eventPublisher;

    public ParkingSessionService(ParkingSessionRepository sessionRepo,
                                ParkingRepository parkingRepo,
                                ParkingPricingRepository pricingRepo,
                                CustomerVehicleClient vehicleClient,
                                PaymentClient paymentClient,
                                ParkingPaymentEventPublisher eventPublisher) {
        this.sessionRepo = sessionRepo;
        this.parkingRepo = parkingRepo;
        this.pricingRepo = pricingRepo;
        this.vehicleClient = vehicleClient;
        this.paymentClient = paymentClient;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Tworzy parking_session gdy OCR wykryje wjazd
     */
    public Long createSessionOnEntry(String licencePlate, Long parkingId, Integer cameraId, Instant entryTime) {
        String normalizedPlate = licencePlate.toUpperCase().trim();
        
        // 1. Znajdź pojazd po tablicy
        Optional<Map<String, Object>> vehicleOpt = vehicleClient.findVehicleByPlate(normalizedPlate);
        if (vehicleOpt.isEmpty()) {
            throw new IllegalArgumentException("Vehicle not found for plate: " + normalizedPlate);
        }
        
        Map<String, Object> vehicleData = vehicleOpt.get();
        Long vehicleId = Long.valueOf(vehicleData.get("vehicleId").toString());
        Long accountId = vehicleData.get("accountId") != null ? 
            Long.valueOf(vehicleData.get("accountId").toString()) : null;
        
        if (accountId == null) {
            throw new IllegalStateException("Vehicle has no associated account: " + normalizedPlate);
        }
        
        // 2. Sprawdź czy istnieje aktywna rezerwacja
        Optional<Map<String, Object>> activeReservation = parkingRepo.findActiveReservation(
            accountId, parkingId, entryTime
        );
        
        Long spotId;
        Long reservationId = null;
        if (activeReservation.isPresent()) {
            // Użyj miejsca z rezerwacji
            spotId = Long.valueOf(activeReservation.get().get("spot_id").toString());
            reservationId = Long.valueOf(activeReservation.get().get("reservation_id").toString());
            // Oznacz rezerwację jako używaną
            parkingRepo.updateReservationStatus(reservationId, "Used");
        } else {
            // Wybierz losowe wolne miejsce
            Optional<Long> freeSpotOpt = parkingRepo.pickRandomFreeSpot(parkingId);
            if (freeSpotOpt.isEmpty()) {
                throw new IllegalStateException("No free spots available at parking: " + parkingId);
            }
            spotId = freeSpotOpt.get();
        }
        
        // 3. Utwórz parking_session
        ParkingSession session = new ParkingSession();
        session.setParkingId(parkingId);
        session.setSpotId(spotId);
        session.setRefVehicleId(vehicleId);
        session.setRefAccountId(accountId);
        session.setEntryTime(LocalDateTime.ofInstant(entryTime, ZoneId.systemDefault()));
        session.setExitTime(null);
        session.setPaymentStatus("Session"); // Status "Session" = aktywna sesja
        session.setPriceTotalMinor(null); // Cena będzie obliczona przy wyjeździe
        
        ParkingSession saved = sessionRepo.save(session);
        log.info("Created parking session {} for plate {} at parking {} spot {}", 
            saved.getId(), normalizedPlate, parkingId, spotId);
        
        return saved.getId();
    }

    /**
     * Przetwarza wyjazd: oblicza koszt i obciąża portfel
     */
    public PaymentResult processExit(String licencePlate, Long parkingId, Integer cameraId, Instant exitTime) {
        String normalizedPlate = licencePlate.toUpperCase().trim();
        
        // 1. Znajdź pojazd
        Optional<Map<String, Object>> vehicleOpt = vehicleClient.findVehicleByPlate(normalizedPlate);
        if (vehicleOpt.isEmpty()) {
            throw new IllegalArgumentException("Vehicle not found for plate: " + normalizedPlate);
        }
        
        Long vehicleId = Long.valueOf(vehicleOpt.get().get("vehicleId").toString());
        
        // 2. Znajdź aktywną sesję
        Optional<ParkingSession> sessionOpt = sessionRepo.findActiveSessionByVehicleAndParking(vehicleId, parkingId);
        if (sessionOpt.isEmpty()) {
            throw new IllegalStateException("No active session found for plate: " + normalizedPlate + " at parking: " + parkingId);
        }
        
        ParkingSession session = sessionOpt.get();
        LocalDateTime entryTime = session.getEntryTime();
        Instant entryInstant = entryTime.atZone(ZoneId.systemDefault()).toInstant();
        
        // 3. Oblicz czas postoju w minutach
        long durationMinutes = Duration.between(entryInstant, exitTime).toMinutes();
        
        // 4. Pobierz cennik
        Optional<ParkingPricing> pricingOpt = pricingRepo.findByParkingId(parkingId);
        if (pricingOpt.isEmpty()) {
            throw new IllegalStateException("No pricing found for parking: " + parkingId);
        }
        
        ParkingPricing pricing = pricingOpt.get();
        int ratePerMin = pricing.getRatePerMin(); // w groszach
        int freeMinutes = pricing.getFreeMinutes();
        int roundingStep = pricing.getRoundingStepMin();
        
        // 5. Oblicz minuty płatne
        int billableMinutes = 0;
        if (durationMinutes > freeMinutes) {
            billableMinutes = (int) durationMinutes - freeMinutes;
            // Zaokrąglenie w górę do skoku roundingStep
            billableMinutes = (int) Math.ceil((double) billableMinutes / roundingStep) * roundingStep;
        }
        
        // 6. Oblicz cenę w groszach
        long totalPriceMinor = (long) billableMinutes * ratePerMin;
        
        // 7. Obciąż portfel przez payment-service
        PaymentResult paymentResult;
        if (totalPriceMinor > 0) {
            paymentResult = paymentClient.chargeForParkingSession(
                session.getRefAccountId(),
                session.getId(),
                totalPriceMinor
            );
        } else {
            // Darmowy parking (w ramach free minutes)
            paymentResult = PaymentResult.success(session.getId(), 0L);
        }
        
        // 8. Zaktualizuj sesję
        session.setExitTime(LocalDateTime.ofInstant(exitTime, ZoneId.systemDefault()));
        session.setPriceTotalMinor(new BigDecimal(totalPriceMinor));
        session.setPaymentStatus(paymentResult.isSuccess() ? "Paid" : "Pending");
        sessionRepo.save(session);
        
        // 9. Wyślij email z potwierdzeniem płatności (jeśli płatność się powiodła)
        if (paymentResult.isSuccess() && totalPriceMinor > 0) {
            eventPublisher.publishParkingPaymentConfirmation(
                session.getRefAccountId(),
                session.getId(),
                entryInstant,
                exitTime,
                totalPriceMinor,
                durationMinutes
            );
        }
        
        log.info("Processed exit for plate {}: duration={}min, price={}, payment={}", 
            normalizedPlate, durationMinutes, totalPriceMinor, paymentResult.isSuccess() ? "success" : "failed");
        
        return paymentResult;
    }

    public static class PaymentResult {
        private final boolean success;
        private final Long sessionId;
        private final Long amountMinor;
        private final String errorMessage;

        private PaymentResult(boolean success, Long sessionId, Long amountMinor, String errorMessage) {
            this.success = success;
            this.sessionId = sessionId;
            this.amountMinor = amountMinor;
            this.errorMessage = errorMessage;
        }

        public static PaymentResult success(Long sessionId, Long amountMinor) {
            return new PaymentResult(true, sessionId, amountMinor, null);
        }

        public static PaymentResult failed(String errorMessage) {
            return new PaymentResult(false, null, null, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public Long getSessionId() {
            return sessionId;
        }

        public Long getAmountMinor() {
            return amountMinor;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

