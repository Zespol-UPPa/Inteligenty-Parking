package com.smartparking.accounts_service.service;

import com.smartparking.accounts_service.model.Account;
import com.smartparking.accounts_service.repo.AccountRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccountService {
    private final AccountRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AccountService(AccountRepository repo) { this.repo = repo; }

    public Account register(String username, String rawPassword) {
        Account a = new Account();
        a.setUsername(username);
        a.setPasswordHash(encoder.encode(rawPassword));
        a.setRole("USER");
        return repo.save(a);
    }

    public Optional<Account> findByUsername(String username) { return repo.findByUsername(username); }

    public boolean verify(String username, String rawPassword) {
        Optional<Account> a = repo.findByUsername(username);
        return a.isPresent() && encoder.matches(rawPassword, a.get().getPasswordHash());
    }
}

