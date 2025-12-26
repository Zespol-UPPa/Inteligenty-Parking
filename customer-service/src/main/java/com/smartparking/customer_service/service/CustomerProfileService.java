package com.smartparking.customer_service.service;

import com.smartparking.customer_service.model.Customer;
import com.smartparking.customer_service.repository.JdbcCustomerRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomerProfileService {
    private final JdbcCustomerRepository customers;
    
    public CustomerProfileService(JdbcCustomerRepository customers) {
        this.customers = customers;
    }
    
    public Optional<Map<String, Object>> getById(Long accountId) {
        return customers.findByAccountId(accountId)
                .map(customer -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", customer.getId());
                    map.put("firstName", customer.getFirstName() != null ? customer.getFirstName() : "");
                    map.put("lastName", customer.getLastName() != null ? customer.getLastName() : "");
                    map.put("accountId", customer.getRefAccountId());
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
}

