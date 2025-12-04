package com.smartparking.accounts_service.controller;

import com.smartparking.accounts_service.model.Account;
import com.smartparking.accounts_service.repo.AccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountRepository repo;

    public AccountController(AccountRepository repo) { this.repo = repo; }

    @GetMapping("/health")
    public ResponseEntity<String> health() { return ResponseEntity.ok("accounts-service: OK"); }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Account a) {
        if (repo.findByUsername(a.getUsername()) != null) {
            return ResponseEntity.badRequest().body("username_exists");
        }
        repo.save(a);
        return ResponseEntity.ok("created");
    }
}
