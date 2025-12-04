package com.smartparking.customer_service.controller;

import com.smartparking.customer_service.service.WalletService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/internal/wallet")
public class InternalWalletController {
    private final WalletService walletService;
    private final String internalToken;

    public InternalWalletController(WalletService walletService, @Value("${INTERNAL_SERVICE_TOKEN:}") String internalToken) {
        this.walletService = walletService;
        this.internalToken = internalToken;
    }

    private boolean checkToken(HttpServletRequest request) {
        // Require internal token for security - if not configured, reject all requests
        if (internalToken == null || internalToken.isBlank()) {
            return false; // Security: require token configuration
        }
        String provided = request.getHeader("X-Internal-Token");
        if (provided == null || provided.isBlank()) {
            return false;
        }
        // Use constant-time comparison to prevent timing attacks
        return constantTimeEquals(provided, internalToken);
    }
    
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<?> getWallet(@PathVariable Long accountId, HttpServletRequest request) {
        if (!checkToken(request)) return ResponseEntity.status(403).build();
        Optional<Map<String, Object>> w = walletService.getByAccountId(accountId);
        return w.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{accountId}/debit")
    public ResponseEntity<?> debitWallet(@PathVariable Long accountId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (!checkToken(request)) return ResponseEntity.status(403).build();
        // expects { "balance_minor": "123.45" } or numeric
        Object val = body.get("balance_minor");
        if (val == null) return ResponseEntity.badRequest().build();
        BigDecimal newBalance;
        try {
            newBalance = new BigDecimal(val.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        boolean ok = walletService.setBalance(accountId, newBalance);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
