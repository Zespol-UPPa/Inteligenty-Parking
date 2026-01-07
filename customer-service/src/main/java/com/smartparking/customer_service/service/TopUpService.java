package com.smartparking.customer_service.service;

import com.smartparking.customer_service.client.AccountClient;
import com.smartparking.customer_service.client.PaymentClient;
import com.smartparking.customer_service.dto.TopUpResult;
import com.smartparking.customer_service.messaging.TopUpEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
public class TopUpService {
    private final WalletService walletService;
    private final PaymentClient paymentClient;
    private final AccountClient accountClient;
    private final TopUpEventPublisher eventPublisher;

    public TopUpService(WalletService walletService, PaymentClient paymentClient, 
                       AccountClient accountClient, TopUpEventPublisher eventPublisher) {
        this.walletService = walletService;
        this.paymentClient = paymentClient;
        this.accountClient = accountClient;
        this.eventPublisher = eventPublisher;
    }

    public TopUpResult processTopUp(Long accountId, Long amountMinor, String paymentMethod) {
        // 1. Pobierz aktualne saldo
        Optional<Map<String, Object>> walletOpt = walletService.getByAccountId(accountId);
        if (walletOpt.isEmpty()) {
            walletService.createForAccountId(accountId);
            walletOpt = walletService.getByAccountId(accountId);
        }

        BigDecimal currentBalance = new BigDecimal(walletOpt.get().get("balance_minor").toString());
        BigDecimal newBalance = currentBalance.add(new BigDecimal(amountMinor));

        // 2. Zaktualizuj saldo
        boolean updated = walletService.setBalance(accountId, newBalance);
        if (!updated) {
            return TopUpResult.failed("Failed to update wallet balance");
        }

        // 3. Utwórz rekord płatności w payment-service (activity='deposit')
        Long paymentId;
        try {
            paymentId = paymentClient.createDepositPayment(accountId, amountMinor, paymentMethod);
        } catch (Exception e) {
            // Rollback balance update if payment record creation fails
            walletService.setBalance(accountId, currentBalance);
            return TopUpResult.failed("Failed to create payment record: " + e.getMessage());
        }

        // 4. Wyślij email przez RabbitMQ (non-blocking)
        String email = accountClient.getEmailByAccountId(accountId).orElse(null);
        if (email != null) {
            eventPublisher.publishTopUpConfirmation(email, accountId, amountMinor, newBalance);
        }

        return TopUpResult.success(paymentId, newBalance);
    }
}

