package com.smartparking.accounts_service.web;

import com.smartparking.accounts_service.client.CustomerClient;
import com.smartparking.accounts_service.dto.EmailVerificationEvent;
import com.smartparking.accounts_service.messaging.EmailEventPublisher;
import com.smartparking.accounts_service.model.VerificationToken;
import com.smartparking.accounts_service.service.AccountService;
import com.smartparking.accounts_service.service.VerificationTokenService;
import com.smartparking.accounts_service.security.JwtUtil;
import com.smartparking.accounts_service.web.dto.AuthRequest;
import com.smartparking.accounts_service.web.dto.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AccountService svc;
    private final JwtUtil jwtUtil;
    private final VerificationTokenService tokenService;
    private final EmailEventPublisher emailPublisher;
    private final CustomerClient customerClient;
    private final String verificationBaseUrl;

    public AuthController(AccountService svc, JwtUtil jwtUtil, VerificationTokenService tokenService,
                         EmailEventPublisher emailPublisher, CustomerClient customerClient,
                         @Value("${verification.base-url:http://localhost:3000}") String verificationBaseUrl) {
        this.svc = svc;
        this.jwtUtil = jwtUtil;
        this.tokenService = tokenService;
        this.emailPublisher = emailPublisher;
        this.customerClient = customerClient;
        this.verificationBaseUrl = verificationBaseUrl;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@RequestBody AuthRequest req) {
        log.info("Registration attempt for username: {}", req.getUsername());
        
        // Check if account already exists
        if (svc.findByUsername(req.getUsername()).isPresent()) {
            log.warn("Registration failed: username already exists: {}", req.getUsername());
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }
        
        // Create inactive account
        var acct = svc.register(req.getUsername(), req.getPassword());
        
        // Create customer record with firstName and lastName if provided
        if (req.getFirstName() != null || req.getLastName() != null) {
            try {
                customerClient.createCustomer(acct.getId(), 
                    req.getFirstName() != null ? req.getFirstName() : "", 
                    req.getLastName() != null ? req.getLastName() : "");
                log.info("Customer record created for accountId={} with firstName={}, lastName={}", 
                    acct.getId(), req.getFirstName(), req.getLastName());
            } catch (Exception e) {
                // Log error but don't fail registration - customer can be created later via lazy creation
                log.warn("Failed to create customer record during registration for accountId={}: {}", 
                    acct.getId(), e.getMessage());
            }
        }
        
        // Generate verification token
        VerificationToken token = tokenService.generateToken(acct.getId());
        String tokenStr = token.getToken();
        String tokenPreview = tokenStr.length() > 16 
            ? tokenStr.substring(0, 8) + "..." + tokenStr.substring(tokenStr.length() - 8)
            : "***";
        log.info("Generated verification token for accountId={}, tokenId={}, tokenPreview={}", 
            acct.getId(), token.getId(), tokenPreview);
        
        // Verify token was saved to database before publishing to RabbitMQ
        Optional<VerificationToken> savedToken = tokenService.validateToken(tokenStr);
        if (savedToken.isEmpty()) {
            log.error("Token was not saved to database! accountId={}, tokenPreview={}", acct.getId(), tokenPreview);
            throw new RuntimeException("Failed to save verification token");
        }
        log.debug("Token verified in database: tokenId={}", token.getId());
        
        // Build verification URL
        String verificationUrl = verificationBaseUrl + "/verification.html?token=" + tokenStr;
        
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
            log.error("Failed to register user: {} - RabbitMQ publish failed", req.getUsername(), e);
            throw new RuntimeException("Failed to publish verification email event. Registration rolled back.", e);
        }
        
        log.info("User registered successfully: accountId={}", acct.getId());
        return ResponseEntity.status(201).body(Map.of(
                "message", "Verification email sent. Please check your email to activate your account.",
                "accountId", acct.getId()
        ));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token) {
        String tokenPreview = token.length() > 16 
            ? token.substring(0, 8) + "..." + token.substring(token.length() - 8)
            : "***";
        log.info("Verification attempt for token: {}", tokenPreview);
        
        // validateToken() already checks if token exists, is not expired, and is not used
        Optional<VerificationToken> tokenOpt = tokenService.validateToken(token);
        
        if (tokenOpt.isEmpty()) {
            log.warn("Token validation failed: token={}, reason=not found or invalid", tokenPreview);
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
        log.info("Resend verification request for email: {}", email);
        
        if (email == null || email.isBlank()) {
            log.warn("Resend verification failed: email is required");
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        
        // Find account by email
        var accountOpt = svc.findByUsername(email);
        if (accountOpt.isEmpty()) {
            // Don't reveal if account exists for security
            log.info("Resend verification: account not found for email: {}", email);
            return ResponseEntity.ok(Map.of("message", "If an account exists with this email, a verification email will be sent."));
        }
        
        var account = accountOpt.get();
        log.info("Resend verification: found account accountId={}, isActive={}", account.getId(), account.getActive());
        
        // Check if account is already activated
        if (Boolean.TRUE.equals(account.getActive())) {
            log.warn("Resend verification failed: account already activated accountId={}", account.getId());
            return ResponseEntity.badRequest().body(Map.of("error", "Account is already activated"));
        }
        
        // Mark old tokens as used before generating new one
        var oldTokens = tokenService.findByAccountId(account.getId());
        int oldTokensCount = oldTokens.size();
        for (VerificationToken oldToken : oldTokens) {
            if (!Boolean.TRUE.equals(oldToken.getIsUsed())) {
                tokenService.markAsUsed(oldToken.getId());
                log.debug("Marked old token as used: tokenId={}, accountId={}", oldToken.getId(), account.getId());
            }
        }
        if (oldTokensCount > 0) {
            log.info("Marked {} old token(s) as used for accountId={}", oldTokensCount, account.getId());
        }
        
        // Generate new verification token
        VerificationToken token = tokenService.generateToken(account.getId());
        String tokenStr = token.getToken();
        String tokenPreview = tokenStr.length() > 16 
            ? tokenStr.substring(0, 8) + "..." + tokenStr.substring(tokenStr.length() - 8)
            : "***";
        log.info("Generated new verification token for resend: accountId={}, tokenId={}, tokenPreview={}", 
            account.getId(), token.getId(), tokenPreview);
        
        // Verify token was saved
        Optional<VerificationToken> savedToken = tokenService.validateToken(tokenStr);
        if (savedToken.isEmpty()) {
            log.error("Token was not saved to database during resend! accountId={}, tokenPreview={}", account.getId(), tokenPreview);
            throw new RuntimeException("Failed to save verification token");
        }
        
        // Build verification URL
        String verificationUrl = verificationBaseUrl + "/verification.html?token=" + tokenStr;
        
        // Publish email verification event
        try {
            EmailVerificationEvent event = new EmailVerificationEvent(
                    account.getUsername(),
                    tokenStr,
                    account.getId(),
                    verificationUrl
            );
            emailPublisher.publishVerificationEmail(event);
            log.info("Resend verification email sent successfully: accountId={}, tokenPreview={}", account.getId(), tokenPreview);
        } catch (Exception e) {
            log.error("Failed to publish verification email event during resend: accountId={}, tokenPreview={}", 
                account.getId(), tokenPreview, e);
            throw new RuntimeException("Failed to publish verification email event", e);
        }
        
        return ResponseEntity.ok(Map.of("message", "Verification email sent. Please check your email."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req, HttpServletResponse response) {
        try {
            boolean ok = svc.verify(req.getUsername(), req.getPassword());
            if (!ok) return ResponseEntity.status(401).build();
            var acc = svc.findByUsername(req.getUsername()).get();
            String token = jwtUtil.generateToken(String.valueOf(acc.getId()), acc.getRole());
            
            // Set HttpOnly cookie for secure server-side authentication
            ResponseCookie cookie = ResponseCookie.from("authToken", token)
                    .httpOnly(true)
                    .secure(false) // Set to true in production with HTTPS
                    .path("/")
                    .maxAge(86400) // 24 hours (match JWT expiration)
                    .sameSite("Lax") // CSRF protection
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            
            log.info("Login successful for accountId={}, cookie set", acc.getId());
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (AccountService.AccountNotActivatedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Clear the authToken cookie by setting it to expire immediately
        ResponseCookie cookie = ResponseCookie.from("authToken", "")
                .httpOnly(true)
                .secure(false) // Match login cookie settings
                .path("/")
                .maxAge(0) // Expire immediately
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        
        log.info("Logout successful, cookie cleared");
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}

