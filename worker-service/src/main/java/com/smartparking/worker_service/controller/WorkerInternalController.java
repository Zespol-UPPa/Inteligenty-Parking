package com.smartparking.worker_service.controller;

import com.smartparking.worker_service.client.AccountClient;
import com.smartparking.worker_service.client.CompanyClient;
import com.smartparking.worker_service.client.ParkingClient;
import com.smartparking.worker_service.model.Worker;
import com.smartparking.worker_service.repo.WorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/workers/internal")
public class WorkerInternalController {
    private static final Logger log = LoggerFactory.getLogger(WorkerInternalController.class);

    private final WorkerRepository workerRepository;
    private final ParkingClient parkingClient;
    private final CompanyClient companyClient;
    private final AccountClient accountClient;  // ← DODAJ TO!

    public WorkerInternalController(
            WorkerRepository workerRepository,
            ParkingClient parkingClient,
            CompanyClient companyClient,
            AccountClient accountClient) {  // ← DODAJ TO!
        this.workerRepository = workerRepository;
        this.parkingClient = parkingClient;
        this.companyClient = companyClient;
        this.accountClient = accountClient;  // ← DODAJ TO!
    }

    // ========================================================================
    // EXISTING ENDPOINTS (już masz)
    // ========================================================================

    /**
     * Get worker by account ID (for activation)
     */
    @GetMapping("/by-account/{accountId}")
    public ResponseEntity<?> getByAccountId(@PathVariable Long accountId) {
        log.info("Getting worker data for accountId: {}", accountId);

        Optional<Worker> workerOpt = workerRepository.findByAccountId(accountId);

        if (workerOpt.isEmpty()) {
            log.warn("Worker not found for accountId: {}", accountId);
            return ResponseEntity.notFound().build();
        }

        Worker worker = workerOpt.get();

        // Build response with basic data
        Map<String, Object> response = new HashMap<>();
        response.put("workerId", worker.getId());
        response.put("firstName", worker.getFirstName() != null ? worker.getFirstName() : "");
        response.put("lastName", worker.getLastName() != null ? worker.getLastName() : "");
        response.put("phoneNumber", worker.getPhoneNumber() != null ? worker.getPhoneNumber() : "");
        response.put("peselNumber", worker.getPeselNumber() != null ? worker.getPeselNumber() : "");
        response.put("companyId", worker.getRefCompanyId() != null ? worker.getRefCompanyId() : "");
        response.put("parkingId", worker.getRefParkingId() != null ? worker.getRefParkingId() : "");

        // Fetch parking name if parkingId exists
        if (worker.getRefParkingId() != null) {
            String parkingName = parkingClient.getParkingName(worker.getRefParkingId());
            response.put("parkingName", parkingName != null ? parkingName : "");
        } else {
            response.put("parkingName", "");
        }

        // Fetch company name if companyId exists
        if (worker.getRefCompanyId() != null) {
            String companyName = companyClient.getCompanyName(worker.getRefCompanyId());
            response.put("companyName", companyName != null ? companyName : "");
        } else {
            response.put("companyName", "");
        }

        log.info("Worker data retrieved: workerId={}, parkingId={}, parkingName={}, companyName={}",
                worker.getId(), worker.getRefParkingId(), response.get("parkingName"), response.get("companyName"));

        return ResponseEntity.ok(response);
    }

