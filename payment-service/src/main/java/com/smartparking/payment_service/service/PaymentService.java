package com.smartparking.payment_service.service;

import com.smartparking.payment_service.dto.PaymentResult;
import com.smartparking.payment_service.model.VirtualPayment;
import com.smartparking.payment_service.repository.VirtualPaymentRepository;
import com.smartparking.payment_service.client.CustomerWalletClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

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

    private VirtualPayment createPaymentEntity(Long accountId, Long sessionId, int amountMinor, String currency, String status) {
        return createPaymentEntity(accountId, sessionId, amountMinor, currency, status, "parking");
    }

    private VirtualPayment createPaymentEntity(Long accountId, Long sessionId, int amountMinor, String currency, String status, String activity) {
        VirtualPayment payment = new VirtualPayment();
        payment.setAmountMinor(amountMinor);
        payment.setCurrencyCode(currency != null ? currency : "PLN");
        payment.setStatusPaid(status);
        payment.setRefAccountId(accountId);
        payment.setRefSessionId(sessionId != null ? sessionId : 0L);
        payment.setActivity(activity != null ? activity : "parking");
        payment.setDateTransaction(LocalDateTime.now());
        return payment;
    }
}
