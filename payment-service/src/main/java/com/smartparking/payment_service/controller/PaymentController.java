package com.smartparking.payment_service.controller;

import com.smartparking.payment_service.dto.ChargeRequest;
import com.smartparking.payment_service.dto.PaymentDto;
import com.smartparking.payment_service.dto.PaymentStatus;
import com.smartparking.payment_service.security.RequestContext;
import com.smartparking.payment_service.security.JwtContextFilter;
import com.smartparking.payment_service.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
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

    private Long requireAccountId(HttpServletRequest request) {
        RequestContext ctx = (RequestContext) request.getAttribute(JwtContextFilter.ATTR_CONTEXT);
        if (ctx == null || ctx.getUsername() == null || ctx.getUsername().isBlank()) {
            throw new IllegalArgumentException("Invalid request context: username is required");
        }
        try {
            Long accountId = Long.parseLong(ctx.getUsername());
            if (accountId <= 0) {
                throw new IllegalArgumentException("Invalid account ID: must be positive");
            }
            return accountId;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid account ID format: " + ctx.getUsername(), e);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    // JSON body version
    @PostMapping(value = "/charge", consumes = "application/json")
    public ResponseEntity<PaymentDto> chargeJson(@RequestBody ChargeRequest req, HttpServletRequest request) {
        Long accountId = requireAccountId(request);
        // Note: req.getUserId() is ignored for security - accountId comes from JWT token
        com.smartparking.payment_service.dto.PaymentResult result = payments.chargeFromWallet(
                accountId, 
                req.getSessionId() != null ? req.getSessionId() : 0L, 
                req.getAmount(), 
                req.getCurrency() != null ? req.getCurrency() : "PLN");
        
        PaymentStatus status = mapStatus(result.getStatus());
        PaymentDto dto = new PaymentDto(result.getPaymentId(), accountId, req.getAmount(), status, Instant.now(), "WALLET");
        return ResponseEntity.ok(dto);
    }

    // Legacy form/query params version
    @PostMapping(value = "/charge", consumes = {"application/x-www-form-urlencoded", "*/*"})
    public ResponseEntity<PaymentDto> charge(@RequestParam(required = false) Long sessionId,
                                             @RequestParam BigDecimal amount,
                                             @RequestParam(defaultValue = "PLN") String currency,
                                             HttpServletRequest request) {
        Long accountId = requireAccountId(request);
        // Note: userId parameter removed for security - accountId comes from JWT token
        com.smartparking.payment_service.dto.PaymentResult result = payments.chargeFromWallet(
                accountId, 
                sessionId != null ? sessionId : 0L, 
                amount, 
                currency);
        
        PaymentStatus status = mapStatus(result.getStatus());
        PaymentDto dto = new PaymentDto(result.getPaymentId(), accountId, amount, status, Instant.now(), "WALLET");
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
