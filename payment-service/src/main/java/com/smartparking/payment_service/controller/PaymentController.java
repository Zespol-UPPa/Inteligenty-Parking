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
import java.util.Map;

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
        // Check if this is an internal call (from parking-service) - if so, use accountId from request body
        Long accountId;
        String internalToken = request.getHeader("X-Internal-Token");
        if (internalToken != null && !internalToken.isBlank() && 
            internalToken.equals(System.getenv("INTERNAL_SERVICE_TOKEN"))) {
            // Internal call - use accountId from request body
            if (req.getAccountId() != null) {
                accountId = req.getAccountId();
            } else {
                accountId = requireAccountId(request);
            }
        } else {
            // External call - use accountId from JWT token
            accountId = requireAccountId(request);
        }
        
        // Note: req.getUserId() is ignored for security - accountId comes from JWT token or internal token
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
    
    @PostMapping("/deposit")
    public ResponseEntity<?> createDeposit(@RequestBody java.util.Map<String, Object> body, HttpServletRequest request) {
        try {
            if (body == null || body.get("accountId") == null || body.get("amountMinor") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields: accountId and amountMinor"));
            }
            
            Long accountId = Long.valueOf(body.get("accountId").toString());
            Long amountMinor = Long.valueOf(body.get("amountMinor").toString());
            
            // Validate amountMinor fits in Integer range
            if (amountMinor > Integer.MAX_VALUE || amountMinor < Integer.MIN_VALUE) {
                return ResponseEntity.badRequest().body(Map.of("error", "Amount exceeds valid range"));
            }
            
            // Validate amountMinor is positive
            if (amountMinor <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Amount must be positive"));
            }
            
            com.smartparking.payment_service.model.VirtualPayment payment = new com.smartparking.payment_service.model.VirtualPayment();
            payment.setRefAccountId(accountId);
            payment.setRefSessionId(0L); // 0 dla depositów
            payment.setAmountMinor(amountMinor.intValue());
            payment.setCurrencyCode("PLN");
            payment.setStatusPaid("Paid");
            payment.setActivity("deposit");
            payment.setDateTransaction(java.time.LocalDateTime.now());
            
            com.smartparking.payment_service.model.VirtualPayment saved = payments.createDepositPayment(payment);
            return ResponseEntity.ok(new com.smartparking.payment_service.dto.IdResponse(saved.getId()));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid number format: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace(); // Log the full exception for debugging
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/charge/reservation", consumes = "application/json")
    public ResponseEntity<PaymentDto> chargeReservationFee(@RequestBody ChargeRequest req, HttpServletRequest request) {
        // Check if this is an internal call (from customer-service) - if so, use accountId from request body
        Long accountId;
        String internalToken = request.getHeader("X-Internal-Token");
        if (internalToken != null && !internalToken.isBlank() && 
            internalToken.equals(System.getenv("INTERNAL_SERVICE_TOKEN"))) {
            // Internal call - use accountId from request body
            if (req.getAccountId() != null) {
                accountId = req.getAccountId();
            } else {
                accountId = requireAccountId(request);
            }
        } else {
            // External call - use accountId from JWT token
            accountId = requireAccountId(request);
        }
        
        // Convert amount to minor units (amount is in main units like PLN)
        Long amountMinor = req.getAmount().multiply(new java.math.BigDecimal(100)).longValue();
        
        com.smartparking.payment_service.dto.PaymentResult result = payments.chargeReservationFee(accountId, amountMinor);
        
        PaymentStatus status = mapStatus(result.getStatus());
        PaymentDto dto = new PaymentDto(result.getPaymentId(), accountId, req.getAmount(), status, Instant.now(), "WALLET");
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

    /**
     * Get payments by list of session IDs
     * Used by admin-service for financial reports
     *
     * POST /payment/admin/by-sessions
     * Body: [sessionId1, sessionId2, ...]
     * Returns: List of payment objects
     */
    @PostMapping("/admin/by-sessions")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getPaymentsBySessionIds(
            @RequestBody java.util.List<Long> sessionIds) {

        if (sessionIds == null || sessionIds.isEmpty()) {
            return ResponseEntity.ok(new java.util.ArrayList<>());
        }

        java.util.List<java.util.Map<String, Object>> paymentData = payments.getPaymentsBySessionIds(sessionIds);
        return ResponseEntity.ok(paymentData);
    }
}
