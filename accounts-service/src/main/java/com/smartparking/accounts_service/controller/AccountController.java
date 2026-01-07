package com.smartparking.accounts_service.controller;

import com.smartparking.accounts_service.model.Account;
import com.smartparking.accounts_service.repo.AccountRepository;
import com.smartparking.accounts_service.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountRepository repo;
    private final AccountService accountService;

    public AccountController(AccountRepository repo, AccountService accountService) {
        this.repo = repo;
        this.accountService = accountService;
    }

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

    @GetMapping("/{accountId}/email")
    public ResponseEntity<Map<String, String>> getEmail(@PathVariable Long accountId) {
        Optional<Account> accountOpt = accountService.findById(accountId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Account account = accountOpt.get();
        return ResponseEntity.ok(Map.of("email", account.getUsername()));
    }
    
    @PutMapping("/{accountId}/password")
    public ResponseEntity<?> changePassword(@PathVariable Long accountId,
                                             @RequestBody Map<String, String> body) {
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");
        
        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "currentPassword and newPassword are required"));
        }
        
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 8 characters long"));
        }
        
        boolean success = accountService.changePassword(accountId, currentPassword, newPassword);
        if (!success) {
            return ResponseEntity.status(401).body(Map.of("error", "Current password is incorrect"));
        }
        
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
    
    @DeleteMapping("/{accountId}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long accountId) {
        boolean success = accountService.deleteAccount(accountId);
        if (!success) {
            return ResponseEntity.status(404).body(Map.of("error", "Account not found"));
        }
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }
}
