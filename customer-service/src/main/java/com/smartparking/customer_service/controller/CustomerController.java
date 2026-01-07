package com.smartparking.customer_service.controller;

import com.smartparking.customer_service.service.WalletService;
import com.smartparking.customer_service.security.RequestContext;
import com.smartparking.customer_service.service.ReservationService;
import com.smartparking.customer_service.service.CustomerProfileService;
import com.smartparking.customer_service.service.VehicleService;
import com.smartparking.customer_service.service.TopUpService;
import com.smartparking.customer_service.dto.TopUpRequest;
import com.smartparking.customer_service.dto.TopUpResult;
import com.smartparking.customer_service.model.Customer;
import com.smartparking.customer_service.client.AccountClient;
import com.smartparking.customer_service.dto.ContactFormEvent;
import com.smartparking.customer_service.messaging.ContactFormEventPublisher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/customer")
public class CustomerController {
    private final CustomerProfileService profiles;
    private final VehicleService vehicles;
    private final ReservationService reservations;
    private final WalletService walletService;
    private final AccountClient accountClient;
    private final ContactFormEventPublisher contactFormEventPublisher;
    private final TopUpService topUpService;

    public CustomerController(CustomerProfileService profiles, VehicleService vehicles, ReservationService reservations, WalletService walletService, AccountClient accountClient, ContactFormEventPublisher contactFormEventPublisher, TopUpService topUpService) {
        this.profiles = profiles;
        this.vehicles = vehicles;
        this.reservations = reservations;
        this.walletService = walletService;
        this.accountClient = accountClient;
        this.contactFormEventPublisher = contactFormEventPublisher;
        this.topUpService = topUpService;
    }

    private Long requireAccountId(RequestContext ctx) {
        if (ctx == null || ctx.getUsername() == null || ctx.getUsername().isBlank()) {
            throw new IllegalArgumentException("Invalid request context: username is required");
        }
        try {
            Long accountId = Long.parseLong(ctx.getUsername());
            if (accountId <= 0) {
                throw new IllegalArgumentException("Invalid account ID: must be positive, got: " + accountId);
            }
            return accountId;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid account ID format: " + ctx.getUsername(), e);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        Long accountId = requireAccountId(ctx);
        Optional<Map<String, Object>> profile = profiles.getById(accountId);
        if (profile.isEmpty()) {
            // Lazy creation: utwórz customer jeśli nie istnieje
            profiles.createForAccountId(accountId);
            profile = profiles.getById(accountId); // Pobierz nowo utworzony rekord
        }
        return profile.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(HttpServletRequest request,
                                           @RequestParam String firstName,
                                           @RequestParam String lastName) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        boolean ok = profiles.updateName(requireAccountId(ctx), firstName, lastName);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<Map<String, Object>>> listVehicles(HttpServletRequest request) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(vehicles.list(requireAccountId(ctx)));
    }

    @PostMapping("/vehicles")
    public ResponseEntity<Map<String, Object>> addVehicle(HttpServletRequest request,
                                                          @RequestParam String licencePlate) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        long id = vehicles.add(requireAccountId(ctx), licencePlate);
        return ResponseEntity.ok(Map.of("id", id));
    }

    @PutMapping("/vehicles/{vehicleId}")
    public ResponseEntity<?> updateVehicle(HttpServletRequest request,
                                          @PathVariable Long vehicleId,
                                          @RequestParam String licencePlate) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        boolean updated = vehicles.update(vehicleId, requireAccountId(ctx), licencePlate);
        return updated ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/vehicles/{vehicleId}")
    public ResponseEntity<?> deleteVehicle(HttpServletRequest request,
                                          @PathVariable Long vehicleId) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        boolean deleted = vehicles.delete(vehicleId, requireAccountId(ctx));
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/wallet")
    public ResponseEntity<?> getWallet(HttpServletRequest request) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        Long accountId = requireAccountId(ctx);
        Optional<Map<String, Object>> wallet = walletService.getByAccountId(accountId);
        if (wallet.isEmpty()) {
            // Lazy creation: utwórz customer i wallet jeśli nie istnieją
            walletService.createForAccountId(accountId);
            wallet = walletService.getByAccountId(accountId); // Pobierz nowo utworzony rekord
        }
        return wallet.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/wallet/topup")
    public ResponseEntity<?> topUp(HttpServletRequest request, @RequestBody TopUpRequest req) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        
        if (req.getAmountMinor() == null || req.getAmountMinor() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid amount"));
        }
        
