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
import java.time.temporal.ChronoUnit;
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
     * Obsługuje zarówno zarejestrowanych jak i niezarejestrowanych klientów
     */
    public Long createSessionOnEntry(String licencePlate, Long parkingId, Integer cameraId, Instant entryTime) {
        String normalizedPlate = licencePlate.toUpperCase().trim();
        
        // 1. Znajdź lub utwórz pojazd (dla niezarejestrowanych tworzy nowy pojazd bez customer_id)
        Map<String, Object> vehicleData;
        try {
            vehicleData = vehicleClient.createOrGetVehicle(normalizedPlate);
        } catch (Exception e) {
            log.error("Failed to create or get vehicle for plate: {}", normalizedPlate, e);
            throw new IllegalStateException("Failed to create or get vehicle: " + e.getMessage());
        }
        
        Long vehicleId = Long.valueOf(vehicleData.get("vehicleId").toString());
        Object accountIdObj = vehicleData.get("accountId");
        Long accountId = null;
        if (accountIdObj != null && !accountIdObj.toString().isEmpty()) {
            try {
                accountId = Long.valueOf(accountIdObj.toString());
            } catch (NumberFormatException e) {
                log.warn("Invalid accountId format for vehicle {}: {}", vehicleId, accountIdObj);
            }
        }
        
        boolean isRegistered = accountId != null;
        log.info("Processing entry: plate={}, vehicleId={}, accountId={}, isRegistered={}", 
            normalizedPlate, vehicleId, accountId, isRegistered);
        
        // 2. SPRAWDZENIE DUPLIKATÓW - czy już istnieje aktywna sesja dla tego pojazdu w tym parkingu
        Optional<ParkingSession> existingSessionOpt = sessionRepo.findActiveSessionByVehicleAndParking(vehicleId, parkingId);
        if (existingSessionOpt.isPresent()) {
            ParkingSession existingSession = existingSessionOpt.get();
            log.warn("DUPLICATE ENTRY DETECTED: Active session already exists for plate {} (vehicleId={}) at parking {} - sessionId={}, entryTime={}, paymentStatus={}, reservationId={}", 
                normalizedPlate, vehicleId, parkingId, existingSession.getId(), existingSession.getEntryTime(), 
                existingSession.getPaymentStatus(), existingSession.getReservationId());
            log.info("Ignoring duplicate entry event - returning existing sessionId={}", existingSession.getId());
            return existingSession.getId(); // Zwróć ID istniejącej sesji zamiast tworzyć nową
        } else {
            log.debug("No existing active session found for vehicleId={}, parkingId={} - proceeding with new session creation", 
                vehicleId, parkingId);
        }
        
        // 3. Sprawdź czy istnieje aktywna, nieużyta rezerwacja (tylko dla zarejestrowanych)
        Long spotId;
        Long reservationId = null;
        
        if (isRegistered) {
            log.info("Checking for active reservation: accountId={}, parkingId={}, vehicleId={}, entryTime={}", 
                accountId, parkingId, vehicleId, entryTime);
            
            Optional<Map<String, Object>> activeReservation = parkingRepo.findActiveReservation(
                accountId, parkingId, vehicleId, entryTime
            );
            
            if (activeReservation.isEmpty()) {
                log.warn("No active reservation found, using random spot: accountId={}, parkingId={}, vehicleId={}, entryTime={}", 
                    accountId, parkingId, vehicleId, entryTime);
            }
            
            if (activeReservation.isPresent()) {
                // Użyj miejsca z rezerwacji
                spotId = Long.valueOf(activeReservation.get().get("spot_id").toString());
                reservationId = Long.valueOf(activeReservation.get().get("reservation_id").toString());
                
                // Dodatkowa ochrona - sprawdź jeszcze raz status przed użyciem (race condition)
                // Jeśli status już nie jest 'Paid' ani 'Active', ktoś inny właśnie użył tej rezerwacji
                Optional<Map<String, Object>> reservationCheck = parkingRepo.findReservationById(reservationId);
                String currentStatus = reservationCheck.map(r -> (String) r.get("status_reservation")).orElse(null);
                if (reservationCheck.isEmpty() || (!"Paid".equals(currentStatus) && !"Active".equals(currentStatus))) {
                    log.warn("Reservation {} status changed during processing (current: {}) - falling back to random spot", 
                        reservationId, currentStatus);
                    Optional<Long> freeSpotOpt = parkingRepo.pickRandomFreeSpot(parkingId);
                    if (freeSpotOpt.isEmpty()) {
                        throw new IllegalStateException("No free spots available at parking: " + parkingId);
                    }
                    spotId = freeSpotOpt.get();
                    reservationId = null;
                } else {
                    // Zmień status rezerwacji na "Active" (w trakcie) - tylko jeśli nadal jest 'Paid' lub 'Active'
                    if ("Paid".equals(currentStatus)) {
                        parkingRepo.updateReservationStatus(reservationId, "Active");
                        log.info("Using reserved spot {} for vehicle {} (reservation {} - status changed from Paid to Active)", 
                            spotId, vehicleId, reservationId);
                    } else {
                        log.info("Using reserved spot {} for vehicle {} (reservation {} - already Active, reusing)", 
                            spotId, vehicleId, reservationId);
                    }
                }
            } else {
                Optional<Long> freeSpotOpt = parkingRepo.pickRandomFreeSpot(parkingId);
                if (freeSpotOpt.isEmpty()) {
                    throw new IllegalStateException("No free spots available at parking: " + parkingId);
                }
                spotId = freeSpotOpt.get();
            }
        } else {
            Optional<Long> freeSpotOpt = parkingRepo.pickRandomFreeSpot(parkingId);
            if (freeSpotOpt.isEmpty()) {
                throw new IllegalStateException("No free spots available at parking: " + parkingId);
            }
            spotId = freeSpotOpt.get();
            log.info("Unregistered vehicle {} assigned to spot {}", vehicleId, spotId);
        }
        
        // 4. Utwórz parking_session
        Instant sessionEntryTime = entryTime;
        if (reservationId != null) {
            Optional<Map<String, Object>> reservationOpt = parkingRepo.findReservationById(reservationId);
            if (reservationOpt.isPresent()) {
                Instant validFrom = (Instant) reservationOpt.get().get("valid_from");
                sessionEntryTime = validFrom;
                log.info("Using reservation valid_from as entry_time: reservationId={}, validFrom={}, actualEntryTime={}", 
                    reservationId, validFrom, entryTime);
            }
        }
        
        ParkingSession session = new ParkingSession();
        session.setParkingId(parkingId);
        session.setSpotId(spotId);
        session.setRefVehicleId(vehicleId); // ZAWSZE ustaw (NOT NULL)
        session.setRefAccountId(accountId);  // NULL dla niezarejestrowanych
        session.setReservationId(reservationId);  // NULL jeśli nie z rezerwacji
        session.setEntryTime(LocalDateTime.ofInstant(sessionEntryTime, ZoneId.of("UTC")));
        session.setExitTime(null);
        session.setPaymentStatus("Session");
        session.setPriceTotalMinor(null);
        
        ParkingSession saved = sessionRepo.save(session);
        log.info("Parking session created: plate={}, sessionId={}, vehicleId={}, accountId={}, parking={}, spot={}, reservationId={}", 
            normalizedPlate, saved.getId(), vehicleId, accountId != null ? accountId : "NULL", parkingId, spotId, 
            reservationId != null ? reservationId : "NULL");
        
        return saved.getId();
    }

    /**
     * Opłaca zakończoną sesję parkingową (exit_time != NULL, status Unpaid).
     * Pobiera płatność z portfela i zmienia status na "Paid".
     * 
     * @param sessionId ID sesji do opłacenia
     * @return PaymentResult z informacją o płatności
     */
    public PaymentResult payForSession(Long sessionId) {
        // 1. Znajdź sesję
        Optional<ParkingSession> sessionOpt = sessionRepo.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        
        ParkingSession session = sessionOpt.get();
        
        // 2. Sprawdź czy sesja jest zakończona (ma exit_time)
        if (session.getExitTime() == null) {
            throw new IllegalStateException("Cannot pay for active session: " + sessionId);
        }
        
        // 3. Sprawdź czy sesja już opłacona
        if ("Paid".equals(session.getPaymentStatus())) {
            log.info("Session already paid: sessionId={}", sessionId);
            return PaymentResult.success(sessionId, 
                session.getPriceTotalMinor() != null ? session.getPriceTotalMinor().longValue() : 0L);
        }
        
        // 4. Sprawdź czy sesja ma cenę
        if (session.getPriceTotalMinor() == null || session.getPriceTotalMinor().longValue() <= 0) {
            // Darmowy parking - automatycznie oznacz jako opłacone
            session.setPaymentStatus("Paid");
            sessionRepo.save(session);
            return PaymentResult.success(sessionId, 0L);
        }
        
        Long accountId = session.getRefAccountId();
        if (accountId == null) {
            throw new IllegalStateException("Cannot pay unregistered vehicle session: " + sessionId);
        }
        
        // 5. Pobierz płatność z portfela
        long totalPriceMinor = session.getPriceTotalMinor().longValue();
        PaymentResult paymentResult = paymentClient.chargeForParkingSession(
            accountId,
            sessionId,
            totalPriceMinor
        );
        
        // 6. Aktualizuj status sesji
        if (paymentResult.isSuccess()) {
            session.setPaymentStatus("Paid");
            sessionRepo.save(session);
            
            // 7. Wyślij email z potwierdzeniem płatności
            Instant entryInstant = session.getEntryTime().atZone(ZoneId.of("UTC")).toInstant();
            Instant exitInstant = session.getExitTime().atZone(ZoneId.of("UTC")).toInstant();
            long durationMinutes = Duration.between(entryInstant, exitInstant).toMinutes();
            
            eventPublisher.publishParkingPaymentConfirmation(
                accountId,
                sessionId,
                entryInstant,
                exitInstant,
                totalPriceMinor,
                durationMinutes
            );
            
            log.info("Session paid successfully: sessionId={}, accountId={}, amount={}", 
                sessionId, accountId, totalPriceMinor);
        } else {
            session.setPaymentStatus("Unpaid"); // Niezapłacone - użytkownik musi opłacić później
            sessionRepo.save(session);
            log.warn("Payment failed for session: sessionId={}, accountId={}, error={}", 
                sessionId, accountId, paymentResult.getErrorMessage());
        }
        
        return paymentResult;
    }
    
    /**
     * Przetwarza wyjazd: oblicza koszt i kończy sesję (ustawia exit_time).
     * NIE pobiera płatności automatycznie - użytkownik musi opłacić sesję później w aplikacji.
     */
    public PaymentResult processExit(String licencePlate, Long parkingId, Integer cameraId, Instant exitTime) {
        String normalizedPlate = licencePlate.toUpperCase().trim();
        
        // 1. Znajdź pojazd
        Optional<Map<String, Object>> vehicleOpt = vehicleClient.findVehicleByPlate(normalizedPlate);
        if (vehicleOpt.isEmpty()) {
            throw new IllegalArgumentException("Vehicle not found for plate: " + normalizedPlate);
        }
        
        Long vehicleId = Long.valueOf(vehicleOpt.get().get("vehicleId").toString());
        Object accountIdObj = vehicleOpt.get().get("accountId");
        Long accountId = null;
        if (accountIdObj != null && !accountIdObj.toString().isEmpty()) {
            try {
                accountId = Long.valueOf(accountIdObj.toString());
            } catch (NumberFormatException e) {
                log.warn("Invalid accountId format for vehicle {}: {}", vehicleId, accountIdObj);
            }
        }
        
        // 2. Znajdź aktywną sesję
        Optional<ParkingSession> sessionOpt = sessionRepo.findActiveSessionByVehicleAndParking(vehicleId, parkingId);
        
        ParkingSession session;
        if (sessionOpt.isEmpty()) {
            // 2a. Nie ma sesji - sprawdź czy istnieje aktywna rezerwacja (backfill scenario)
            if (accountId != null) {
                log.warn("No active session found for plate {} (vehicleId={}) at parking {}, checking for active reservation", 
                    normalizedPlate, vehicleId, parkingId);
                
                Optional<Map<String, Object>> activeReservationOpt = parkingRepo.findActiveReservation(
                    accountId, parkingId, vehicleId, exitTime
                );
                
                if (activeReservationOpt.isPresent()) {
                    Map<String, Object> reservation = activeReservationOpt.get();
                    Long reservationId = Long.valueOf(reservation.get("reservation_id").toString());
                    Long spotId = Long.valueOf(reservation.get("spot_id").toString());
                    Instant validFrom = (Instant) reservation.get("valid_from");
                    String reservationStatus = (String) reservation.get("status");
                    
                    log.info("Found active reservation {} (status={}) but no session, creating backfill session with entry_time={}", 
                        reservationId, reservationStatus, validFrom);
                    
                    // Utwórz sesję "wstecz" (backfill) z entry_time = valid_from rezerwacji
                    session = new ParkingSession();
                    session.setParkingId(parkingId);
                    session.setSpotId(spotId);
                    session.setRefVehicleId(vehicleId);
                    session.setRefAccountId(accountId);
                    session.setReservationId(reservationId);
                    session.setEntryTime(LocalDateTime.ofInstant(validFrom, ZoneId.of("UTC")));
                    session.setExitTime(null);
                    session.setPaymentStatus("Session");
                    session.setPriceTotalMinor(null);
                    
                    session = sessionRepo.save(session);
                    log.info("Created backfill session: sessionId={}, reservationId={}, entryTime={}", 
                        session.getId(), reservationId, validFrom);
                } else {
                    throw new IllegalStateException("No active session found for plate: " + normalizedPlate + " at parking: " + parkingId);
                }
            } else {
                throw new IllegalStateException("No active session found for plate: " + normalizedPlate + " at parking: " + parkingId);
            }
        } else {
            session = sessionOpt.get();
        }
        
        LocalDateTime entryTime = session.getEntryTime();
        Instant entryInstant = entryTime.atZone(ZoneId.of("UTC")).toInstant();
        
        // 3. Sprawdź czy sesja powstała z rezerwacji
        Long reservationId = session.getReservationId();
        boolean isFromReservation = reservationId != null;
        
        long totalPriceMinor = 0L;
        long durationMinutes = 0L;
        
        if (isFromReservation) {
            // SESJA Z REZERWACJI - płatność już była pobrana z góry przy rezerwacji
            Optional<Map<String, Object>> reservationOpt = parkingRepo.findReservationById(reservationId);
            
            if (reservationOpt.isPresent()) {
                Map<String, Object> reservation = reservationOpt.get();
                String reservationStatus = (String) reservation.get("status_reservation");
                
                if ("Active".equals(reservationStatus) || "Paid".equals(reservationStatus)) {
                    // Pobierz okres rezerwacji
                    Instant validFrom = (Instant) reservation.get("valid_from");
                    Instant validUntil = (Instant) reservation.get("valid_until");
                    
                    // Oblicz czas trwania rezerwacji (nie faktyczny czas postoju)
                    durationMinutes = Duration.between(validFrom, validUntil).toMinutes();
                    
                    // Pobierz cennik do obliczenia rzeczywistej ceny rezerwacji (dla historii)
                    Optional<ParkingPricing> pricingOpt = pricingRepo.findByParkingId(parkingId);
                    if (pricingOpt.isEmpty()) {
                        throw new IllegalStateException("No pricing found for parking: " + parkingId);
                    }
                    
                    ParkingPricing pricing = pricingOpt.get();
                    int ratePerMin = pricing.getRatePerMin(); // w groszach
                    
                    // Oblicz cenę za całą rezerwację (już zapłacone, ale zapisz dla historii)
                    // BEZ darmowych 15 minut dla rezerwacji
                    long reservationPriceMinor = durationMinutes * ratePerMin;
                    
                    // Sprawdź grace period (15 minut po valid_until) - jeśli wyjazd po tym czasie, nalicz karę
                    Instant gracePeriodEnd = validUntil.plus(15, ChronoUnit.MINUTES);
                    long penaltyMinor = 0L;
                    
                    if (exitTime.isAfter(gracePeriodEnd)) {
                        // Nalicz karę za przekroczenie grace period
                        long overtimeMinutes = Duration.between(gracePeriodEnd, exitTime).toMinutes();
                        penaltyMinor = overtimeMinutes * ratePerMin;
                        log.warn("Reservation {} exceeded grace period: exitTime={}, gracePeriodEnd={}, overtime={}min, penalty={}",
                            reservationId, exitTime, gracePeriodEnd, overtimeMinutes, penaltyMinor);
                    }
                    
                    // Całkowita cena = cena rezerwacji + kara (jeśli przekroczono grace period)
                    totalPriceMinor = reservationPriceMinor + penaltyMinor;
                    
                    // Jeśli jest kara, pobierz ją z portfela
                    if (penaltyMinor > 0 && accountId != null) {
                        try {
                            PaymentResult penaltyResult = paymentClient.chargeForParkingSession(
                                accountId, session.getId(), penaltyMinor
                            );
                            if (!penaltyResult.isSuccess()) {
                                log.warn("Failed to charge penalty {} for reservation {}: {}", 
                                    penaltyMinor, reservationId, penaltyResult.getErrorMessage());
                                // Kontynuuj mimo błędu - kara może być pobrana później
                            } else {
                                log.info("Penalty {} charged for reservation {} (exceeded grace period)", 
                                    penaltyMinor, reservationId);
                            }
                        } catch (Exception e) {
                            log.error("Exception charging penalty for reservation {}: {}", reservationId, e.getMessage(), e);
                            // Kontynuuj mimo błędu
                        }
                    }
                    
                    // Zmień status rezerwacji na "Expired" (zakończona)
                    parkingRepo.updateReservationStatus(reservationId, "Expired");
                    
                    // Zapisz rzeczywistą cenę rezerwacji (dla historii) - już zapłacone przy rezerwacji
                    session.setPaymentStatus("Paid"); // Już zapłacone przy rezerwacji (kara pobrana osobno jeśli była)
                    session.setPriceTotalMinor(new BigDecimal(totalPriceMinor)); // Zapisz całkowitą cenę (rezerwacja + kara)
                    
                    // Dla rezerwacji: jeśli wyjechaliśmy w czasie rezerwacji, exit_time = valid_until
                    // Jeśli wyjechaliśmy po rezerwacji, exit_time = faktyczny czas wyjazdu
                    Instant sessionExitTime = exitTime.isBefore(validUntil) || exitTime.equals(validUntil) 
                        ? validUntil 
                        : exitTime;
                    session.setExitTime(LocalDateTime.ofInstant(sessionExitTime, ZoneId.of("UTC")));
                    sessionRepo.save(session);
                    
                    log.info("Exit from reservation: sessionId={}, reservationId={}, plate={}, " +
                            "reservationDuration={}min (from {} to {}), reservationPrice={}, penalty={}, totalPrice={}, status=Paid",
                        session.getId(), reservationId, normalizedPlate, durationMinutes, validFrom, validUntil,
                        reservationPriceMinor, penaltyMinor, totalPriceMinor);
                    
                    // Wyślij email z potwierdzeniem (sesja z rezerwacji - już zapłacona)
                    if (accountId != null) {
                        eventPublisher.publishParkingPaymentConfirmation(
                            accountId,
                            session.getId(),
                            entryInstant,
                            exitTime,
                            totalPriceMinor,
                            durationMinutes
                        );
                    }
                    
                    return PaymentResult.success(session.getId(), totalPriceMinor);
                } else {
                    log.warn("Reservation {} has unexpected status '{}' - treating as normal session", 
                        reservationId, reservationStatus);
                    // Fallback: traktuj jako normalną sesję
                    isFromReservation = false;
                }
            } else {
                log.warn("Reservation {} not found for sessionId={} - treating as normal session", 
                    reservationId, session.getId());
                // Fallback: traktuj jako normalną sesję
                isFromReservation = false;
            }
        }
        
        // 4. NORMALNA SESJA (nie z rezerwacji) - oblicz cenę na podstawie faktycznego czasu postoju
        if (!isFromReservation) {
            durationMinutes = Duration.between(entryInstant, exitTime).toMinutes();
            
            // 4a. Pobierz cennik
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
            totalPriceMinor = (long) billableMinutes * ratePerMin;
            
            // 7. NIE pobieramy płatności automatycznie - tylko ustawiamy status
            Long sessionAccountId = session.getRefAccountId();
            
            if (sessionAccountId != null) {
                // ZAREJESTROWANY - status "Unpaid" (użytkownik musi opłacić w aplikacji)
                session.setPaymentStatus("Unpaid");
                log.info("Exit processed: sessionId={}, plate={}, duration={}min, price={}, status=Unpaid (payment required)", 
                    session.getId(), normalizedPlate, durationMinutes, totalPriceMinor);
            } else {
                // NIEZAREJESTROWANY - status "Unpaid", płatność przy wyjeździe (przez terminal/kasę)
                session.setPaymentStatus("Unpaid");
                log.info("Unregistered vehicle exit: sessionId={}, plate={}, price={}, status=Unpaid", 
                    session.getId(), normalizedPlate, totalPriceMinor);
            }
            
            // 8. Zaktualizuj sesję - exit_time, cena i status
            session.setExitTime(LocalDateTime.ofInstant(exitTime, ZoneId.of("UTC")));
            session.setPriceTotalMinor(new BigDecimal(totalPriceMinor));
            sessionRepo.save(session);
            
            // 9. NIE wysyłamy emaila - płatność jeszcze nie wykonana
            // Email zostanie wysłany po opłaceniu sesji przez użytkownika (metoda payForSession)
            
            return PaymentResult.unpaid(session.getId(), totalPriceMinor);
        }
        
        // Ten return nie powinien być osiągnięty, ale na wszelki wypadek
        return PaymentResult.unpaid(session.getId(), totalPriceMinor);
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
        
        public static PaymentResult unpaid(Long sessionId, Long amountMinor) {
            return new PaymentResult(false, sessionId, amountMinor, "Unpaid - payment required at exit");
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

