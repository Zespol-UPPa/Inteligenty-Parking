package com.smartparking.accounts_service.repo;

import com.smartparking.accounts_service.model.Account;

import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findByUsername(String username);
    Account save(Account account);
    Optional<Account> findById(Long id);
    Account deactivateById(Account account);
    Account markActiveById(Account account);
}
