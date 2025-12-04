package com.smartparking.company_service.controller;

import com.smartparking.company_service.model.Company;
import com.smartparking.company_service.repo.CompanyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/company")
public class CompanyCrudController {
    private final CompanyRepository repo;

    public CompanyCrudController(CompanyRepository repo) { this.repo = repo; }

    @GetMapping("/health")
    public ResponseEntity<String> health() { return ResponseEntity.ok("company-service: OK"); }

    @PostMapping("/create")
    public ResponseEntity<String> create(@RequestBody Company c) {
        repo.save(c);
        return ResponseEntity.ok("created");
    }

    @GetMapping("/all")
    public ResponseEntity<List<Company>> all() { return ResponseEntity.ok(repo.findAll()); }
}

