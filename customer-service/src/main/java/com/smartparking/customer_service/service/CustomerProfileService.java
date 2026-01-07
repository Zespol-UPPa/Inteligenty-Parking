package com.smartparking.customer_service.service;

import com.smartparking.customer_service.client.AccountClient;
import com.smartparking.customer_service.model.Customer;
import com.smartparking.customer_service.repository.JdbcCustomerRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomerProfileService {
    private final JdbcCustomerRepository customers;
    private final AccountClient accountClient;
    
    public CustomerProfileService(JdbcCustomerRepository customers, AccountClient accountClient) {
        this.customers = customers;
        this.accountClient = accountClient;
    }
    
    public Optional<Map<String, Object>> getById(Long accountId) {
        return customers.findByAccountId(accountId)
                .map(customer -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", customer.getId());
                    map.put("firstName", customer.getFirstName() != null ? customer.getFirstName() : "");
                    map.put("lastName", customer.getLastName() != null ? customer.getLastName() : "");
                    map.put("accountId", customer.getRefAccountId());
                    // Pobierz email z accounts-service
                    String email = accountClient.getEmailByAccountId(accountId).orElse("");
                    map.put("email", email);
                    return map;
                });
    }
    
    public boolean updateName(Long accountId, String firstName, String lastName) {
        Optional<Customer> customerOpt = customers.findByAccountId(accountId);
        if (customerOpt.isEmpty()) {
            return false;
        }
        Customer customer = customerOpt.get();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customers.save(customer);
        return true;
    }
    
    public Customer createForAccountId(Long accountId) {
        Customer customer = new Customer();
        customer.setRefAccountId(accountId);
        customer.setFirstName(""); // Puste wartości na start
        customer.setLastName("");
        return customers.save(customer);
    }
    
    public Customer createForAccountId(Long accountId, String firstName, String lastName) {
        // Sprawdź czy customer już istnieje
        Optional<Customer> existingCustomer = customers.findByAccountId(accountId);
        
        if (existingCustomer.isPresent()) {
            // Aktualizuj istniejącego customer
            Customer customer = existingCustomer.get();
            customer.setFirstName(firstName != null ? firstName : "");
            customer.setLastName(lastName != null ? lastName : "");
            return customers.save(customer);
        } else {
            // Utwórz nowego customer
            Customer customer = new Customer();
            customer.setRefAccountId(accountId);
            customer.setFirstName(firstName != null ? firstName : "");
            customer.setLastName(lastName != null ? lastName : "");
            return customers.save(customer);
        }
    }
}

