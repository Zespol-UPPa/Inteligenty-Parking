package com.smartparking.admin_service.controller;

import com.smartparking.admin_service.client.AccountClient;
import com.smartparking.admin_service.client.ParkingClient;
import com.smartparking.admin_service.client.WorkerClient;
import com.smartparking.admin_service.model.Admin;
import com.smartparking.admin_service.repo.AdminRepository;

import com.smartparking.admin_service.service.FinancialReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;


import com.smartparking.admin_service.dto.IdResponse;
import com.smartparking.admin_service.dto.ParkingUsageDto;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final ParkingClient parkingClient;
    private final AdminRepository adminRepository;
    private final AccountClient accountClient;
    private WorkerClient workerClient;
    private final FinancialReportService financialReportService;

    public AdminController(ParkingClient parkingClient,
                           AdminRepository adminRepository,
                           AccountClient accountClient, FinancialReportService financialReportService, WorkerClient workerClient) {
        this.parkingClient = parkingClient;
        this.adminRepository = adminRepository;
        this.accountClient = accountClient;
        this.workerClient = workerClient;
        this.financialReportService = financialReportService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/locations")
    public ResponseEntity<IdResponse> addLocation(@RequestParam String name,
                                                  @RequestParam String address,
                                                  @RequestParam Long companyId) {
        long id = parkingClient.createLocation(name, address, companyId);
        return ResponseEntity.ok(new IdResponse(id));
    }

    @PostMapping("/spots")
    public ResponseEntity<IdResponse> addSpot(@RequestParam Long locationId,
                                              @RequestParam String code,
                                              @RequestParam Integer floorLvl,
                                              @RequestParam(defaultValue = "false") boolean toReserved,
                                              @RequestParam(defaultValue = "Available") String type) {
        long id = parkingClient.createSpot(locationId, code, floorLvl, toReserved, type);
        return ResponseEntity.ok(new IdResponse(id));
    }

    @GetMapping("/reports/usage")
    public ResponseEntity<List<ParkingUsageDto>> usageReport() {
        return ResponseEntity.ok(parkingClient.usageReport());
    }

    /**================================================
     * PROFILE MANAGEMENT
     ================================================ */

    /**
     * GET /admin/profile
     * Returns current admin's profile
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("X-Account-Id") Long accountId) {
        log.info("GET /admin/profile called");

        try {
            Optional<Admin> adminOpt = adminRepository.findByAccountId(accountId);

            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Admin not found"));
            }

            Admin admin = adminOpt.get();

            Optional<String> emailOpt = accountClient.getEmailByAccountId(accountId);

            Map<String, Object> response = new HashMap<>();
            response.put("accountId", accountId);
            response.put("firstName", admin.getFirstName() != null ? admin.getFirstName() : "");
            response.put("lastName", admin.getLastName() != null ? admin.getLastName() : "");
            response.put("phoneNumber", admin.getPhoneNumber() != null ? admin.getPhoneNumber() : "");
            response.put("email", emailOpt.orElse(""));
            response.put("peselNumber", admin.getPeselNumber() != null ? admin.getPeselNumber() : "");
            response.put("companyId", admin.getRefCompanyId());
            response.put("role", "ADMIN");
            response.put("active", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /admin/profile
     * Updates admin's personal information
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("X-Account-Id") Long accountId,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String phoneNumber) {

        try {
            log.info("Updating profile for admin accountId: {}", accountId);

            Optional<Admin> adminOpt = adminRepository.findByAccountId(accountId);

            if (adminOpt.isEmpty()) {
                log.warn("Admin not found for accountId: {}", accountId);
                return ResponseEntity.status(404)
                        .body(Map.of("error", "Admin profile not found"));
            }

            Admin admin = adminOpt.get();

            // Update personal data (keep peselNumber unchanged)
            adminRepository.updatePersonalData(
                    accountId,
                    firstName,
                    lastName,
                    phoneNumber,
                    admin.getPeselNumber()
            );

            log.info("Profile updated successfully for admin: {}", admin.getId());
            return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));

        } catch (Exception e) {
            log.error("Failed to update admin profile", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to update profile: " + e.getMessage()));
        }
    }

    /**
     * PUT /admin/password
     * Changes admin's password
     */
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("X-Account-Id") Long accountId,
            @RequestBody Map<String, String> request) {

        try {
            String currentPassword = request.get("currentPassword");
            String newPassword = request.get("newPassword");

            log.info("Password change requested for admin accountId: {}", accountId);

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

            log.info("Password changed successfully for admin accountId: {}", accountId);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));

        } catch (Exception e) {
            log.error("Failed to change password", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to change password: " + e.getMessage()));
        }
    }

    /**================================================
     * PARKING MANAGEMENT
     ================================================ */

    /**
     * GET /admin/parkings
     * Returns list of parking IDs for admin's company
     */
    @GetMapping("/parkings")
    public ResponseEntity<List<Long>> getCompanyParkings(@RequestHeader("X-Account-Id") Long accountId) {
        log.info("Getting parkings for admin accountId: {}", accountId);

        try {
            // Get admin to find their company
            Optional<Admin> adminOpt = adminRepository.findByAccountId(accountId);

            if (adminOpt.isEmpty()) {
                log.warn("Admin not found for accountId: {}", accountId);
                return ResponseEntity.status(404).build();
            }

            Admin admin = adminOpt.get();
            Long companyId = admin.getRefCompanyId();

            if (companyId == null) {
                log.warn("Admin has no company assigned: {}", accountId);
                return ResponseEntity.ok(List.of());
            }

            // Get parking IDs from parking-service
            List<Long> parkingIds = parkingClient.getParkingIdsByCompany(companyId);

            log.info("Found {} parkings for company {}", parkingIds.size(), companyId);
            return ResponseEntity.ok(parkingIds);

        } catch (Exception e) {
            log.error("Failed to get parkings", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * GET /admin/parkings/{parkingId}/stats
     * Returns statistics for a specific parking
     */
    @GetMapping("/parkings/{parkingId}/stats")
    public ResponseEntity<Map<String, Object>> getParkingStats(
            @PathVariable Long parkingId,
            @RequestHeader("X-Account-Id") Long accountId) {

        log.info("Getting stats for parking {} by admin {}", parkingId, accountId);

        try {
            // Optional: Verify admin has access to this parking
            // For now, we trust the request

            Map<String, Object> stats = parkingClient.getParkingStats(parkingId);

            if (stats == null || stats.isEmpty()) {
                log.warn("No stats found for parking {}", parkingId);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Failed to get parking stats", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * GET /admin/parkings/{parkingId}/pricing
     * Returns pricing information for a specific parking
     */
    @GetMapping("/parkings/{parkingId}/pricing")
    public ResponseEntity<Map<String, Object>> getParkingPricing(
            @PathVariable Long parkingId,
            @RequestHeader("X-Account-Id") Long accountId) {

        log.info("Getting pricing for parking {} by admin {}", parkingId, accountId);

        try {
            Map<String, Object> pricing = parkingClient.getParkingPricing(parkingId);

            if (pricing == null || pricing.isEmpty()) {
                log.warn("No pricing found for parking {}", parkingId);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(pricing);

        } catch (Exception e) {
            log.error("Failed to get parking pricing", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * PUT /admin/parkings/pricing/{pricingId}
     * Updates pricing for a parking
     */
    @PutMapping("/parkings/pricing/{pricingId}")
    public ResponseEntity<?> updateParkingPricing(
            @PathVariable Long pricingId,
            @RequestParam Integer freeMinutes,
            @RequestParam Integer ratePerMin,
            @RequestParam Integer reservationFeeMinor,
            @RequestHeader("X-Account-Id") Long accountId) {

        log.info("Updating pricing {} by admin {} - freeMinutes: {}, ratePerMin: {}, reservationFee: {}",
                pricingId, accountId, freeMinutes, ratePerMin, reservationFeeMinor);

        try {
            boolean success = parkingClient.updatePricing(pricingId, freeMinutes, ratePerMin, reservationFeeMinor);

            if (success) {
                log.info("Pricing updated successfully");
                return ResponseEntity.ok(Map.of("message", "Pricing updated successfully"));
            } else {
                log.warn("Failed to update pricing");
                return ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to update pricing"));
            }

        } catch (Exception e) {
            log.error("Failed to update pricing", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to update pricing: " + e.getMessage()));
        }
    }

    /**
     * POST /admin/parkings
     * Creates a new parking with sections
     *
     * Admin-service acts as proxy - forwards to parking-service
     */
    @PostMapping("/parkings")
    public ResponseEntity<?> createParking(
            @RequestHeader("X-Account-Id") Long accountId,
            @RequestBody Map<String, Object> request) {

        log.info("Creating parking for admin accountId: {}", accountId);

        try {
            // Get admin to verify they exist and get company
            Optional<Admin> adminOpt = adminRepository.findByAccountId(accountId);

            if (adminOpt.isEmpty()) {
                log.warn("Admin not found for accountId: {}", accountId);
                return ResponseEntity.status(404)
                        .body(Map.of("error", "Admin not found"));
            }

            Admin admin = adminOpt.get();
            Long companyId = admin.getRefCompanyId();

            if (companyId == null) {
                log.warn("Admin has no company assigned");
                return ResponseEntity.status(400)
                        .body(Map.of("error", "Admin must be assigned to a company"));
            }

            // Add companyId to request
            request.put("companyId", companyId);

            // Forward to parking-service
            Long parkingId = parkingClient.createParkingWithSections(request);

            log.info("Successfully created parking with id: {}", parkingId);
            return ResponseEntity.ok(Map.of(
                    "parkingId", parkingId,
                    "message", "Parking created successfully. Don't forget to set pricing!"
            ));

        } catch (Exception e) {
            log.error("Failed to create parking", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to create parking: " + e.getMessage()));
        }

}

/**================================================
 * FINANCIAL REPORTS ENDPOINTS
 ================================================ */

    /**
     * GET /admin/reports/financial/summary
     * Returns financial summary for admin's company or specific parking
     *
     * Query params:
     * - parkingId (optional): if provided, returns data for that parking only
     * - period: "today", "week", "month", "quarter", "year"
     */
    @GetMapping("/reports/financial/summary")
    public ResponseEntity<Map<String, Object>> getFinancialSummary(
            @RequestHeader("X-Account-Id") Long accountId,
            @RequestParam(required = false) Long parkingId,
            @RequestParam(defaultValue = "month") String period) {

        log.info("Getting financial summary for admin {}, parking: {}, period: {}",
                accountId, parkingId, period);

        try {
            // Get admin to find their company
            Optional<Admin> adminOpt = adminRepository.findByAccountId(accountId);

            if (adminOpt.isEmpty()) {
                log.warn("Admin not found for accountId: {}", accountId);
                return ResponseEntity.status(404).build();
            }

            Admin admin = adminOpt.get();
            Long companyId = admin.getRefCompanyId();

            if (companyId == null) {
                log.warn("Admin has no company assigned: {}", accountId);
                return ResponseEntity.status(400)
                        .body(Map.of("error", "Admin must be assigned to a company"));
            }

            // Calculate date range
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = calculateStartDate(endDate, period);

            // Get financial summary using FinancialReportService
            Map<String, Object> summary;

            if (parkingId != null) {
                // Specific parking summary
                summary = financialReportService.getParkingFinancialSummary(
                        parkingId, startDate, endDate);
            } else {
                // Company-wide summary (all parkings)
                summary = financialReportService.getCompanyFinancialSummary(
                        companyId, startDate, endDate);
            }

            log.info("Successfully retrieved financial summary");
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("Failed to get financial summary", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to retrieve financial data"));
        }
    }

    /**
     * GET /admin/reports/financial/revenue-over-time
     * Returns revenue data points for charts (bar chart, line chart)
     *
     * Query params:
     * - parkingId (optional): if provided, returns data for that parking only
     * - period: "week", "month", "quarter", "year"
     */
    @GetMapping("/reports/financial/revenue-over-time")
    public ResponseEntity<List<Map<String, Object>>> getRevenueOverTime(
            @RequestHeader("X-Account-Id") Long accountId,
            @RequestParam(required = false) Long parkingId,
            @RequestParam(defaultValue = "month") String period) {

        log.info("Getting revenue over time for admin {}, parking: {}, period: {}",
                accountId, parkingId, period);

        try {
            // Get admin to find their company
            Optional<Admin> adminOpt = adminRepository.findByAccountId(accountId);

            if (adminOpt.isEmpty()) {
                log.warn("Admin not found for accountId: {}", accountId);
                return ResponseEntity.status(404).build();
            }

            Admin admin = adminOpt.get();
            Long companyId = admin.getRefCompanyId();

            if (companyId == null) {
                log.warn("Admin has no company assigned: {}", accountId);
                return ResponseEntity.ok(List.of());
            }

            // Calculate date range
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = calculateStartDate(endDate, period);

            // Get revenue data using FinancialReportService
            List<Map<String, Object>> revenueData = financialReportService.getRevenueOverTime(
                    parkingId, companyId, startDate, endDate, period);

            log.info("Successfully retrieved {} revenue data points", revenueData.size());
            return ResponseEntity.ok(revenueData);

        } catch (Exception e) {
            log.error("Failed to get revenue over time", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * GET /admin/reports/financial/revenue-distribution
     * Returns revenue breakdown by parking (for pie chart)
     * Only for admins viewing all parkings
     *
     * Query params:
     * - period: "today", "week", "month", "quarter", "year"
     */
    @GetMapping("/reports/financial/revenue-distribution")
    public ResponseEntity<List<Map<String, Object>>> getRevenueDistribution(
            @RequestHeader("X-Account-Id") Long accountId,
            @RequestParam(defaultValue = "month") String period) {

        log.info("Getting revenue distribution for admin {}, period: {}", accountId, period);

        try {
            // Get admin to find their company
            Optional<Admin> adminOpt = adminRepository.findByAccountId(accountId);

            if (adminOpt.isEmpty()) {
                log.warn("Admin not found for accountId: {}", accountId);
                return ResponseEntity.status(404).build();
            }

            Admin admin = adminOpt.get();
            Long companyId = admin.getRefCompanyId();

            if (companyId == null) {
                log.warn("Admin has no company assigned: {}", accountId);
                return ResponseEntity.ok(List.of());
            }

            // Calculate date range
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = calculateStartDate(endDate, period);

            // Get revenue distribution using FinancialReportService
            List<Map<String, Object>> distribution = financialReportService.getRevenueDistribution(
                    companyId, startDate, endDate);

            log.info("Successfully retrieved revenue distribution for {} parkings",
                    distribution.size());
            return ResponseEntity.ok(distribution);

        } catch (Exception e) {
            log.error("Failed to get revenue distribution", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * GET /admin/reports/financial/transactions
     * Returns detailed transaction list
     *
     * Query params:
     * - parkingId (optional): if provided, returns data for that parking only
     * - period: "today", "week", "month", "quarter", "year"
     * - status (optional): "all", "paid", "pending", "failed"
     */
    @GetMapping("/reports/financial/transactions")
    public ResponseEntity<List<Map<String, Object>>> getTransactions(
            @RequestHeader("X-Account-Id") Long accountId,
            @RequestParam(required = false) Long parkingId,
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(defaultValue = "all") String status) {

        log.info("Getting transactions for admin {}, parking: {}, period: {}, status: {}",
                accountId, parkingId, period, status);

        try {
            // Get admin to find their company
            Optional<Admin> adminOpt = adminRepository.findByAccountId(accountId);

            if (adminOpt.isEmpty()) {
                log.warn("Admin not found for accountId: {}", accountId);
                return ResponseEntity.status(404).build();
            }

            Admin admin = adminOpt.get();
            Long companyId = admin.getRefCompanyId();

            if (companyId == null) {
                log.warn("Admin has no company assigned: {}", accountId);
                return ResponseEntity.ok(List.of());
            }

            // Calculate date range
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = calculateStartDate(endDate, period);

            // Get transactions using FinancialReportService
            List<Map<String, Object>> transactions = financialReportService.getTransactions(
                    parkingId, companyId, startDate, endDate, status);

            log.info("Successfully retrieved {} transactions", transactions.size());
            return ResponseEntity.ok(transactions);

        } catch (Exception e) {
            log.error("Failed to get transactions", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Helper method to calculate start date based on period
     * UPDATED: Added support for "semester" (6 months)
     */
    private LocalDateTime calculateStartDate(LocalDateTime endDate, String period) {
        return switch (period.toLowerCase()) {
            case "today" -> endDate.toLocalDate().atStartOfDay();
            case "week" -> endDate.minusWeeks(1);
            case "month" -> endDate.minusMonths(1);
            case "quarter" -> endDate.minusMonths(3);
            case "semester" -> endDate.minusMonths(6);  // <-- DODAJ TĘ LINIĘ
            case "year" -> endDate.minusYears(1);
            default -> endDate.minusMonths(1);
        };
    }
    /**
     * GET /admin/personnel
     * Returns all personnel (admins + workers) from admin's company
     * Returns combined list with role, status, etc.
     */
    @GetMapping("/personnel")
    public ResponseEntity<?> getCompanyPersonnel(@RequestHeader("X-Account-Id") Long accountId) {
        log.info("GET /admin/personnel called for accountId: {}", accountId);

        try {
            // Get current admin to find their company
            Optional<Admin> adminOpt = adminRepository.findByAccountId(accountId);

            if (adminOpt.isEmpty()) {
                log.warn("Admin not found for accountId: {}", accountId);
                return ResponseEntity.status(404)
                        .body(Map.of("error", "Admin not found"));
            }

            Admin currentAdmin = adminOpt.get();
            Long companyId = currentAdmin.getRefCompanyId();

            if (companyId == null) {
                log.warn("Admin has no company assigned");
                return ResponseEntity.ok(List.of());
            }

            List<Map<String, Object>> personnel = new ArrayList<>();

            // 1. Get all admins from this company
            List<Admin> admins = adminRepository.findByCompanyId(companyId);
            log.info("Found {} admins for company {}", admins.size(), companyId);

            for (Admin admin : admins) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", admin.getRefAccountId());
                item.put("firstName", admin.getFirstName() != null ? admin.getFirstName() : "");
                item.put("lastName", admin.getLastName() != null ? admin.getLastName() : "");
                item.put("phoneNumber", admin.getPhoneNumber() != null ? admin.getPhoneNumber() : "");
                item.put("peselNumber", admin.getPeselNumber() != null ? admin.getPeselNumber() : "");
                item.put("role", "ADMIN");

                // Get email from account-service
                Optional<String> emailOpt = accountClient.getEmailByAccountId(admin.getRefAccountId());
                item.put("email", emailOpt.orElse(""));

                // Get active status from account-service
                boolean isActive = accountClient.isAccountActive(admin.getRefAccountId());
                item.put("active", isActive);

                personnel.add(item);
            }

            // 2. Get all workers from this company via WorkerClient
            List<Map<String, Object>> workers = workerClient.getWorkersByCompany(companyId);
            log.info("Found {} workers for company {}", workers.size(), companyId);

            // Workers already have all fields populated from worker-service
            personnel.addAll(workers);

            log.info("Returning {} total personnel (admins + workers)", personnel.size());
            return ResponseEntity.ok(personnel);

        } catch (Exception e) {
            log.error("Failed to get company personnel", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to retrieve personnel: " + e.getMessage()));
        }
    }

    /**
     * PUT /admin/personnel/worker/{accountId}/deactivate
     * Deactivate a worker account
     */
    @PutMapping("/personnel/worker/{accountId}/deactivate")
    public ResponseEntity<?> deactivateWorker(
            @RequestHeader("X-Account-Id") Long adminAccountId,
            @PathVariable Long accountId) {

        log.info("Admin {} requesting to deactivate worker {}", adminAccountId, accountId);

        try {
            // Verify admin has permission (same company)
            Optional<Admin> adminOpt = adminRepository.findByAccountId(adminAccountId);
            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "Admin not found"));
            }

            Admin admin = adminOpt.get();
            Long companyId = admin.getRefCompanyId();

            boolean workerBelongsToCompany = workerClient.verifyWorkerCompany(accountId, companyId);

            if (!workerBelongsToCompany) {
                log.warn("Worker {} does not belong to company {}", accountId, companyId);
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Cannot deactivate worker from different company"));
            }

            boolean success = accountClient.deactivateAccount(accountId);

            if (!success) {
                return ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to deactivate worker"));
            }

            log.info("Worker {} deactivated successfully by admin {}", accountId, adminAccountId);
            return ResponseEntity.ok(Map.of("message", "Worker deactivated successfully"));

        } catch (Exception e) {
            log.error("Failed to deactivate worker", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to deactivate worker: " + e.getMessage()));
        }
    }

    /**
     * PUT /admin/personnel/worker/{accountId}/activate
     * Activate a worker account
     */
    @PutMapping("/personnel/worker/{accountId}/activate")
    public ResponseEntity<?> activateWorker(
            @RequestHeader("X-Account-Id") Long adminAccountId,
            @PathVariable Long accountId) {

        log.info("Admin {} requesting to activate worker {}", adminAccountId, accountId);

        try {
            Optional<Admin> adminOpt = adminRepository.findByAccountId(adminAccountId);
            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "Admin not found"));
            }

            Admin admin = adminOpt.get();
            Long companyId = admin.getRefCompanyId();

            boolean workerBelongsToCompany = workerClient.verifyWorkerCompany(accountId, companyId);

            if (!workerBelongsToCompany) {
                log.warn("Worker {} does not belong to company {}", accountId, companyId);
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Cannot activate worker from different company"));
            }

            boolean success = accountClient.activateAccount(accountId);

            if (!success) {
                return ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to activate worker"));
            }

            log.info("Worker {} activated successfully by admin {}", accountId, adminAccountId);
            return ResponseEntity.ok(Map.of("message", "Worker activated successfully"));

        } catch (Exception e) {
            log.error("Failed to activate worker", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to activate worker: " + e.getMessage()));
        }
    }
}

