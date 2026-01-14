package com.smartparking.worker_service.controller;

import com.smartparking.worker_service.model.Worker;
import com.smartparking.worker_service.client.AccountClient;
import com.smartparking.worker_service.client.ParkingClient;
import com.smartparking.worker_service.repo.WorkerRepository;
import com.smartparking.worker_service.service.WorkerFinancialReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/worker")
public class WorkerController {
    private static final Logger log = LoggerFactory.getLogger(WorkerController.class);

    private final WorkerRepository workerRepository;
    private final ParkingClient parkingClient;
    private final AccountClient accountClient;
    private final WorkerFinancialReportService financialReportService; // ✅ DODANE

    public WorkerController(
            ParkingClient parkingClient,
            AccountClient accountClient,
            WorkerRepository workerRepository,
            WorkerFinancialReportService financialReportService) { // ✅ DODANE
        this.parkingClient = parkingClient;
        this.accountClient = accountClient;
        this.workerRepository = workerRepository;
        this.financialReportService = financialReportService; // ✅ DODANE
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    /**
     * GET /worker/profile
     * Returns current worker's profile
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("X-Account-Id") Long accountId) {
        log.info("========================================");
        log.info("GET /worker/profile called");
        log.info("X-Account-Id header: {}", accountId);

        try {
            log.info("Searching for worker with accountId: {}", accountId);
            Optional<Worker> workerOpt = workerRepository.findByAccountId(accountId);

            if (workerOpt.isEmpty()) {
                log.warn("Worker not found for accountId: {}", accountId);
                return ResponseEntity.status(404)
                        .body(Map.of("error", "Worker profile not found"));
            }

            Worker worker = workerOpt.get();
            log.info("Worker found: id={}, firstName={}, lastName={}",
                    worker.getId(), worker.getFirstName(), worker.getLastName());

            // Get email from accounts-service
            log.info("Fetching email from accounts-service...");
            Optional<String> emailOpt = accountClient.getEmailByAccountId(accountId);
            log.info("Email fetched: {}", emailOpt.orElse("(empty)"));

            Map<String, Object> response = new HashMap<>();
            response.put("accountId", accountId);
            response.put("workerId", worker.getId());
            response.put("firstName", worker.getFirstName() != null ? worker.getFirstName() : "");
            response.put("lastName", worker.getLastName() != null ? worker.getLastName() : "");
            response.put("phoneNumber", worker.getPhoneNumber() != null ? worker.getPhoneNumber() : "");
            response.put("email", emailOpt.orElse(""));
            response.put("peselNumber", worker.getPeselNumber() != null ? worker.getPeselNumber() : "");
            response.put("companyId", worker.getRefCompanyId());
            response.put("role", "WORKER");
            response.put("active", true);

            log.info("Profile retrieved successfully");
            log.info("========================================");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get worker profile", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to retrieve profile: " + e.getMessage()));
        }
    }

    /**
     * PUT /worker/profile
     * Updates worker's personal information
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("X-Account-Id") Long accountId,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String phoneNumber) {

        try {
            log.info("Updating profile for worker accountId: {}", accountId);

            Optional<Worker> workerOpt = workerRepository.findByAccountId(accountId);

            if (workerOpt.isEmpty()) {
                log.warn("Worker not found for accountId: {}", accountId);
                return ResponseEntity.status(404)
                        .body(Map.of("error", "Worker profile not found"));
            }

            Worker worker = workerOpt.get();

            // Update personal data (keep peselNumber unchanged)
            workerRepository.updatePersonalData(
                    accountId,
                    firstName,
                    lastName,
                    phoneNumber,
                    worker.getPeselNumber()
            );

            log.info("Profile updated successfully for worker: {}", worker.getId());
            return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));

        } catch (Exception e) {
            log.error("Failed to update worker profile", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to update profile: " + e.getMessage()));
        }
    }

    /**
     * PUT /worker/password
     * Changes worker's password
     */
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("X-Account-Id") Long accountId,
            @RequestBody Map<String, String> request) {

        try {
            String currentPassword = request.get("currentPassword");
            String newPassword = request.get("newPassword");

            log.info("Password change requested for worker accountId: {}", accountId);

            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Current password and new password are required"));
            }

            if (newPassword.length() < 8) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "New password must be at least 8 characters long"));
            }

            boolean success = accountClient.changePassword(accountId, currentPassword, newPassword);

            if (!success) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Current password is incorrect"));
            }

            log.info("Password changed successfully for worker accountId: {}", accountId);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));

        } catch (Exception e) {
            log.error("Failed to change password", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to change password: " + e.getMessage()));
        }
    }

    // ================================================
    // FINANCIAL REPORTS (worker's parking only)
    // ================================================

    /**
     * GET /worker/reports/financial/summary
     * Get financial summary for worker's assigned parking
     */
    @GetMapping("/reports/financial/summary")
    public ResponseEntity<Map<String, Object>> getFinancialSummary(
            @RequestHeader("X-Account-Id") Long accountId,
            @RequestParam String period) {

        log.info("GET /worker/reports/financial/summary - accountId: {}, period: {}", accountId, period);

        try {
            Map<String, Object> summary = financialReportService.getFinancialSummary(accountId, period);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error getting financial summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /worker/reports/financial/revenue-over-time
     * Get revenue over time for charts
     */
    @GetMapping("/reports/financial/revenue-over-time")
    public ResponseEntity<List<Map<String, Object>>> getRevenueOverTime(
            @RequestHeader("X-Account-Id") Long accountId,
            @RequestParam String period) {

        log.info("GET /worker/reports/financial/revenue-over-time - accountId: {}, period: {}", accountId, period);

        try {
            List<Map<String, Object>> data = financialReportService.getRevenueOverTime(accountId, period);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Error getting revenue over time", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /worker/reports/financial/transactions
     * Get detailed transactions list
     */
    @GetMapping("/reports/financial/transactions")
    public ResponseEntity<List<Map<String, Object>>> getTransactions(
            @RequestHeader("X-Account-Id") Long accountId,
            @RequestParam String period,
            @RequestParam(defaultValue = "all") String status) {

        log.info("GET /worker/reports/financial/transactions - accountId: {}, period: {}, status: {}",
                accountId, period, status);

        try {
            List<Map<String, Object>> transactions = financialReportService.getTransactions(accountId, period, status);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Error getting transactions", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}