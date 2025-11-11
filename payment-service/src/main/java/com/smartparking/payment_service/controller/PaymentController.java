package com.smartparking.payment_service.controller;

import com.smartparking.core.dto.PaymentDto;
import com.smartparking.core.enums.PaymentStatus;
import com.smartparking.payment_service.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService payments;
    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/charge")
    public ResponseEntity<PaymentDto> charge(@RequestParam Long userId,
                                             @RequestParam(required = false) Long sessionId,
                                             @RequestParam BigDecimal amount,
                                             @RequestParam(defaultValue = "PLN") String currency) {
        long paymentId = payments.chargeFromWallet(userId, sessionId != null ? sessionId : 0L, amount, currency);
        PaymentDto dto = new PaymentDto(paymentId, userId, amount, PaymentStatus.SUCCESS, Instant.now(), "WALLET");
        return ResponseEntity.ok(dto);
    }
}


