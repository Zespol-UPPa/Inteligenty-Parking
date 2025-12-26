package com.smartparking.customer_service.repository;

import com.smartparking.customer_service.model.Wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface WalletRepository {
    Optional<Wallet> findById(Long id);
    Optional<Wallet> findByCustomerId(Long customerId);
    List<Wallet> findAll();
    Wallet save(Wallet wallet);
    
    // Methods for accountId-based operations
    // Note: accountId refers to ref_account_id in customer table, not customer_id
    Optional<Map<String, Object>> findByAccountId(Long accountId);
    int updateBalanceByAccountId(Long accountId, BigDecimal newBalance);
}
