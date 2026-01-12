package com.smartparking.company_service.controller;

import com.smartparking.company_service.model.Company;
import com.smartparking.company_service.repo.CompanyRepository;
import com.smartparking.company_service.repo.JdbcCompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/company")
public class CompanyCrudController {
    private static final Logger log = LoggerFactory.getLogger(CompanyCrudController.class);

    private final JdbcCompanyRepository repo;
    private final CompanyRepository companyRepository;

    public CompanyCrudController(JdbcCompanyRepository repo, CompanyRepository companyRepository) {
        this.repo = repo;
        this.companyRepository = companyRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("company-service: OK");
    }

    @PostMapping("/create")
    public ResponseEntity<String> create(@RequestBody Company c) {
        repo.save(c);
        return ResponseEntity.ok("created");
    }

    @GetMapping("/all")
    public ResponseEntity<List<Company>> all() {
        return ResponseEntity.ok(repo.findAll());
    }

    /**
     * GET /company/{companyId}/name
     * Returns company name for external services
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