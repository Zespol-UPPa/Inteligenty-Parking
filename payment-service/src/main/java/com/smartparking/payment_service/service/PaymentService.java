package com.smartparking.payment_service.service;

import com.smartparking.payment_service.dto.PaymentResult;
import com.smartparking.payment_service.model.VirtualPayment;
import com.smartparking.payment_service.repository.VirtualPaymentRepository;
import com.smartparking.payment_service.client.CustomerWalletClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    private final VirtualPaymentRepository payments;
    private final CustomerWalletClient walletClient;
    public PaymentService(VirtualPaymentRepository payments, CustomerWalletClient walletClient) {
        this.payments = payments;
        this.walletClient = walletClient;
    }

    public PaymentResult chargeFromWallet(Long accountId, Long sessionId, BigDecimal amount, String currency) {
        // Convert amount to minor units (grosze) - amount is in main units (PLN)
        BigDecimal amountMinorDecimal = amount.multiply(new BigDecimal(100));
        if (amountMinorDecimal.compareTo(new BigDecimal(Integer.MAX_VALUE)) > 0) {
            // Amount exceeds Integer.MAX_VALUE - create failed payment (amount too large)
            VirtualPayment payment = createPaymentEntity(accountId, sessionId, 0, currency, "Failed");
            VirtualPayment saved = payments.save(payment);
            return new PaymentResult(saved.getId(), "Failed");
        }
        int amountMinor = amountMinorDecimal.intValue();

        Optional<Map<String, Object>> walletOpt;
        try {
            walletOpt = walletClient.getWallet(accountId);
        } catch (Exception e) {
            // Error getting wallet - create pending payment
            VirtualPayment payment = createPaymentEntity(accountId, sessionId, amountMinor, currency, "Pending");
            VirtualPayment saved = payments.save(payment);
            return new PaymentResult(saved.getId(), "Pending");
        }

        if (walletOpt.isEmpty()) {
            // No wallet; treat as pending external
            VirtualPayment payment = createPaymentEntity(accountId, sessionId, amountMinor, currency, "Pending");
            VirtualPayment saved = payments.save(payment);
            return new PaymentResult(saved.getId(), "Pending");
        }

        Map<String, Object> wallet = walletOpt.get();
        // balance_minor is stored as integer minor units in DB; map might contain Integer/Long or String
        Object balanceMinorObj = wallet.get("balance_minor");
        if (balanceMinorObj == null) {
            // No balance field - treat as insufficient funds
            VirtualPayment payment = createPaymentEntity(accountId, sessionId, amountMinor, currency, "Failed");
            VirtualPayment saved = payments.save(payment);
            return new PaymentResult(saved.getId(), "Failed");
        }

        BigDecimal balanceMinor;
        try {
            balanceMinor = new BigDecimal(balanceMinorObj.toString());
        } catch (Exception e) {
            // Invalid balance format - create failed payment
            VirtualPayment payment = createPaymentEntity(accountId, sessionId, amountMinor, currency, "Failed");
            VirtualPayment saved = payments.save(payment);
            return new PaymentResult(saved.getId(), "Failed");
        }

        BigDecimal amountMinorDecimalCompare = new BigDecimal(amountMinor);
        if (balanceMinor.compareTo(amountMinorDecimalCompare) >= 0) {
            BigDecimal newBalanceMinor = balanceMinor.subtract(amountMinorDecimalCompare);
            boolean updated;
            try {
                updated = walletClient.updateBalance(accountId, newBalanceMinor);
            } catch (Exception e) {
                // Error updating balance - create failed payment
                VirtualPayment payment = createPaymentEntity(accountId, sessionId, amountMinor, currency, "Failed");
                VirtualPayment saved = payments.save(payment);
                return new PaymentResult(saved.getId(), "Failed");
            }

            if (updated) {
                VirtualPayment payment = createPaymentEntity(accountId, sessionId, amountMinor, currency, "Paid");
                VirtualPayment saved = payments.save(payment);
                return new PaymentResult(saved.getId(), "Paid");
            } else {
                VirtualPayment payment = createPaymentEntity(accountId, sessionId, amountMinor, currency, "Failed");
                VirtualPayment saved = payments.save(payment);
                return new PaymentResult(saved.getId(), "Failed");
            }
        } else {
            // insufficient funds
            VirtualPayment payment = createPaymentEntity(accountId, sessionId, amountMinor, currency, "Failed");
            VirtualPayment saved = payments.save(payment);
            return new PaymentResult(saved.getId(), "Failed");
        }
    }

    public com.smartparking.payment_service.dto.PaymentResult chargeForParkingSession(Long accountId, Long sessionId, Long amountMinor) {
        BigDecimal amount = new BigDecimal(amountMinor).divide(new BigDecimal(100));
        com.smartparking.payment_service.dto.PaymentResult result = chargeFromWallet(accountId, sessionId, amount, "PLN");
        
        if (result.getStatus().equals("Paid")) {
            // Zaktualizuj virtual_payment z activity='parking'
            Optional<VirtualPayment> paymentOpt = payments.findById(result.getPaymentId());
            if (paymentOpt.isPresent()) {
                VirtualPayment payment = paymentOpt.get();
                payment.setActivity("parking");
                payments.save(payment);
            }
        }
        
        return result;
    }

    public com.smartparking.payment_service.dto.PaymentResult chargeReservationFee(Long accountId, Long amountMinor) {
        BigDecimal amount = new BigDecimal(amountMinor).divide(new BigDecimal(100));
        
        // Convert amount to minor units (grosze) - amount is in main units (PLN)
        BigDecimal amountMinorDecimal = amount.multiply(new BigDecimal(100));
        if (amountMinorDecimal.compareTo(new BigDecimal(Integer.MAX_VALUE)) > 0) {
            // Amount exceeds Integer.MAX_VALUE - create failed payment (amount too large)
            VirtualPayment payment = createPaymentEntity(accountId, 0L, 0, "PLN", "Failed", "reservation");
            VirtualPayment saved = payments.save(payment);
            return new com.smartparking.payment_service.dto.PaymentResult(saved.getId(), "Failed");
        }
        int amountMinorInt = amountMinorDecimal.intValue();

        Optional<Map<String, Object>> walletOpt;
        try {
            walletOpt = walletClient.getWallet(accountId);
        } catch (Exception e) {
            // Error getting wallet - create pending payment
            VirtualPayment payment = createPaymentEntity(accountId, 0L, amountMinorInt, "PLN", "Pending", "reservation");
            VirtualPayment saved = payments.save(payment);
            return new com.smartparking.payment_service.dto.PaymentResult(saved.getId(), "Pending");
        }

        if (walletOpt.isEmpty()) {
            // No wallet; treat as pending external
            VirtualPayment payment = createPaymentEntity(accountId, 0L, amountMinorInt, "PLN", "Pending", "reservation");
            VirtualPayment saved = payments.save(payment);
            return new com.smartparking.payment_service.dto.PaymentResult(saved.getId(), "Pending");
        }

        Map<String, Object> wallet = walletOpt.get();
        // balance_minor is stored as integer minor units in DB; map might contain Integer/Long or String
        Object balanceMinorObj = wallet.get("balance_minor");
        if (balanceMinorObj == null) {
            // No balance field - treat as insufficient funds
            VirtualPayment payment = createPaymentEntity(accountId, 0L, amountMinorInt, "PLN", "Failed", "reservation");
            VirtualPayment saved = payments.save(payment);
            return new com.smartparking.payment_service.dto.PaymentResult(saved.getId(), "Failed");
        }

        BigDecimal balanceMinor;
        try {
            balanceMinor = new BigDecimal(balanceMinorObj.toString());
        } catch (Exception e) {
            // Invalid balance format - create failed payment
            VirtualPayment payment = createPaymentEntity(accountId, 0L, amountMinorInt, "PLN", "Failed", "reservation");
            VirtualPayment saved = payments.save(payment);
            return new com.smartparking.payment_service.dto.PaymentResult(saved.getId(), "Failed");
        }

        BigDecimal amountMinorDecimalCompare = new BigDecimal(amountMinorInt);
        if (balanceMinor.compareTo(amountMinorDecimalCompare) >= 0) {
            BigDecimal newBalanceMinor = balanceMinor.subtract(amountMinorDecimalCompare);
            boolean updated;
            try {
                updated = walletClient.updateBalance(accountId, newBalanceMinor);
            } catch (Exception e) {
                // Error updating balance - create failed payment
                VirtualPayment payment = createPaymentEntity(accountId, 0L, amountMinorInt, "PLN", "Failed", "reservation");
                VirtualPayment saved = payments.save(payment);
                return new com.smartparking.payment_service.dto.PaymentResult(saved.getId(), "Failed");
            }

            if (updated) {
                VirtualPayment payment = createPaymentEntity(accountId, 0L, amountMinorInt, "PLN", "Paid", "reservation");
                VirtualPayment saved = payments.save(payment);
                return new com.smartparking.payment_service.dto.PaymentResult(saved.getId(), "Paid");
            } else {
                VirtualPayment payment = createPaymentEntity(accountId, 0L, amountMinorInt, "PLN", "Failed", "reservation");
                VirtualPayment saved = payments.save(payment);
                return new com.smartparking.payment_service.dto.PaymentResult(saved.getId(), "Failed");
            }
        } else {
            // insufficient funds
            VirtualPayment payment = createPaymentEntity(accountId, 0L, amountMinorInt, "PLN", "Failed", "reservation");
            VirtualPayment saved = payments.save(payment);
            return new com.smartparking.payment_service.dto.PaymentResult(saved.getId(), "Failed");
        }
    }

    public VirtualPayment createDepositPayment(VirtualPayment payment) {
        return payments.save(payment);
    }

    /**
     * Refunds a reservation fee payment back to the wallet
     * @param paymentId The payment ID to refund (może być 0/null jeśli nie mamy paymentId - wtedy tylko dodaje środki)
     * @param accountId The account ID to refund to
     * @param amountMinor The amount to refund in minor units (grosze)
     * @return PaymentResult with status "Refunded" or "Failed"
     */
    public com.smartparking.payment_service.dto.PaymentResult refundReservationFee(Long paymentId, Long accountId, Long amountMinor) {
        // Jeśli paymentId = 0 lub null, po prostu dodajemy środki do portfela (refund bez anulowania płatności)
        boolean refundWithoutPayment = (paymentId == null || paymentId == 0L);
        
        if (!refundWithoutPayment) {
        // 1. Sprawdź czy płatność istnieje i jest "Paid"
        Optional<VirtualPayment> paymentOpt = payments.findById(paymentId);
        if (paymentOpt.isEmpty()) {
                // Jeśli płatność nie istnieje, traktuj jak refund bez paymentId
                refundWithoutPayment = true;
            } else {
        VirtualPayment payment = paymentOpt.get();
        // Sprawdź czy płatność jest już refundowana lub nie była opłacona
        if (!"Paid".equals(payment.getStatusPaid())) {
            return new com.smartparking.payment_service.dto.PaymentResult(paymentId, "Failed");
        }
        
        // Sprawdź czy aktywność to "reservation"
        if (!"reservation".equals(payment.getActivity())) {
            return new com.smartparking.payment_service.dto.PaymentResult(paymentId, "Failed");
                }
            }
        }
        
        // 2. Pobierz aktualne saldo portfela
        Optional<Map<String, Object>> walletOpt;
        try {
            walletOpt = walletClient.getWallet(accountId);
        } catch (Exception e) {
            return new com.smartparking.payment_service.dto.PaymentResult(paymentId, "Failed");
        }
        
        if (walletOpt.isEmpty()) {
            return new com.smartparking.payment_service.dto.PaymentResult(paymentId, "Failed");
        }
        
        Map<String, Object> wallet = walletOpt.get();
        Object balanceMinorObj = wallet.get("balance_minor");
        if (balanceMinorObj == null) {
            return new com.smartparking.payment_service.dto.PaymentResult(paymentId, "Failed");
        }
        
        BigDecimal balanceMinor;
        try {
            balanceMinor = new BigDecimal(balanceMinorObj.toString());
        } catch (Exception e) {
            return new com.smartparking.payment_service.dto.PaymentResult(paymentId, "Failed");
        }
        
        // 3. Dodaj środki z powrotem do portfela
        BigDecimal refundAmountMinor = new BigDecimal(amountMinor);
        BigDecimal newBalanceMinor = balanceMinor.add(refundAmountMinor);
        boolean updated;
        try {
            updated = walletClient.updateBalance(accountId, newBalanceMinor);
        } catch (Exception e) {
            return new com.smartparking.payment_service.dto.PaymentResult(paymentId, "Failed");
        }
        
        if (updated) {
            // 4. Oznacz płatność jako refundowaną (używamy "Cancelled" bo enum nie ma "Refunded")
            // Tylko jeśli mamy paymentId i płatność istnieje
            Long originalRefSessionId = null;
            if (!refundWithoutPayment && paymentId != null && paymentId != 0L) {
                Optional<VirtualPayment> paymentOpt = payments.findById(paymentId);
                if (paymentOpt.isPresent()) {
                    VirtualPayment payment = paymentOpt.get();
            payment.setStatusPaid("Cancelled");
            payments.save(payment);
                    originalRefSessionId = payment.getRefSessionId();
                }
            }
            
            // 5. Utwórz rekord refund (opcjonalnie - dla historii)
            // Używamy activity="deposit" dla refund, bo enum activity_type ma tylko: 'deposit', 'reservation', 'parking'
            VirtualPayment refundPayment = createPaymentEntity(accountId, 0L, amountMinor.intValue(), "PLN", "Paid", "deposit");
            if (originalRefSessionId != null) {
                refundPayment.setRefSessionId(originalRefSessionId);
            }
            payments.save(refundPayment);
            
            return new com.smartparking.payment_service.dto.PaymentResult(refundWithoutPayment ? refundPayment.getId() : (paymentId != null ? paymentId : 0L), "Refunded");
        } else {
            return new com.smartparking.payment_service.dto.PaymentResult(paymentId != null ? paymentId : 0L, "Failed");
        }
    }

    private VirtualPayment createPaymentEntity(Long accountId, Long sessionId, int amountMinor, String currency, String status) {
        return createPaymentEntity(accountId, sessionId, amountMinor, currency, status, "parking");
    }

    /**
     * Tworzy płatność dla niezarejestrowanego klienta (płatność przy wyjeździe - gotówka/karta)
     * @param sessionId ID sesji parkingowej
     * @param amountMinor Kwota w groszach
     * @param paymentMethod Metoda płatności (cash, card, etc.)
     * @return PaymentResult z statusem "Paid"
     */
    public com.smartparking.payment_service.dto.PaymentResult payForUnregisteredSession(Long sessionId, Long amountMinor, String paymentMethod) {
        if (amountMinor == null || amountMinor <= 0) {
            throw new IllegalArgumentException("Invalid amount: " + amountMinor);
        }
        if (amountMinor > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Amount too large: " + amountMinor);
        }
        
        // Utwórz płatność bez accountId (null) - dla niezarejestrowanych
        VirtualPayment payment = new VirtualPayment();
        payment.setAmountMinor(amountMinor.intValue());
        payment.setCurrencyCode("PLN");
        payment.setStatusPaid("Paid"); // Płatność przy wyjeździe zawsze "Paid"
        payment.setRefAccountId(null); // NULL dla niezarejestrowanych
        payment.setRefSessionId(sessionId);
        payment.setActivity("parking");
        payment.setDateTransaction(LocalDateTime.now());
        
        VirtualPayment saved = payments.save(payment);
        return new com.smartparking.payment_service.dto.PaymentResult(saved.getId(), "Paid");
    }

    private VirtualPayment createPaymentEntity(Long accountId, Long sessionId, int amountMinor, String currency, String status, String activity) {
        VirtualPayment payment = new VirtualPayment();
        payment.setAmountMinor(amountMinor);
        payment.setCurrencyCode(currency != null ? currency : "PLN");
        payment.setStatusPaid(status);
        payment.setRefAccountId(accountId); // Może być null dla niezarejestrowanych
        payment.setRefSessionId(sessionId != null ? sessionId : 0L);
        payment.setActivity(activity != null ? activity : "parking");
        payment.setDateTransaction(LocalDateTime.now());
        return payment;
    }

    /**
     * Pobiera wszystkie transakcje dla danego konta użytkownika
     * @param accountId ID konta użytkownika
     * @return Lista transakcji w formacie Map
     */
    public List<Map<String, Object>> getTransactionsByAccountId(Long accountId) {
        List<VirtualPayment> paymentsList = payments.findByAccountId(accountId);
        
        return paymentsList.stream()
            .map(p -> {
                Map<String, Object> t = new HashMap<>();
                t.put("id", p.getId());
                t.put("amount", p.getAmountMinor() / 100.0); // Konwersja z groszy na złotówki
                t.put("currency", p.getCurrencyCode());
                t.put("status", p.getStatusPaid());
                // Konwertuj LocalDateTime na ISO string dla frontendu
                if (p.getDateTransaction() != null) {
                    t.put("date", p.getDateTransaction().toString()); // Format: "2025-12-20T10:02:00"
                } else {
                    t.put("date", LocalDateTime.now().toString());
                }
                t.put("activity", p.getActivity()); // 'deposit', 'reservation', 'parking'
                t.put("sessionId", p.getRefSessionId());
                return t;
            })
            .collect(Collectors.toList());
    }

    /**
     * Pobiera statystyki dla danego konta użytkownika
     * @param accountId ID konta użytkownika
     * @return Map ze statystykami: totalSpent, totalTopUps, totalTransactions
     */
    public Map<String, Object> getStatisticsByAccountId(Long accountId) {
        List<VirtualPayment> allPayments = payments.findByAccountId(accountId);
        
        // Total Spent: suma płatności za parking i rezerwacje (tylko status "Paid")
        long totalSpent = allPayments.stream()
            .filter(p -> ("parking".equals(p.getActivity()) || "reservation".equals(p.getActivity())))
            .filter(p -> "Paid".equals(p.getStatusPaid()))
            .mapToLong(p -> p.getAmountMinor())
            .sum();
        
        // Total Top-Ups: suma doładowań (tylko status "Paid")
        long totalTopUps = allPayments.stream()
            .filter(p -> "deposit".equals(p.getActivity()))
            .filter(p -> "Paid".equals(p.getStatusPaid()))
            .mapToLong(p -> p.getAmountMinor())
            .sum();
        
        // Total Transactions: liczba wszystkich opłaconych transakcji
        long totalTransactions = allPayments.stream()
            .filter(p -> "Paid".equals(p.getStatusPaid()))
            .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSpent", totalSpent / 100.0); // Konwersja z groszy na złotówki
        stats.put("totalTopUps", totalTopUps / 100.0);
        stats.put("totalTransactions", totalTransactions);
        return stats;
    }
}