    /**
     * Update worker personal data (for activation)
     */
    @PutMapping("/update-personal-data")
    public ResponseEntity<?> updatePersonalData(@RequestBody Map<String, Object> data) {
        try {
            Long accountId = ((Number) data.get("accountId")).longValue();
            String firstName = (String) data.get("firstName");
            String lastName = (String) data.get("lastName");
            String phoneNumber = (String) data.get("phoneNumber");
            String peselNumber = (String) data.get("peselNumber");

            log.info("Updating worker personal data for accountId: {}", accountId);

            workerRepository.updatePersonalData(accountId, firstName, lastName, phoneNumber, peselNumber);

            log.info("Worker personal data updated successfully for accountId: {}", accountId);
            return ResponseEntity.ok(Map.of("message", "Personal data updated successfully"));

        } catch (Exception e) {
            log.error("Failed to update worker personal data", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to update personal data: " + e.getMessage()));
        }
    }

    // ========================================================================
    // ENDPOINTS FOR PERSONNEL MANAGEMENT
    // ========================================================================

    /**
     * GET /workers/internal/by-company/{companyId}
     * Returns all workers from specified company with email and status
     * Used by admin-service to get all company personnel
     */
    @GetMapping("/by-company/{companyId}")
    public ResponseEntity<List<Map<String, Object>>> getWorkersByCompany(
            @PathVariable Long companyId) {

        log.info("GET /workers/internal/by-company/{} called", companyId);

        try {
            // Get all workers from company
            List<Worker> workers = workerRepository.findByCompanyId(companyId);
            log.info("Found {} workers for company {}", workers.size(), companyId);

            List<Map<String, Object>> result = new ArrayList<>();

            // For each worker, get email and status from accounts-service
            for (Worker worker : workers) {
                Map<String, Object> workerData = new HashMap<>();

                // Use refAccountId as the main ID (same across all services)
                workerData.put("id", worker.getRefAccountId());  // ← This is what frontend displays
                workerData.put("accountId", worker.getRefAccountId());
                workerData.put("firstName", worker.getFirstName());
                workerData.put("lastName", worker.getLastName());
                workerData.put("phoneNumber", worker.getPhoneNumber());
                workerData.put("peselNumber", worker.getPeselNumber());
                workerData.put("companyId", worker.getRefCompanyId());
                workerData.put("parkingId", worker.getRefParkingId());
                workerData.put("role", "WORKER");

                // Get email from accounts-service
                try {
                    Optional<String> emailOpt = accountClient.getEmailByAccountId(worker.getRefAccountId());
                    workerData.put("email", emailOpt.orElse(""));
                } catch (Exception e) {
                    log.error("Failed to get email for worker {}", worker.getRefAccountId(), e);
                    workerData.put("email", "");
                }

                // Get active status from accounts-service
                try {
                    boolean isActive = accountClient.isAccountActive(worker.getRefAccountId());
                    workerData.put("isActive", isActive);
                } catch (Exception e) {
                    log.error("Failed to get status for worker {}", worker.getRefAccountId(), e);
                    workerData.put("isActive", false);
                }

                // Get parking name (optional)
                if (worker.getRefParkingId() != null) {
                    try {
                        String parkingName = parkingClient.getParkingName(worker.getRefParkingId());
                        workerData.put("parkingName", parkingName);
                    } catch (Exception e) {
                        log.error("Failed to get parking name", e);
                        workerData.put("parkingName", "");
                    }
                }

                result.add(workerData);
            }

            log.info("Returning {} workers with full details", result.size());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to get workers for company {}", companyId, e);
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * GET /workers/internal/verify-company?accountId={accountId}&companyId={companyId}
     * Verifies if worker belongs to specified company
     * Used by admin-service before deactivating/activating worker
     */
    @GetMapping("/verify-company")
    public ResponseEntity<Boolean> verifyWorkerCompany(
            @RequestParam Long accountId,
            @RequestParam Long companyId) {

        log.info("Verifying worker {} belongs to company {}", accountId, companyId);

        try {
            Optional<Worker> workerOpt = workerRepository.findByAccountId(accountId);

            if (workerOpt.isEmpty()) {
                log.warn("Worker not found for accountId: {}", accountId);
                return ResponseEntity.ok(false);
            }

            Worker worker = workerOpt.get();
            boolean belongs = worker.getRefCompanyId().equals(companyId);

            log.info("Worker {} belongs to company {}: {}", accountId, companyId, belongs);
            return ResponseEntity.ok(belongs);

        } catch (Exception e) {
            log.error("Failed to verify worker company", e);
            return ResponseEntity.ok(false);
        }
    }
}