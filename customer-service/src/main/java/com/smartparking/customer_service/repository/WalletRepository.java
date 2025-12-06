package com.smartparking.customer_service.repository;

import com.smartparking.customer_service.model.Wallet;

import java.util.List;
import java.util.Optional;

public interface WalletRepository {
    Optional<Wallet> findById(Long id);
    Optional<Wallet> findByCustomerId(Long customerId);
    List<Wallet> findAll();
    Wallet save(Wallet wallet);
}
