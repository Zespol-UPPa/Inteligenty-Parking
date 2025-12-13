package com.smartparking.customer_service.service;

import com.smartparking.customer_service.repository.JdbcWalletRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
public class WalletService {
    private final JdbcWalletRepository repo;
    public WalletService(JdbcWalletRepository repo) {
        this.repo = repo;
    }

    public Optional<Map<String, Object>> getByAccountId(Long accountId) {
        return repo.findByAccountId(accountId);
    }

    public boolean setBalance(Long accountId, BigDecimal newBalance) {
        int updated = repo.updateBalanceByAccountId(accountId, newBalance);
        return updated > 0;
    }
}

