package com.smartparking.payment_service.controller;

import com.smartparking.payment_service.dto.ChargeRequest;
import com.smartparking.payment_service.dto.PaymentDto;
import com.smartparking.payment_service.dto.PaymentStatus;
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

    // JSON body version
    @PostMapping(value = "/charge", consumes = "application/json")
    public ResponseEntity<PaymentDto> chargeJson(@RequestBody ChargeRequest req) {
        com.smartparking.payment_service.dto.PaymentResult result = payments.chargeFromWallet(
                req.getUserId(), 
                req.getSessionId() != null ? req.getSessionId() : 0L, 
                req.getAmount(), 
                req.getCurrency() != null ? req.getCurrency() : "PLN");
        
        PaymentStatus status = mapStatus(result.getStatus());
        PaymentDto dto = new PaymentDto(result.getPaymentId(), req.getUserId(), req.getAmount(), status, Instant.now(), "WALLET");
        return ResponseEntity.ok(dto);
    }

    // Legacy form/query params version
    @PostMapping(value = "/charge", consumes = {"application/x-www-form-urlencoded", "*/*"})
    public ResponseEntity<PaymentDto> charge(@RequestParam Long userId,
                                             @RequestParam(required = false) Long sessionId,
                                             @RequestParam BigDecimal amount,
                                             @RequestParam(defaultValue = "PLN") String currency) {
        com.smartparking.payment_service.dto.PaymentResult result = payments.chargeFromWallet(
                userId, 
                sessionId != null ? sessionId : 0L, 
                amount, 
                currency);
        
        PaymentStatus status = mapStatus(result.getStatus());
        PaymentDto dto = new PaymentDto(result.getPaymentId(), userId, amount, status, Instant.now(), "WALLET");
        return ResponseEntity.ok(dto);
    }
    
    private PaymentStatus mapStatus(String dbStatus) {
        if (dbStatus == null) {
            return PaymentStatus.FAILED;
        }
        return switch (dbStatus.toLowerCase()) {
            case "paid" -> PaymentStatus.SUCCESS;
            case "pending" -> PaymentStatus.PENDING;
            case "failed" -> PaymentStatus.FAILED;
            case "cancelled" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.FAILED;
        };
    }
}
