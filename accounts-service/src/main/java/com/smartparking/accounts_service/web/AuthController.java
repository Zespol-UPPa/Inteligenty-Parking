package com.smartparking.accounts_service.web;

import com.smartparking.accounts_service.client.AdminClient;
import com.smartparking.accounts_service.client.CustomerClient;
import com.smartparking.accounts_service.client.WorkerClient;
import com.smartparking.accounts_service.dto.EmailVerificationEvent;
import com.smartparking.accounts_service.messaging.EmailEventPublisher;
import com.smartparking.accounts_service.model.Account;
import com.smartparking.accounts_service.model.LoginCode;
import com.smartparking.accounts_service.model.VerificationToken;
import com.smartparking.accounts_service.repo.AccountRepository;
import com.smartparking.accounts_service.repo.LoginCodeRepository;
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
    private final WorkerClient workerClient;
    private final AdminClient adminClient;
    private final LoginCodeRepository loginCodeRepo;
    private final String verificationBaseUrl;




    public AuthController(AccountService svc, JwtUtil jwtUtil, VerificationTokenService tokenService,
                          EmailEventPublisher emailPublisher, CustomerClient customerClient, WorkerClient workerClient,
                          AdminClient adminClient, LoginCodeRepository loginCodeRepository,
                          @Value("${verification.base-url:http://localhost:3000}") String verificationBaseUrl) {
        this.svc = svc;
        this.jwtUtil = jwtUtil;
        this.tokenService = tokenService;
        this.emailPublisher = emailPublisher;
        this.customerClient = customerClient;
        this.workerClient=workerClient;
        this.adminClient= adminClient;
        this.loginCodeRepo = loginCodeRepository;
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



    /*
    =============================POSTINGI DO WORKER APP==================
     */

    @PostMapping("/login/staff")

    public ResponseEntity<?> loginStaff(@RequestBody AuthRequest req, HttpServletResponse response) {

        try {
            // Parse username as account_id
            Long accountId;
            try {
                accountId = Long.parseLong(req.getUsername());
            } catch (NumberFormatException e) {
                log.warn("Invalid account ID format: {}", req.getUsername());
                return ResponseEntity.status(401).build();
            }
            // Find account by ID
            Optional<Account> accountOpt = svc.findById(accountId);
            if (accountOpt.isEmpty()) {
                log.warn("Account not found: accountId={}", accountId);
                return ResponseEntity.status(401).build();
            }
            Account acc = accountOpt.get();
            // Check if Worker or Admin
            if (!acc.getRole().equals("Worker") && !acc.getRole().equals("Admin")) {
                log.warn("Login attempt from non-staff account: accountId={}, role={}",
                        accountId, acc.getRole());
                return ResponseEntity.status(403)
                        .body(Map.of("error", "This endpoint is for staff only"));
            }
            // Check if active
            if (!acc.getActive()) {
                log.warn("Login attempt with inactive account: accountId={}", accountId);
                throw new AccountService.AccountNotActivatedException(
                        "Account not activated. Please use your login code."
                );
            }
            // Verify password
            if (!svc.verifyPassword(acc, req.getPassword())) {
                log.warn("Invalid password for accountId={}", accountId);
                return ResponseEntity.status(401).build();
            }

            String token = jwtUtil.generateToken(String.valueOf(acc.getId()), acc.getRole());
            ResponseCookie cookie = ResponseCookie.from("authToken", token)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(86400)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            log.info("Staff login successful: accountId={}, role={}", acc.getId(), acc.getRole());
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (AccountService.AccountNotActivatedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }

    }



    @GetMapping("/activation-info")

    public ResponseEntity<?> getActivationInfo(@RequestParam String code) {
        log.info("Activation info request for code: {}", code);

        try {
            // Validate login_code
            Optional<LoginCode> codeOpt = loginCodeRepo.findValidByCode(code);
            if (codeOpt.isEmpty()) {
                log.warn("Invalid or used login code: {}", code);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid or already used activation code"));
            }

            Long accountId = codeOpt.get().getAccountId();

            // Get Account
            Optional<Account> accountOpt = svc.findById(accountId);
            if (accountOpt.isEmpty()) {
                log.error("Account not found for login code: accountId={}", accountId);
                return ResponseEntity.badRequest().body(Map.of("error", "Account not found"));
            }

            Account account = accountOpt.get();
            String role = account.getRole();

            // Get personal data from Worker/Admin Service
            Map<String, Object> personalData;
            if ("Worker".equalsIgnoreCase(role)) {
                personalData = workerClient.getWorkerByAccountId(accountId);
            } else if ("Admin".equalsIgnoreCase(role)) {
                personalData = adminClient.getAdminByAccountId(accountId);
            } else {
                log.error("Invalid role for activation: accountId={}, role={}", accountId, role);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid account role"));

            }

            // Build response
            Map<String, Object> response = Map.of(
                    "accountId", accountId,
                    "email", account.getUsername(),
                    "firstName", personalData.getOrDefault("firstName", ""),
                    "lastName", personalData.getOrDefault("lastName", ""),
                    "phoneNumber", personalData.getOrDefault("phoneNumber", ""),
                    "peselNumber", personalData.getOrDefault("peselNumber", ""),
                    "role", role,
                    "parkingName", personalData.getOrDefault("parkingName", ""),
                    "companyName", personalData.getOrDefault("companyName", "")
            );

            log.info("Activation info retrieved: accountId={}, role={}", accountId, role);
            return ResponseEntity.ok(response);



        } catch (Exception e) {
            log.error("Failed to get activation info", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to retrieve account data"));

        }

    }



    @PostMapping("/activate/worker")
    @Transactional
    public ResponseEntity<?> activateWorker(@RequestBody Map<String, String> req) {
        String code = req.get("code");
        String password = req.get("password");
        String confirmPassword = req.get("confirmPassword");
        String firstName = req.get("firstName");
        String lastName = req.get("lastName");
        String phoneNumber = req.get("phoneNumber");
        String peselNumber = req.get("peselNumber");

        log.info("Worker activation attempt with code: {}", code);

        // Validation
        if (password == null || password.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Password must be at least 8 characters"));
        }
        if (!password.equals(confirmPassword)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Passwords do not match"));
        }

        // Validate login_code
        Optional<LoginCode> codeOpt = loginCodeRepo.findValidByCode(code);

        if (codeOpt.isEmpty()) {
            log.warn("Invalid or used activation code: {}", code);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid or already used activation code"));
        }

        LoginCode loginCode = codeOpt.get();
        Long accountId = loginCode.getAccountId();

        // Get Account
        Optional<Account> accountOpt = svc.findById(accountId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Account not found"));
        }
        Account account = accountOpt.get();

        // Check role
        if (!account.getRole().equals("Worker")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "This activation is for worker accounts only"));
        }

        // Check if already activated
        if (Boolean.TRUE.equals(account.getActive())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Account already activated"));
        }

        // CRITICAL FIX: Update personal data BEFORE activating and marking code as used
        // This way if update fails, transaction rolls back and nothing is committed
        try {
            boolean updated = workerClient.updateWorkerPersonalData(
                    accountId, firstName, lastName, phoneNumber, peselNumber
            );

            if (!updated) {
                log.error("Failed to update worker personal data: accountId={}", accountId);
                throw new RuntimeException("Failed to update worker personal data");
            }

            // Only after successful update, activate account and mark code as used
            svc.setPasswordAndActivate(accountId, password);
            loginCodeRepo.markUsed(loginCode);

            log.info("Worker activated successfully: accountId={}", accountId);
            return ResponseEntity.ok(Map.of("message", "Worker account activated successfully"));

        } catch (Exception e) {
            // Transaction will rollback - code won't be marked as used, password won't be saved
            log.error("Worker activation error for accountId={}: {}", accountId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Activation failed. Please try again."));
        }
    }



    @PostMapping("/activate/admin")
    @Transactional
    public ResponseEntity<?> activateAdmin(@RequestBody Map<String, String> req) {

        String code = req.get("code");
        String password = req.get("password");
        String confirmPassword = req.get("confirmPassword");
        String firstName = req.get("firstName");
        String lastName = req.get("lastName");
        String phoneNumber = req.get("phoneNumber");
        String peselNumber = req.get("peselNumber");

        log.info("Admin activation attempt with code: {}", code);

        // Validation
        if (password == null || password.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Password must be at least 8 characters"));
        }
        if (!password.equals(confirmPassword)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Passwords do not match"));
        }

        // Validate login_code
        Optional<LoginCode> codeOpt = loginCodeRepo.findValidByCode(code);
        if (codeOpt.isEmpty()) {
            log.warn("Invalid or used activation code: {}", code);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid or already used activation code"));
        }
        LoginCode loginCode = codeOpt.get();
        Long accountId = loginCode.getAccountId();

        // Get Account
        Optional<Account> accountOpt = svc.findById(accountId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Account not found"));
        }

        Account account = accountOpt.get();

        // Check role
        if (!account.getRole().equals("Admin")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "This activation is for admin accounts only"));
        }

        // Check if already activated
        if (Boolean.TRUE.equals(account.getActive())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Account already activated"));
        }

        // CRITICAL FIX: Update personal data BEFORE activating and marking code as used
        try {
            boolean updated = adminClient.updateAdminPersonalData(
                    accountId, firstName, lastName, phoneNumber, peselNumber
            );

            if (!updated) {
                log.error("Failed to update admin personal data: accountId={}", accountId);
                throw new RuntimeException("Failed to update admin personal data");
            }

            // Only after successful update, activate account and mark code as used
            svc.setPasswordAndActivate(accountId, password);
            loginCodeRepo.markUsed(loginCode);

            log.info("Admin activated successfully: accountId={}", accountId);
            return ResponseEntity.ok(Map.of("message", "Admin account activated successfully"));

        } catch (Exception e) {
            // Transaction will rollback - code won't be marked as used, password won't be saved
            log.error("Admin activation error for accountId={}: {}", accountId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Activation failed. Please try again."));
        }
    }

}

