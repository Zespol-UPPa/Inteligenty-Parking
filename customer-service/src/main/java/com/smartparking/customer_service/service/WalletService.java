package com.smartparking.customer_service.service;

import com.smartparking.customer_service.model.Customer;
import com.smartparking.customer_service.model.Wallet;
import com.smartparking.customer_service.repository.CustomerRepository;
import com.smartparking.customer_service.repository.JdbcWalletRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
public class WalletService {
    private final JdbcWalletRepository repo;
    private final CustomerRepository customerRepository;
    private final CustomerProfileService customerProfileService;
    
    public WalletService(JdbcWalletRepository repo, CustomerRepository customerRepository, CustomerProfileService customerProfileService) {
        this.repo = repo;
        this.customerRepository = customerRepository;
        this.customerProfileService = customerProfileService;
    }

    public Optional<Map<String, Object>> getByAccountId(Long accountId) {
        return repo.findByAccountId(accountId);
    }

    public boolean setBalance(Long accountId, BigDecimal newBalance) {
        int updated = repo.updateBalanceByAccountId(accountId, newBalance);
        return updated > 0;
    }
    
    public void createForAccountId(Long accountId) {
        // Sprawdź czy customer istnieje, jeśli nie - utwórz go
        Optional<Customer> customerOpt = customerRepository.findByAccountId(accountId);
        Customer customer;
        if (customerOpt.isEmpty()) {
            customer = customerProfileService.createForAccountId(accountId);
        } else {
            customer = customerOpt.get();
        }
        
        // Sprawdź czy wallet już istnieje dla tego customer
        Optional<Wallet> existingWallet = repo.findByCustomerId(customer.getId());
        if (existingWallet.isPresent()) {
            return; // Wallet już istnieje
        }
        
        // Utwórz nowy wallet z balance = 0 i currency = "PLN"
        Wallet wallet = new Wallet();
        wallet.setCustomerId(customer.getId());
        wallet.setBalanceMinor(BigDecimal.ZERO);
        wallet.setCurrencyCode("PLN");
        repo.save(wallet);
    }
}

