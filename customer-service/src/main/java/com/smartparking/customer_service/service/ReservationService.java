package com.smartparking.customer_service.service;

import com.smartparking.customer_service.client.AccountClient;
import com.smartparking.customer_service.client.ParkingReservationClient;
import com.smartparking.customer_service.client.PaymentClient;
import com.smartparking.customer_service.dto.ReservationResult;
import com.smartparking.customer_service.messaging.ReservationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReservationService {
    private final ParkingReservationClient parkingClient;
    private final AccountClient accountClient;
    private final ReservationEventPublisher eventPublisher;
    private final PaymentClient paymentClient;
    private final WalletService walletService;
    
    public ReservationService(ParkingReservationClient parkingClient, 
                              AccountClient accountClient,
                              ReservationEventPublisher eventPublisher,
                              PaymentClient paymentClient,
                              WalletService walletService) {
        this.parkingClient = parkingClient;
        this.accountClient = accountClient;
        this.eventPublisher = eventPublisher;
        this.paymentClient = paymentClient;
        this.walletService = walletService;
    }
    
    public List<Map<String, Object>> list(Long accountId) {
        return parkingClient.getReservationsByAccountId(accountId);
    }
    
    public ReservationResult create(Long accountId, Long parkingId, Long spotId, Instant start, Instant end) {
        // 1. Pobierz opłatę za rezerwację z parking-service
        Optional<Integer> feeOpt = parkingClient.getReservationFee(parkingId);
        if (feeOpt.isEmpty()) {
            return ReservationResult.failed("Parking pricing not found");
        }
        int reservationFeeMinor = feeOpt.get();
        
        // 2. Pobierz saldo portfela
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
        
        // 3. Sprawdź czy saldo >= opłata za rezerwację
        BigDecimal feeDecimal = new BigDecimal(reservationFeeMinor);
        if (balanceMinor.compareTo(feeDecimal) < 0) {
            return ReservationResult.failed("Insufficient funds");
        }
        
        // 4. Pobierz opłatę z portfela przez payment-service
        PaymentClient.PaymentResult paymentResult;
        try {
            paymentResult = paymentClient.chargeReservationFee(accountId, (long) reservationFeeMinor);
        } catch (Exception e) {
            return ReservationResult.failed("Failed to process payment: " + e.getMessage());
        }
        
        if (!paymentResult.isPaid()) {
            return ReservationResult.failed("Payment failed: " + paymentResult.getStatus());
        }
        
        // 5. Utwórz rezerwację w parking-service ze statusem "Paid"
        long reservationId;
        try {
            reservationId = parkingClient.createReservation(accountId, parkingId, spotId, end, "Paid");
        } catch (Exception e) {
            // TODO: Rollback payment if reservation creation fails
            return ReservationResult.failed("Failed to create reservation: " + e.getMessage());
        }
        
        // 6. Wyślij email potwierdzający rezerwację z prawdziwymi datami
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
}

