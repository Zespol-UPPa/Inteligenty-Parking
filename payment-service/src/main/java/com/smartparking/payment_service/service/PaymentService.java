package com.smartparking.payment_service.service;

import com.smartparking.payment_service.repository.VirtualPaymentRepository;
import com.smartparking.payment_service.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {
    private final VirtualPaymentRepository payments;
    private final WalletRepository wallets;
    public PaymentService(VirtualPaymentRepository payments, WalletRepository wallets) {
        this.payments = payments;
        this.wallets = wallets;
    }

    public long chargeFromWallet(Long accountId, Long sessionId, BigDecimal amount, String currency) {
        Optional<Map<String, Object>> walletOpt = wallets.findByAccountId(accountId);
        if (walletOpt.isEmpty()) {
            // No wallet; treat as pending external
            return payments.create(accountId, sessionId, amount, currency, "Pending");
        }
        Map<String, Object> wallet = walletOpt.get();
        BigDecimal balance = (BigDecimal) wallet.get("balance_minor");
        Long walletId = (Long) wallet.get("id_wallet");
        if (balance.compareTo(amount) >= 0) {
            BigDecimal newBalance = balance.subtract(amount);
            wallets.updateBalance(walletId, newBalance);
            return payments.create(accountId, sessionId, amount, currency, "Paid");
        } else {
            // insufficient funds
            return payments.create(accountId, sessionId, amount, currency, "Failed");
        }
    }
}


