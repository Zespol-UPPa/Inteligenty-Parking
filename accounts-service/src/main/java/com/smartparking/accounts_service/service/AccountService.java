package com.smartparking.accounts_service.service;

import com.smartparking.accounts_service.model.Account;
import com.smartparking.accounts_service.repo.AccountRepository;
import com.smartparking.accounts_service.repo.VerificationTokenRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccountService {
    private final AccountRepository repo;
    private final VerificationTokenRepository tokenRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AccountService(AccountRepository repo, VerificationTokenRepository tokenRepository) {
        this.repo = repo;
        this.tokenRepository = tokenRepository;
    }

    public Account register(String username, String rawPassword) {
        Account a = new Account();
        a.setUsername(username);
        a.setPasswordHash(encoder.encode(rawPassword));
        a.setRole("User"); // Zgodne z ENUM w bazie (User, Worker, Admin)
        a.setActive(false); // Account is inactive until email verification
        return repo.save(a);
    }

    public Optional<Account> findByUsername(String username) { return repo.findByUsername(username); }
    
    public Optional<Account> findById(Long id) { return repo.findById(id); }

    public boolean verify(String username, String rawPassword) {
        Optional<Account> a = repo.findByUsername(username);
        if (a.isEmpty()) {
            return false;
        }
        Account account = a.get();
        // Check password
        if (!encoder.matches(rawPassword, account.getPasswordHash())) {
            return false;
        }
        // Check if account is activated
        if (!account.getActive()) {
            throw new AccountNotActivatedException("Account not activated. Please check your email for verification link.");
        }
        return true;
    }

    public void activateAccount(Long accountId) {
        Optional<Account> accountOpt = repo.findById(accountId);
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            account.setActive(true);
            repo.save(account);
        }
    }
    
    public boolean changePassword(Long accountId, String currentPassword, String newPassword) {
        Optional<Account> accountOpt = repo.findById(accountId);
        if (accountOpt.isEmpty()) {
            return false;
        }
        Account account = accountOpt.get();
        // Verify current password
        if (!encoder.matches(currentPassword, account.getPasswordHash())) {
            return false;
        }
        // Update password
        account.setPasswordHash(encoder.encode(newPassword));
        repo.save(account);
        return true;
    }
    
    public boolean deleteAccount(Long accountId) {
        Optional<Account> accountOpt = repo.findById(accountId);
        if (accountOpt.isEmpty()) {
            return false;
        }
        // Delete verification tokens first to avoid foreign key constraint violation
        tokenRepository.deleteByAccountId(accountId);
        // Delete account (cascade will handle related records in other services)
        repo.deleteById(accountId);
        return true;
    }

    public static class AccountNotActivatedException extends RuntimeException {
        public AccountNotActivatedException(String message) {
            super(message);
        }
    }
}

