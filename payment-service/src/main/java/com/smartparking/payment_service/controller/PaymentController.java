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
import java.util.List;
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

    /**
     * Pobiera historię transakcji dla danego konta użytkownika
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<Map<String, Object>>> getTransactions(HttpServletRequest request) {
        try {
            Long accountId = requireAccountId(request);
            List<Map<String, Object>> transactions = payments.getTransactionsByAccountId(accountId);
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    /**
     * Pobiera statystyki dla danego konta użytkownika (totalSpent, totalTopUps, totalTransactions)
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(HttpServletRequest request) {
        try {
            Long accountId = requireAccountId(request);
            Map<String, Object> stats = payments.getStatisticsByAccountId(accountId);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get statistics: " + e.getMessage()));
        }
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

    @PostMapping(value = "/refund/reservation", consumes = "application/json")
    public ResponseEntity<PaymentDto> refundReservationFee(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        // Check if this is an internal call (from customer-service)
        Long accountId;
        String internalToken = request.getHeader("X-Internal-Token");
        if (internalToken != null && !internalToken.isBlank() && 
            internalToken.equals(System.getenv("INTERNAL_SERVICE_TOKEN"))) {
            if (body.get("accountId") != null) {
                accountId = Long.valueOf(body.get("accountId").toString());
            } else {
                accountId = requireAccountId(request);
            }
        } else {
            accountId = requireAccountId(request);
        }
        
        if (body.get("paymentId") == null || body.get("amountMinor") == null) {
            return ResponseEntity.badRequest().body(null);
        }
        
        Long paymentId = Long.valueOf(body.get("paymentId").toString());
        Long amountMinor = Long.valueOf(body.get("amountMinor").toString());
        
        com.smartparking.payment_service.dto.PaymentResult result = payments.refundReservationFee(paymentId, accountId, amountMinor);
        
        PaymentStatus status = "Refunded".equals(result.getStatus()) ? PaymentStatus.CANCELLED : mapStatus(result.getStatus());
        PaymentDto dto = new PaymentDto(result.getPaymentId(), accountId, 
            new BigDecimal(amountMinor).divide(new BigDecimal(100)), status, Instant.now(), "WALLET");
        return ResponseEntity.ok(dto);
    }
    
    /**
     * Endpoint do płatności dla niezarejestrowanych klientów (płatność przy wyjeździe)
     * Używane przez terminal/kasę przy wyjeździe
     */
    @PostMapping("/session/{sessionId}/pay-guest")
    public ResponseEntity<?> payForUnregisteredSession(
            @PathVariable Long sessionId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        // Sprawdź token wewnętrzny (z parking-service lub terminala)
        String internalToken = request.getHeader("X-Internal-Token");
        if (internalToken == null || internalToken.isBlank() || 
            !internalToken.equals(System.getenv("INTERNAL_SERVICE_TOKEN"))) {
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized - internal token required"));
        }
        
        Object amountMinorObj = body.get("amountMinor");
        if (amountMinorObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "amountMinor is required"));
        }
        
        Long amountMinor;
        try {
            if (amountMinorObj instanceof Number) {
                amountMinor = ((Number) amountMinorObj).longValue();
            } else {
                amountMinor = Long.parseLong(amountMinorObj.toString());
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid amountMinor format: " + amountMinorObj));
        }
        
        if (amountMinor <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Amount must be positive"));
        }
        
        String paymentMethod = body.getOrDefault("paymentMethod", "cash").toString();
        
        try {
            com.smartparking.payment_service.dto.PaymentResult result = 
                payments.payForUnregisteredSession(sessionId, amountMinor, paymentMethod);
            
            return ResponseEntity.ok(Map.of(
                "paymentId", result.getPaymentId(),
                "status", result.getStatus(),
                "sessionId", sessionId,
                "amountMinor", amountMinor
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to process payment: " + e.getMessage()));
        }
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
