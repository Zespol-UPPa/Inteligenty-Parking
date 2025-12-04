package com.smartparking.payment_service.service;

import com.smartparking.payment_service.dto.PaymentResult;
import com.smartparking.payment_service.repository.VirtualPaymentRepository;
import com.smartparking.payment_service.client.CustomerWalletClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
        Optional<Map<String, Object>> walletOpt = walletClient.getWallet(accountId);
        if (walletOpt.isEmpty()) {
            // No wallet; treat as pending external
            long paymentId = payments.create(accountId, sessionId, amount, currency, "Pending");
            return new PaymentResult(paymentId, "Pending");
        }
        Map<String, Object> wallet = walletOpt.get();
        // balance_minor is stored as integer minor units in DB; map might contain Integer/Long or String
        Object balanceMinorObj = wallet.get("balance_minor");
        if (balanceMinorObj == null) {
            // No balance field - treat as insufficient funds
            long paymentId = payments.create(accountId, sessionId, amount, currency, "Failed");
            return new PaymentResult(paymentId, "Failed");
        }
        BigDecimal balanceMinor = new BigDecimal(balanceMinorObj.toString());
        BigDecimal amountMinor = amount.multiply(new BigDecimal(100));
        if (balanceMinor.compareTo(amountMinor) >= 0) {
            BigDecimal newBalanceMinor = balanceMinor.subtract(amountMinor);
            boolean updated = walletClient.updateBalance(accountId, newBalanceMinor);
            if (updated) {
                long paymentId = payments.create(accountId, sessionId, amount, currency, "Paid");
                return new PaymentResult(paymentId, "Paid");
            } else {
                long paymentId = payments.create(accountId, sessionId, amount, currency, "Failed");
                return new PaymentResult(paymentId, "Failed");
            }
        } else {
            // insufficient funds
            long paymentId = payments.create(accountId, sessionId, amount, currency, "Failed");
            return new PaymentResult(paymentId, "Failed");
        }
    }
}
