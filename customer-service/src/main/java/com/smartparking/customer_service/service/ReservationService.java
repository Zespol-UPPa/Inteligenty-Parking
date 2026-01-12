package com.smartparking.customer_service.service;

import com.smartparking.customer_service.client.AccountClient;
import com.smartparking.customer_service.client.ParkingReservationClient;
import com.smartparking.customer_service.client.PaymentClient;
import com.smartparking.customer_service.dto.ReservationResult;
import com.smartparking.customer_service.messaging.ReservationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReservationService {
    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private final ParkingReservationClient parkingClient;
    private final AccountClient accountClient;
    private final ReservationEventPublisher eventPublisher;
    private final PaymentClient paymentClient;
    private final WalletService walletService;
    private final VehicleService vehicleService;
    
    public ReservationService(ParkingReservationClient parkingClient, 
                              AccountClient accountClient,
                              ReservationEventPublisher eventPublisher,
                              PaymentClient paymentClient,
                              WalletService walletService,
                              VehicleService vehicleService) {
        this.parkingClient = parkingClient;
        this.accountClient = accountClient;
        this.eventPublisher = eventPublisher;
        this.paymentClient = paymentClient;
        this.walletService = walletService;
        this.vehicleService = vehicleService;
    }
    
    public List<Map<String, Object>> list(Long accountId) {
        // Pobierz rezerwacje z parking-service
        List<Map<String, Object>> reservations = parkingClient.getReservationsByAccountId(accountId);
        log.info("Retrieved {} reservations from parking-service for accountId={}", reservations.size(), accountId);

        // Pobierz pojazdy użytkownika i zbuduj mapę vehicleId -> licence_plate
        List<Map<String, Object>> vehiclesForAccount = vehicleService.list(accountId);
        log.info("Retrieved {} vehicles for accountId={}", vehiclesForAccount.size(), accountId);
        
        Map<Long, String> plateByVehicleId = new java.util.HashMap<>();
        for (Map<String, Object> v : vehiclesForAccount) {
            Object idObj = v.get("id");
            // VehicleService.list() zwraca "licencePlate" (camelCase), nie "licence_plate"
            Object plateObj = v.get("licencePlate");
            if (idObj == null || plateObj == null) continue;
            try {
                Long vid = Long.parseLong(idObj.toString());
                plateByVehicleId.put(vid, plateObj.toString());
                log.info("Mapped vehicle_id={} to plate={}", vid, plateObj.toString());
            } catch (NumberFormatException e) {
                log.warn("Failed to parse vehicle id: {}", idObj);
            }
        }
        log.info("Created plateByVehicleId map with {} entries: {}", plateByVehicleId.size(), plateByVehicleId);

        // Wzbogac rezerwacje o vehicle_plate (jeśli mamy vehicle_id)
        for (Map<String, Object> r : reservations) {
            Object vehicleIdObj = r.get("vehicle_id");
            if (vehicleIdObj != null) {
                try {
                    Long vid;
                    if (vehicleIdObj instanceof Number) {
                        vid = ((Number) vehicleIdObj).longValue();
                    } else {
                        vid = Long.parseLong(vehicleIdObj.toString());
                    }
                    String plate = plateByVehicleId.get(vid);
                    if (plate != null) {
                        r.put("vehicle_plate", plate);
                        log.info("Found vehicle_plate={} for reservation {} with vehicle_id={}", 
                                plate, r.get("id_reservation"), vid);
                    } else {
                        log.warn("vehicle_id {} not found in plateByVehicleId for reservation {}. Available vehicle IDs: {}", 
                                vid, r.get("id_reservation"), plateByVehicleId.keySet());
                    }
                } catch (NumberFormatException e) {
                    log.error("Failed to parse vehicle_id {} for reservation {}: {}", 
                            vehicleIdObj, r.get("id_reservation"), e.getMessage());
                }
            } else {
                log.warn("vehicle_id is NULL for reservation {}", r.get("id_reservation"));
            }
        }

        return reservations;
    }
    
    public ReservationResult create(Long accountId, Long parkingId, Long spotId, Long vehicleId, Instant start, Instant end) {
        // 1. Pobierz cennik z parking-service (potrzebujemy rate_per_min do obliczenia ceny)
        Optional<Map<String, Object>> pricingOpt = parkingClient.getPricing(parkingId);
        if (pricingOpt.isEmpty()) {
            return ReservationResult.failed("Parking pricing not found");
        }
        
        Map<String, Object> pricing = pricingOpt.get();
        Object ratePerMinObj = pricing.get("ratePerMin");
        if (ratePerMinObj == null) {
            return ReservationResult.failed("Parking rate per minute not found");
        }
        
        int ratePerMin;
        try {
            if (ratePerMinObj instanceof Number) {
                ratePerMin = ((Number) ratePerMinObj).intValue();
            } else {
                ratePerMin = Integer.parseInt(ratePerMinObj.toString());
            }
        } catch (Exception e) {
            return ReservationResult.failed("Invalid parking rate per minute format");
        }
        
        // 2. Oblicz cenę rezerwacji na podstawie czasu trwania (BEZ darmowych 15 minut)
        long durationMinutes = java.time.Duration.between(start, end).toMinutes();
        if (durationMinutes <= 0) {
            return ReservationResult.failed("Reservation duration must be positive");
        }
        
        // Cena = czas trwania * stawka za minutę (BEZ freeMinutes dla rezerwacji)
        long reservationFeeMinor = durationMinutes * ratePerMin;
        
        // 3. Pobierz saldo portfela (walidacja przed utworzeniem rezerwacji)
        Optional<Map<String, Object>> walletOpt = walletService.getByAccountId(accountId);
        if (walletOpt.isEmpty()) {
            // Utwórz portfel jeśli nie istnieje
            walletService.createForAccountId(accountId);
            walletOpt = walletService.getByAccountId(accountId);
            if (walletOpt.isEmpty()) {
                return ReservationResult.failed("Failed to create wallet");
            }
        }
        
        Map<String, Object> wallet = walletOpt.get();
        Object balanceMinorObj = wallet.get("balance_minor");
        if (balanceMinorObj == null) {
            return ReservationResult.failed("Wallet balance not available");
        }
        
        BigDecimal balanceMinor;
        try {
            balanceMinor = new BigDecimal(balanceMinorObj.toString());
        } catch (Exception e) {
            return ReservationResult.failed("Invalid wallet balance format");
        }
        
        // 4. Sprawdź czy saldo >= opłata za rezerwację (walidacja przed rezerwacją)
        BigDecimal feeDecimal = new BigDecimal(reservationFeeMinor);
        if (balanceMinor.compareTo(feeDecimal) < 0) {
            return ReservationResult.failed("Insufficient funds");
        }
        
        // 5. Utwórz rezerwację w parking-service ze statusem "Paid" (miejsce jest zarezerwowane)
        // Status "Paid" jest wymagany, bo enum reservation_status nie ma statusu "Pending"
        // Jeśli płatność się nie powiedzie, anulujemy rezerwację
        long reservationId;
        try {
            reservationId = parkingClient.createReservation(accountId, parkingId, spotId, vehicleId, start, end, "Paid");
        } catch (Exception e) {
            return ReservationResult.failed("Failed to create reservation: " + e.getMessage());
        }
        
        // 6. Pobierz opłatę z portfela przez payment-service (po utworzeniu rezerwacji)
        PaymentClient.PaymentResult paymentResult;
        try {
            paymentResult = paymentClient.chargeReservationFee(accountId, (long) reservationFeeMinor);
        } catch (Exception e) {
            // Płatność się nie powiodła - anuluj rezerwację (używamy accountId dla bezpieczeństwa)
            try {
                parkingClient.cancelReservation(reservationId, accountId);
            } catch (Exception cancelException) {
                // Log error but don't fail - reservation will remain in "Paid" status but payment failed
                log.error("CRITICAL: Failed to cancel reservation {} after payment failed: {}", 
                    reservationId, cancelException.getMessage());
            }
            return ReservationResult.failed("Failed to process payment: " + e.getMessage());
        }
        
        if (!paymentResult.isPaid()) {
            // Płatność się nie powiodła - anuluj rezerwację (używamy accountId dla bezpieczeństwa)
            try {
                parkingClient.cancelReservation(reservationId, accountId);
            } catch (Exception cancelException) {
                // Log error but don't fail - reservation will remain in "Paid" status but payment failed
                log.error("CRITICAL: Failed to cancel reservation {} after payment failed: {}", 
                    reservationId, cancelException.getMessage());
            }
            return ReservationResult.failed("Payment failed: " + paymentResult.getStatus());
        }
        
        // 7. Wyślij email potwierdzający rezerwację z prawdziwymi datami
        // (tylko jeśli wszystko się powiodło - rezerwacja utworzona i płatność przeszła)
        String email = accountClient.getEmailByAccountId(accountId).orElse(null);
        if (email != null) {
            Optional<Map<String, Object>> locationDetails = parkingClient.getLocationDetails(parkingId);
            String parkingName = locationDetails.map(d -> d.get("name_parking").toString()).orElse("Unknown Parking");
            eventPublisher.publishReservationConfirmation(
                email, accountId, reservationId, parkingId, spotId, start, end, parkingName
            );
        }
        
        return ReservationResult.success(reservationId);
    }
    
    /**
     * Anuluje rezerwację przed jej rozpoczęciem.
     * Zwraca środki użytkownikowi.
     * 
     * @param accountId ID konta użytkownika
     * @param reservationId ID rezerwacji do anulowania
     * @return ReservationResult z informacją o sukcesie/błędzie
     */
    public ReservationResult cancel(Long accountId, Long reservationId) {
        // 1. Sprawdź czy rezerwacja istnieje i należy do użytkownika
        Optional<Map<String, Object>> reservationOpt = parkingClient.getReservationById(reservationId);
        if (reservationOpt.isEmpty()) {
            log.warn("Reservation {} not found for cancellation", reservationId);
            return ReservationResult.failed("Reservation not found");
        }
        
        Map<String, Object> reservation = reservationOpt.get();
        Long reservationAccountId;
        try {
            Object accountIdObj = reservation.get("ref_account_id");
            if (accountIdObj == null) {
                return ReservationResult.failed("Reservation account ID is null");
            }
            reservationAccountId = Long.valueOf(accountIdObj.toString());
        } catch (Exception e) {
            log.error("Failed to parse reservation account ID: {}", e.getMessage());
            return ReservationResult.failed("Invalid reservation data");
        }
        
        if (!reservationAccountId.equals(accountId)) {
            log.warn("Reservation {} does not belong to account {} (belongs to {})", 
                reservationId, accountId, reservationAccountId);
            return ReservationResult.failed("Reservation does not belong to this account");
        }
        
        // 2. Sprawdź status rezerwacji
        String status;
        try {
            Object statusObj = reservation.get("status_reservation");
            if (statusObj == null) {
                return ReservationResult.failed("Reservation status is null");
            }
            status = statusObj.toString();
        } catch (Exception e) {
            log.error("Failed to get reservation status: {}", e.getMessage());
            return ReservationResult.failed("Invalid reservation data");
        }
        
        if (!"Paid".equals(status)) {
            log.warn("Cannot cancel reservation {} with status '{}' (only 'Paid' can be cancelled)", 
                reservationId, status);
            return ReservationResult.failed("Cannot cancel reservation with status: " + status);
        }
        
        // 3. Sprawdź czy rezerwacja jeszcze się nie rozpoczęła
        Instant validFrom;
        try {
            Object validFromObj = reservation.get("valid_from");
            if (validFromObj == null) {
                return ReservationResult.failed("Reservation start time is null");
            }
            if (validFromObj instanceof java.sql.Timestamp) {
                validFrom = ((java.sql.Timestamp) validFromObj).toInstant();
            } else if (validFromObj instanceof Instant) {
                validFrom = (Instant) validFromObj;
            } else {
                validFrom = Instant.parse(validFromObj.toString());
            }
        } catch (Exception e) {
            log.error("Failed to parse reservation valid_from: {}", e.getMessage());
            return ReservationResult.failed("Invalid reservation start time");
        }
        
        if (validFrom.isBefore(Instant.now())) {
            log.warn("Cannot cancel reservation {} that has already started (valid_from: {}, now: {})", 
                reservationId, validFrom, Instant.now());
            return ReservationResult.failed("Cannot cancel reservation that has already started");
        }
        
        // 4. Pobierz opłatę za rezerwację (do zwrotu)
        Long parkingId;
        try {
            Object parkingIdObj = reservation.get("parking_id");
            if (parkingIdObj == null) {
                return ReservationResult.failed("Reservation parking ID is null");
            }
            parkingId = Long.valueOf(parkingIdObj.toString());
        } catch (Exception e) {
            log.error("Failed to parse reservation parking ID: {}", e.getMessage());
            return ReservationResult.failed("Invalid reservation data");
        }
        
        Optional<Integer> feeOpt = parkingClient.getReservationFee(parkingId);
        if (feeOpt.isEmpty()) {
            log.warn("Reservation fee not found for parking {}", parkingId);
            return ReservationResult.failed("Reservation fee not found");
        }
        int reservationFeeMinor = feeOpt.get();
        
        // 5. Anuluj rezerwację w parking-service (sprawdza właściciela, status i czas)
        boolean cancelled = parkingClient.cancelReservation(reservationId, accountId);
        if (!cancelled) {
            log.warn("Failed to cancel reservation {} in parking-service", reservationId);
            return ReservationResult.failed("Failed to cancel reservation - invalid status or already started");
        }
        
        log.info("Reservation {} cancelled successfully", reservationId);
        
        // 6. Zwróć pieniądze użytkownikowi
        // Używamy paymentId = 0 (null) - payment-service utworzy nową transakcję zwrotu
        try {
            PaymentClient.PaymentResult refundResult = paymentClient.refundReservationFee(0L, accountId, (long) reservationFeeMinor);
            if (!"Refunded".equals(refundResult.getStatus()) && !"Paid".equals(refundResult.getStatus())) {
                log.error("Refund failed for reservation {}: status = {}", reservationId, refundResult.getStatus());
                // Rezerwacja już anulowana, ale zwrot nie powiódł się - to jest problem operacyjny
                return ReservationResult.failed("Reservation cancelled but refund failed: " + refundResult.getStatus());
            }
            log.info("Reservation {} cancelled and fee {} refunded to account {}", 
                reservationId, reservationFeeMinor, accountId);
        } catch (Exception e) {
            log.error("Exception during refund for reservation {}: {}", reservationId, e.getMessage(), e);
            // Rezerwacja już anulowana, ale zwrot nie powiódł się - to jest problem operacyjny
            return ReservationResult.failed("Reservation cancelled but refund failed: " + e.getMessage());
        }
        
        // 7. Wyślij email z potwierdzeniem anulowania (opcjonalnie)
        // TODO: Można dodać eventPublisher.publishReservationCancellation()
        
        return ReservationResult.success(reservationId);
    }
}

