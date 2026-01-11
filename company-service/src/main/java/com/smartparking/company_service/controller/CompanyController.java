package com.smartparking.company_service.controller;

import com.smartparking.company_service.repo.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/companies")
public class CompanyController {
    private static final Logger log = LoggerFactory.getLogger(CompanyController.class);

    private final CompanyRepository companyRepository;

    public CompanyController(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    /**
     * GET /companies/{companyId}/name
     * Returns company name for external services (admin-service, worker-service, parking-service)
     */
    @GetMapping("/{companyId}/name")
    public ResponseEntity<?> getCompanyName(@PathVariable Long companyId) {
        log.info("Getting company name for companyId: {}", companyId);

        try {
            String name = companyRepository.getNameById(companyId);

            if (name != null) {
                return ResponseEntity.ok(Map.of("name", name));
            }

            log.warn("Company not found: {}", companyId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Failed to get company name", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get company name: " + e.getMessage()));
        }
    }
}