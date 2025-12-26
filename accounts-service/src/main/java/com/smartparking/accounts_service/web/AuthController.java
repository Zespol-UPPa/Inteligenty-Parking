package com.smartparking.accounts_service.web;

import com.smartparking.accounts_service.dto.EmailVerificationEvent;
import com.smartparking.accounts_service.messaging.EmailEventPublisher;
import com.smartparking.accounts_service.model.VerificationToken;
import com.smartparking.accounts_service.service.AccountService;
import com.smartparking.accounts_service.service.VerificationTokenService;
import com.smartparking.accounts_service.security.JwtUtil;
import com.smartparking.accounts_service.web.dto.AuthRequest;
import com.smartparking.accounts_service.web.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountService svc;
    private final JwtUtil jwtUtil;
    private final VerificationTokenService tokenService;
    private final EmailEventPublisher emailPublisher;
    private final String verificationBaseUrl;

    public AuthController(AccountService svc, JwtUtil jwtUtil, VerificationTokenService tokenService,
                         EmailEventPublisher emailPublisher,
                         @Value("${verification.base-url:http://localhost:3000}") String verificationBaseUrl) {
        this.svc = svc;
        this.jwtUtil = jwtUtil;
        this.tokenService = tokenService;
        this.emailPublisher = emailPublisher;
        this.verificationBaseUrl = verificationBaseUrl;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@RequestBody AuthRequest req) {
        // Check if account already exists
        if (svc.findByUsername(req.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }
        
        // Create inactive account
        var acct = svc.register(req.getUsername(), req.getPassword());
        
        // Generate verification token
        VerificationToken token = tokenService.generateToken(acct.getId());
        
        // Build verification URL
        String verificationUrl = verificationBaseUrl + "/verification.html?token=" + token.getToken();
        
        // Publish email verification event
        // If RabbitMQ publish fails, transaction will rollback (account and token will be removed)
        try {
            EmailVerificationEvent event = new EmailVerificationEvent(
                    acct.getUsername(),
                    token.getToken(),
                    acct.getId(),
                    verificationUrl
            );
            emailPublisher.publishVerificationEmail(event);
        } catch (Exception e) {
            // If RabbitMQ publish fails, rollback transaction
            throw new RuntimeException("Failed to publish verification email event. Registration rolled back.", e);
        }
        
        return ResponseEntity.status(201).body(Map.of(
                "message", "Verification email sent. Please check your email to activate your account.",
                "accountId", acct.getId()
        ));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token) {
        // validateToken() already checks if token exists, is not expired, and is not used
        Optional<VerificationToken> tokenOpt = tokenService.validateToken(token);
        
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired verification token"));
        }
        
        VerificationToken verificationToken = tokenOpt.get();
        
        // Check if account is already activated
        var accountOpt = svc.findById(verificationToken.getAccountId());
        if (accountOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Account not found"));
        }
        
        if (Boolean.TRUE.equals(accountOpt.get().getActive())) {
            return ResponseEntity.ok(Map.of("message", "Account already activated"));
        }
        
        // Activate account
        svc.activateAccount(verificationToken.getAccountId());
        
        // Mark token as used
        tokenService.markAsUsed(verificationToken.getId());
        
        return ResponseEntity.ok(Map.of("message", "Account activated successfully"));
    }

    @PostMapping("/resend-verification")
    @Transactional
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        
        // Find account by email
        var accountOpt = svc.findByUsername(email);
        if (accountOpt.isEmpty()) {
            // Don't reveal if account exists for security
            return ResponseEntity.ok(Map.of("message", "If an account exists with this email, a verification email will be sent."));
        }
        
        var account = accountOpt.get();
        
        // Check if account is already activated
        if (Boolean.TRUE.equals(account.getActive())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Account is already activated"));
        }
        
        // Generate new verification token (or reuse existing valid one)
        VerificationToken token = tokenService.generateToken(account.getId());
        
        // Build verification URL
        String verificationUrl = verificationBaseUrl + "/verification.html?token=" + token.getToken();
        
        // Publish email verification event
        try {
            EmailVerificationEvent event = new EmailVerificationEvent(
                    account.getUsername(),
                    token.getToken(),
                    account.getId(),
                    verificationUrl
            );
            emailPublisher.publishVerificationEmail(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish verification email event", e);
        }
        
        return ResponseEntity.ok(Map.of("message", "Verification email sent. Please check your email."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        try {
            boolean ok = svc.verify(req.getUsername(), req.getPassword());
            if (!ok) return ResponseEntity.status(401).build();
            var acc = svc.findByUsername(req.getUsername()).get();
            String token = jwtUtil.generateToken(String.valueOf(acc.getId()), acc.getRole());
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (AccountService.AccountNotActivatedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}

