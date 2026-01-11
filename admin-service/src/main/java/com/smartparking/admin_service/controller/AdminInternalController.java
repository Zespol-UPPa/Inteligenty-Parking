package com.smartparking.admin_service.controller;

import com.smartparking.admin_service.client.CompanyClient;
import com.smartparking.admin_service.model.Admin;
import com.smartparking.admin_service.repo.AdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admins/internal")
public class AdminInternalController {
    private static final Logger log = LoggerFactory.getLogger(AdminInternalController.class);

    private final AdminRepository adminRepository;
    private final CompanyClient companyClient;

    public AdminInternalController(
            AdminRepository adminRepository,
            CompanyClient companyClient) {
        this.adminRepository = adminRepository;
        this.companyClient = companyClient;
    }

    /**
     * Get admin by account ID (for activation)
     */
    @GetMapping("/by-account/{accountId}")
    public ResponseEntity<?> getByAccountId(@PathVariable Long accountId) {
        log.info("Getting admin data for accountId: {}", accountId);

        Optional<Admin> adminOpt = adminRepository.findByAccountId(accountId);

        if (adminOpt.isEmpty()) {
            log.warn("Admin not found for accountId: {}", accountId);
            return ResponseEntity.notFound().build();
        }

        Admin admin = adminOpt.get();

        // Build response with basic data
        Map<String, Object> response = new HashMap<>();
        response.put("adminId", admin.getId());
        response.put("firstName", admin.getFirstName() != null ? admin.getFirstName() : "");
        response.put("lastName", admin.getLastName() != null ? admin.getLastName() : "");
        response.put("phoneNumber", admin.getPhoneNumber() != null ? admin.getPhoneNumber() : "");
        response.put("peselNumber", admin.getPeselNumber() != null ? admin.getPeselNumber() : "");
        response.put("companyId", admin.getRefCompanyId() != null ? admin.getRefCompanyId() : "");

        // Fetch company name if companyId exists
        if (admin.getRefCompanyId() != null) {
            String companyName = companyClient.getCompanyName(admin.getRefCompanyId());
            response.put("companyName", companyName != null ? companyName : "");
        } else {
            response.put("companyName", "");
        }

        log.info("Admin data retrieved: adminId={}, companyId={}, companyName={}",
                admin.getId(), admin.getRefCompanyId(), response.get("companyName"));

        return ResponseEntity.ok(response);
    }

    /**
     * Update admin personal data (for activation)
     */
    @PutMapping("/update-personal-data")
    public ResponseEntity<?> updatePersonalData(@RequestBody Map<String, Object> data) {
        try {
            Long accountId = ((Number) data.get("accountId")).longValue();
            String firstName = (String) data.get("firstName");
            String lastName = (String) data.get("lastName");
            String phoneNumber = (String) data.get("phoneNumber");
            String peselNumber = (String) data.get("peselNumber");

            log.info("Updating admin personal data for accountId: {}", accountId);

            adminRepository.updatePersonalData(accountId, firstName, lastName, phoneNumber, peselNumber);

            log.info("Admin personal data updated successfully for accountId: {}", accountId);
            return ResponseEntity.ok(Map.of("message", "Personal data updated successfully"));

        } catch (Exception e) {
            log.error("Failed to update admin personal data", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to update personal data: " + e.getMessage()));
        }
    }
}