        Long accountId = requireAccountId(ctx);
        TopUpResult result = topUpService.processTopUp(accountId, req.getAmountMinor(), req.getPaymentMethod());
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                "paymentId", result.getPaymentId(),
                "newBalance", result.getNewBalance()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", result.getErrorMessage()));
        }
    }

    @GetMapping("/reservations")
    public ResponseEntity<List<Map<String, Object>>> listReservations(HttpServletRequest request) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(reservations.list(requireAccountId(ctx)));
    }

    @PostMapping("/reservations")
    public ResponseEntity<Map<String, Object>> createReservation(HttpServletRequest request,
                                                                 @RequestParam Long parkingId,
                                                                 @RequestParam Long spotId,
                                                                 @RequestParam(required = false) String startDateTime,
                                                                 @RequestParam(required = false) Long durationSeconds) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        
        Instant start;
        if (startDateTime != null && !startDateTime.isBlank()) {
            try {
                // Parse ISO-8601 format: "2026-01-07T14:00:00"
                // URLSearchParams may encode ':' as '%3A', so decode first
                String decodedDateTime = java.net.URLDecoder.decode(startDateTime, java.nio.charset.StandardCharsets.UTF_8);
                start = Instant.parse(decodedDateTime);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid date/time format: " + e.getMessage()));
            }
        } else {
            start = Instant.now();
        }
        
        Instant end = start.plusSeconds(durationSeconds != null ? durationSeconds : 7200); // Default 2 hours
        
        com.smartparking.customer_service.dto.ReservationResult result = reservations.create(
            requireAccountId(ctx), parkingId, spotId, start, end
        );
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of("id", result.getReservationId()));
        } else {
            String errorMessage = result.getErrorMessage();
            if (errorMessage != null && errorMessage.contains("Insufficient")) {
                return ResponseEntity.status(402).body(Map.of("error", errorMessage));
            }
            return ResponseEntity.badRequest().body(Map.of("error", errorMessage != null ? errorMessage : "Failed to create reservation"));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(HttpServletRequest request,
                                           @RequestBody Map<String, String> body) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        
        Long accountId = requireAccountId(ctx);
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");
        
        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "currentPassword and newPassword are required"));
        }
        
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 8 characters long"));
        }
        
        try {
            boolean success = accountClient.changePassword(accountId, currentPassword, newPassword);
            if (!success) {
                return ResponseEntity.status(401).body(Map.of("error", "Current password is incorrect"));
            }
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to change password: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(HttpServletRequest request) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        // Note: History requires implementation - may need to query parking-service
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/history/statistics")
    public ResponseEntity<?> getHistoryStatistics(HttpServletRequest request) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        // Note: Statistics requires implementation
        return ResponseEntity.ok(Map.of(
                "totalSessions", 0,
                "totalTime", 0,
                "totalSpent", 0
        ));
    }

    @PostMapping("/history/{sessionId}/pay")
    public ResponseEntity<?> payForSession(HttpServletRequest request,
                                          @PathVariable Long sessionId) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        // Note: Payment requires implementation - may need to communicate with payment-service
        return ResponseEntity.status(501).body(Map.of("error", "Payment not yet implemented"));
    }

    @PostMapping("/internal/create")
    public ResponseEntity<?> createCustomer(@RequestParam Long accountId,
                                            @RequestParam(required = false) String firstName,
                                            @RequestParam(required = false) String lastName) {
        // Internal endpoint for accounts-service to create customer during registration
        // No authentication required - should be called only from within the service mesh
        try {
            Customer customer = profiles.createForAccountId(accountId, 
                firstName != null ? firstName : "", 
                lastName != null ? lastName : "");
            return ResponseEntity.ok(Map.of("id", customer.getId(), "accountId", customer.getRefAccountId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to create customer: " + e.getMessage()));
        }
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(HttpServletRequest request) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        if (ctx == null) return ResponseEntity.status(401).build();
        
        Long accountId = requireAccountId(ctx);
        
        try {
            // Delete account in accounts-service (this will cascade to customer records if configured)
            boolean success = accountClient.deleteAccount(accountId);
            if (!success) {
                return ResponseEntity.status(404).body(Map.of("error", "Account not found"));
            }
            return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete account: " + e.getMessage()));
        }
    }

    @PostMapping("/contact")
    public ResponseEntity<?> submitContactForm(HttpServletRequest request, @RequestBody Map<String, String> body) {
        RequestContext ctx = (RequestContext) request.getAttribute("requestContext");
        Long accountId = null;
        String userEmail = null;
        String userName = null;
        
        // Try to get account info if user is authenticated
        if (ctx != null) {
            try {
                accountId = requireAccountId(ctx);
                // Get user email from account service
                userEmail = accountClient.getEmailByAccountId(accountId).orElse(null);
                // Get user name from customer profile
                Optional<Map<String, Object>> profileOpt = profiles.getById(accountId);
                if (profileOpt.isPresent()) {
                    Map<String, Object> profile = profileOpt.get();
                    String firstName = (String) profile.get("firstName");
                    String lastName = (String) profile.get("lastName");
                    if (firstName != null && lastName != null && !firstName.isBlank() && !lastName.isBlank()) {
                        userName = firstName + " " + lastName;
                    } else if (firstName != null && !firstName.isBlank()) {
                        userName = firstName;
                    } else if (lastName != null && !lastName.isBlank()) {
                        userName = lastName;
                    }
                }
            } catch (Exception e) {
                // If we can't get account info, continue with form data
            }
        }
        
        // Override with form data if provided
        String formEmail = body.get("email");
        String formName = body.get("name");
        String subject = body.get("subject");
        String message = body.get("message");
        
        if (formEmail != null && !formEmail.isBlank()) {
            userEmail = formEmail;
        }
        if (formName != null && !formName.isBlank()) {
            userName = formName;
        }
        
        // Validate required fields
        if (userEmail == null || userEmail.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is required"));
        }
        
        // Default values
        if (userName == null || userName.isBlank()) {
            userName = "User";
        }
        if (subject == null || subject.isBlank()) {
            subject = "Contact Form Submission";
        }
        
        try {
            ContactFormEvent event = new ContactFormEvent(userEmail, userName, subject, message, accountId);
            contactFormEventPublisher.publishContactForm(event);
            return ResponseEntity.ok(Map.of("message", "Contact form submitted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to submit contact form: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("customer-service: OK");
    }
}
